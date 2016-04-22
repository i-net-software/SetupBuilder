/*
 * Copyright 2015 - 2016 i-net software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.inet.gradle.setup.rpm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.inet.gradle.setup.DesktopStarter;
import com.inet.gradle.setup.LocalizedResource;
import com.inet.gradle.setup.SetupBuilder;

/**
 * Builder for the SPEC file, that is required for the Redhat package tool.
 * <br>
 * This file contains settings for the package like dependencies, architecture, description and 
 * scripts and commands that are executed before, during and after the installation.
 * 
 * @author Stefan Heidrich
 */
class RpmControlFileBuilder {

    private static final char NEWLINE = '\n';

	private final Rpm          rpm;

    private final SetupBuilder setup;

    private File               buildDir;

    private Collection<String> confFiles = new ArrayList<>();
    
    enum Script {
        PREINST, POSTINST, PRERM, POSTRM
    }
    
    Map<Script, StringBuilder> scriptMap = new HashMap<>();
    
    
    /**
     * the constructor setting the fields
     * @param rpm the task for the redhat package
     * @param setup the generic task for all setups
     * @param buildDir the directory to build the package in
     */
    RpmControlFileBuilder( Rpm rpm, SetupBuilder setup, File buildDir ) {
        this.rpm = rpm;
        this.setup = setup;
        this.buildDir = buildDir;
    }

    /**
     * Create the configuration files for the RedHat package based on the settings in the task.
     * 
     * @throws Exception
     */
    void build() throws Exception {
    	
    	createControlFile();
    }

    /**
     * Creates the SPEC file for the package
     * @throws IOException if something could not be written to the file
     */
	private void createControlFile() throws IOException {
		if(!buildDir.exists()) {
			buildDir.mkdirs();
		} else if(!buildDir.isDirectory()) {
			throw new IllegalArgumentException("The buildDir parameter must be a directory!");
		} 
		
		FileOutputStream fileoutput = null;
		OutputStreamWriter controlWriter = null;

		try {
			File spec = new File(buildDir, setup.getAppIdentifier() + ".spec");
			fileoutput = new FileOutputStream(spec);
			controlWriter = new OutputStreamWriter(fileoutput, "UTF-8");
			
			putSummary(controlWriter);
			putName(controlWriter);
			putVersion(controlWriter);
			putRelease(controlWriter);
			putLicense(controlWriter);
			putSection(controlWriter); 
			putBuildRoot(controlWriter);
			putURL(controlWriter);
			putVendor(controlWriter);
			putPackager(controlWriter);
			putPrefix(controlWriter);
			putDepends(controlWriter);
			putArchitecture(controlWriter);
			
			putDescription(controlWriter); 
			
			putPrep(controlWriter);
			
			putBuild(controlWriter);
			
			putInstall(controlWriter);
			
			putClean(controlWriter);
			
			putFiles(controlWriter);
			
			putPre(controlWriter);
			
			putPost(controlWriter);

			putPreun(controlWriter);

			putPostun(controlWriter);
			
			controlWriter.flush();
			
		} finally {
			if(controlWriter != null) {
				try {
					controlWriter.close();
				} catch (IOException e) {
					// IGNORE
				}
			}
			if(fileoutput != null) {
				try {
					fileoutput.close();
				} catch (IOException e) {
					// IGNORE
				}
			}
		}
	}

	/**
	 * Get executed before the package has been removed
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putPreun(OutputStreamWriter controlWriter)  throws IOException {
		controlWriter.write(NEWLINE + "%preun" + NEWLINE);
		controlWriter.write(NEWLINE + "if [ $1 -eq 0 ]; then" + NEWLINE);
		
		ArrayList<String> preuns = rpm.getPreun();
		for (String preun : preuns) {
			controlWriter.write(preun + NEWLINE);	
		}
		StringBuilder prep_script = scriptMap.get( Script.PRERM );
		if(prep_script != null) {
			controlWriter.write(prep_script.toString() + NEWLINE);
		}
		
		// removes only the files in the installation path
		List<String> del_files = setup.getDeleteFiles();
		for (String file : del_files) {
			controlWriter.write("if [ -f \"${RPM_INSTALL_PREFIX}/" + file + "\" ]; then\n  rm -f \"${RPM_INSTALL_PREFIX}/" + file + "\"\nfi" + NEWLINE);	
		}
		// removes only the dirs in the installation path
		List<String> del_dirs = setup.getDeleteFolders();
		for (String dirs : del_dirs) {
			controlWriter.write("rm -R -f \"${RPM_INSTALL_PREFIX}/" + dirs + "\"" + NEWLINE);	
		}
		
		DesktopStarter starter = setup.getRunBeforeUninstall();
		if(starter != null ) {
			controlWriter.write(NEWLINE);
			String executable = starter.getExecutable();
			String mainClass = starter.getMainClass();
			String workingDir = starter.getWorkDir();
			if( executable != null ) {
				if( workingDir != null ) {
					controlWriter.write("( cd \"${RPM_INSTALL_PREFIX}/" + workingDir + "\" && " + executable + " " + starter.getStartArguments() + " )" + NEWLINE);
				} else {
					controlWriter.write("( cd \"${RPM_INSTALL_PREFIX}\" && " + executable + " " + starter.getStartArguments() + " )" + NEWLINE);	
				}
				
			} else if( mainClass != null ) {
				if( workingDir != null ) {
					controlWriter.write("( cd \"${RPM_INSTALL_PREFIX}/" + workingDir + "\" && java -cp " + starter.getMainJar()  + " " +  mainClass + " " + starter.getStartArguments() + " )" + NEWLINE);
				} else {
					controlWriter.write("( cd \"${RPM_INSTALL_PREFIX}\" && java -cp " + starter.getMainJar()  + " " +  mainClass + " " + starter.getStartArguments() + " )"  + NEWLINE);	
				}
			}
			controlWriter.write(NEWLINE);
		}
		controlWriter.write(NEWLINE + "fi" + NEWLINE);
	}

	/**
	 * This script is executed before the package has been installed.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putPre(OutputStreamWriter controlWriter) throws IOException {
		controlWriter.write(NEWLINE + "%pre" + NEWLINE);
		ArrayList<String> pres = rpm.getPre();
		for (String pre : pres) {
			controlWriter.write(pre + NEWLINE);	
		}
		
		StringBuilder pre_script = scriptMap.get( Script.PREINST );
		if(pre_script != null) {
			controlWriter.write(pre_script.toString() + NEWLINE);
		}
		
		// removes only the files in the installation path
		List<String> del_files = setup.getDeleteFiles();
		for (String file : del_files) {
			controlWriter.write("if [ -f \"${RPM_INSTALL_PREFIX}/" + file + "\" ]; then\n  rm -f \"${RPM_INSTALL_PREFIX}/" + file + "\"\nfi" + NEWLINE);	
		}
		// removes only the dirs in the installation path
		List<String> del_dirs = setup.getDeleteFolders();
		for (String dirs : del_dirs) {
			controlWriter.write("rm -R -f \"${RPM_INSTALL_PREFIX}/" + dirs + "\"" + NEWLINE);	
		}
		
	}
	
	/**
	 * This script is executed after the package has been installed.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putPost(OutputStreamWriter controlWriter) throws IOException {
		controlWriter.write(NEWLINE + "%post" + NEWLINE);
		ArrayList<String> posts = rpm.getPost();
		for (String post : posts) {
			controlWriter.write(post + NEWLINE);	
		}	
		
		StringBuilder post_script = scriptMap.get( Script.POSTINST );
		if(post_script != null) {
			controlWriter.write(post_script.toString() + NEWLINE);
		}
		
		
		DesktopStarter starter = setup.getRunAfter();
		if(starter != null ) {
			String executable = starter.getExecutable();
			String mainClass = starter.getMainClass();
			String workingDir = starter.getWorkDir();
			if( executable != null ) {
				if( workingDir != null ) {
					controlWriter.write("( cd \"${RPM_INSTALL_PREFIX}/" + workingDir + "\" && " + executable + " & )" + NEWLINE);
				} else {
					controlWriter.write("( cd \"${RPM_INSTALL_PREFIX}\" && " + executable + " & )" + NEWLINE);	
				}
				
			} else if( mainClass != null ) {
				if( workingDir != null ) {
					controlWriter.write("( cd \"${RPM_INSTALL_PREFIX}/" + workingDir + "\" && java -cp " + starter.getMainJar()  + " " +  mainClass + " > starter.log )" + NEWLINE);
				} else {
					controlWriter.write("( cd \"${RPM_INSTALL_PREFIX}\" && java -cp " + starter.getMainJar()  + " " +  mainClass + " > starter.log )"  + NEWLINE);	
				}
			}
		}
		
		controlWriter.write("gtk-update-icon-cache /usr/share/icons/hicolor &>/dev/null || :"  + NEWLINE);
		
	}
	
	/**
	 * This script is executed after the package has been removed.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putPostun(OutputStreamWriter controlWriter) throws IOException {
		controlWriter.write(NEWLINE + "%postun" + NEWLINE);
		if(rpm.getPostun().size() > 0 || scriptMap.get( Script.POSTRM ) != null ) {
			controlWriter.write(NEWLINE + "if [ $1 -eq 0 ]; then" + NEWLINE);
		}
		
		
		ArrayList<String> posts = rpm.getPostun();
		for (String post : posts) {
			controlWriter.write(post + NEWLINE);	
		}	
		StringBuilder postun_script = scriptMap.get( Script.POSTRM );
		if(postun_script != null) {
			controlWriter.write(postun_script.toString() + NEWLINE);
		}
		if(rpm.getPostun().size() > 0 || scriptMap.get( Script.POSTRM ) != null ) {
			controlWriter.write(NEWLINE + "fi" + NEWLINE);
		}
	}
	
	/**
	 * Specifies the files that should be installed. The files will be installed under the installationRoot in the system.
	 * If files need to be installed somewhere else these files need to be copied via the post step. 
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putFiles(OutputStreamWriter controlWriter) throws IOException {
		controlWriter.write(NEWLINE + "%files" + NEWLINE);
		controlWriter.write( "\"" + rpm.getInstallationRoot()+ "\"" + NEWLINE); // nimmt anscheinend nicht die Files in der Root
		
		if(setup.getDesktopStarters() != null && setup.getDesktopStarters().size() > 0) {
			controlWriter.write( "/usr/share/applications/*" + NEWLINE);
			controlWriter.write( "/usr/share/icons/**/*" + NEWLINE);
			controlWriter.write( "/usr/bin/*" + NEWLINE);
		}
		
		if(setup.getServices() != null && setup.getServices().size() > 0) {
			controlWriter.write("/etc/init.d/*" + NEWLINE);	
		}
		
		if( setup.getLicenseFiles() != null && setup.getLicenseFiles().size() > 0) {
			controlWriter.write( "/usr/share/licenses/**/*" + NEWLINE);
		}
		
	}

	/**
	 * This is used to clean up the build directory tree. Normally RPM does this for you.
	 * During the clean step the created package will be copied to the distribution directory. 
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putClean(OutputStreamWriter controlWriter) throws IOException {
		controlWriter.write(NEWLINE + "%clean" + NEWLINE);
		String release = rpm.getRelease();
		if(release == null || release.length() == 0) {
			release = "1";
		}
		controlWriter.write("cp ../SRPMS/" + setup.getAppIdentifier() + "-" + setup.getVersion() + "-" + release + ".src.rpm '" + setup.getDestinationDir().getAbsolutePath()+ "'" + NEWLINE);
		controlWriter.write("mv -f ../RPMS/noarch/" + setup.getAppIdentifier() + "-" + setup.getVersion() + "-" + release + ".noarch.rpm '" + rpm.getSetupFile() + "'"+ NEWLINE);
		ArrayList<String> cleans = rpm.getClean();
		for (String clean : cleans) {
			controlWriter.write(clean + NEWLINE);	
		}
	}

	/**
	 * Contains the necessary steps to install the build software
	 * The files in the BUILD directory needs to be copied into the BUILDROOT directory so that the install step 
	 * finds the files.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putInstall(OutputStreamWriter controlWriter) throws IOException {
		controlWriter.write(NEWLINE + "%install" + NEWLINE);
		controlWriter.write("cp -R . '%{buildroot}'" + NEWLINE);
//		if(setup.getServices() != null && setup.getServices().size() > 0) {
//			controlWriter.write("cp -R etc %{buildroot}" + NEWLINE);	
//		}
		ArrayList<String> installs = rpm.getInstall();
		for (String install : installs) {
			controlWriter.write(install + NEWLINE);	
		}
	}

	/**
	 * This will be executed during the building of the package
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putBuild(OutputStreamWriter controlWriter) throws IOException {
		controlWriter.write(NEWLINE + "%build" + NEWLINE);
		ArrayList<String> builds = rpm.getBuild();
		for (String build : builds) {
			controlWriter.write(build + NEWLINE);	
		}
	}

	/**
	 * This is the first script RPM executes during a build.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putPrep(OutputStreamWriter controlWriter) throws IOException {
		controlWriter.write(NEWLINE + "%prep" + NEWLINE);
		ArrayList<String> preps = rpm.getPrep();
		for (String prep : preps) {
			controlWriter.write(prep + NEWLINE);	
		}
	}

	/**
	 * Write the prefix to the file.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putPrefix(OutputStreamWriter controlWriter) 
			throws IOException {
		String prefix = rpm.getInstallationRoot();
		
		controlWriter.write("Prefix: \"" + prefix + "\"" + NEWLINE);
		
	}

	/**
	 * Write the packager to the file. The packager is the same as the vendor.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putPackager(OutputStreamWriter controlWriter)
			throws IOException {
		String vendor = setup.getVendor();
		if(vendor == null || vendor.length() == 0) {
			throw new RuntimeException("No vendor declared in the setup configuration.");
		} else {
			controlWriter.write("Packager: " + vendor + NEWLINE);
		}
	}

	/**
	 * Write the url to the file. 
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putURL(OutputStreamWriter controlWriter)throws IOException {
		String url = rpm.getUrl();
		if(url != null && url.length() > 0) {
			controlWriter.write("URL: " + url + NEWLINE);
		}
	}

	/**
	 * Write the BuildRoot entry to the file. 
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putBuildRoot(OutputStreamWriter controlWriter)
			throws IOException {
		controlWriter.write("BuildRoot: ${_builddir}/%{name}-root" + NEWLINE);
	}

	/**
	 * Write the license to the file. If no license is specified 'Restricted' is used.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putLicense(OutputStreamWriter controlWriter)
			throws IOException {
		String license = rpm.getLicense();
		if(license == null || license.length() == 0) {
			license = "Restricted";
		}
		controlWriter.write("License: " + license + NEWLINE);
	}

	/**
	 * Write the release to the file. If no release is specified '1' is used.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putRelease(OutputStreamWriter controlWriter) 
			throws IOException {
		String release = rpm.getRelease();
		if(release == null || release.length() == 0) {
			release = "1";
		}
		controlWriter.write("Release: " + release + NEWLINE);
		
	}

	/**
	 * Write the description to the file. The description is created from the long description entries.
	 * The description for language specified with the defaultDescriptionLanguage property will be used as default description
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
    private void putDescription( OutputStreamWriter controlWriter ) throws IOException {
        controlWriter.write( NEWLINE + "%define __jar_repack %{nil}" + NEWLINE );
        
        List<LocalizedResource> descriptions = setup.getLongDescriptions();
        if(descriptions.size() > 0) {
        for (LocalizedResource desc : descriptions) {
        	
        	String lang = desc.getLanguage();
        	String content = NEWLINE + "%description";
        	content += ( lang.equalsIgnoreCase(setup.getDefaultResourceLanguage()) ? " -l" + lang + NEWLINE : "" + NEWLINE );
        	
        	try ( Scanner scanner = new Scanner( desc.getResource(), "UTF8" ) ) {
        		content += scanner.useDelimiter("\\A").next();
        	} finally {
        		controlWriter.write( content + NEWLINE );
        	}
		}        
        } else {
        	controlWriter.write( NEWLINE + "%description" + NEWLINE + NEWLINE );
        }
    }

	/**
	 * Write the vendor to the file. The vendor is mandatory. If no vendor is declared a runtime exception will be thrown.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putVendor(OutputStreamWriter controlWriter)
			throws IOException {
		String vendor = setup.getVendor();
		if(vendor == null || vendor.length() == 0) {
			throw new RuntimeException("No vendor declared in the setup configuration.");
		} else {
			controlWriter.write("Vendor: " + vendor + NEWLINE);
		}
	}
	/**
	 * Write the dependencies to the file. If no dependencies are specified, the 'java-devel >= 1.8' dependencies will be used.
	 * If a service is specified the 'daemonize' dependency will also be added.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putDepends(OutputStreamWriter controlWriter)
	                throws IOException {
	    String depends = rpm.getDepends();
	    if(depends == null || depends.length() == 0 ) {
	        depends = "java-devel >= 1.8";
	    }
	    if(setup.getServices() != null && setup.getServices().size() > 0) {
	    	depends = depends + ", daemonize, initscripts";
	    }
	    controlWriter.write("Requires: " + depends + NEWLINE);
	}

	/**
	 * Write the architecture to the file. If no architecture is specified then 'noarch' will be used.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putArchitecture(OutputStreamWriter controlWriter)
			throws IOException {
		String architecture = rpm.getArchitecture();
		if(architecture == null || architecture.length() == 0) {
			architecture = "noarch";
		}
		controlWriter.write("BuildArchitectures: " + architecture + NEWLINE);
	}

	/**
	 * Write the section to the file.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putSection(OutputStreamWriter controlWriter)
			throws IOException {
		String section = rpm.getSection();
		if(section != null && section.length() > 0) {
			controlWriter.write("Group: " + section + NEWLINE);
		}
	}

	/**
	 * Write the version to the file. The version is mandatory. If no version is declared a runtime exception will be thrown.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putVersion(OutputStreamWriter controlWriter)
			throws IOException {
		String version = setup.getVersion();
		if(version == null || version.length() == 0) {
			throw new RuntimeException("No version declared in the setup configuration.");
		} else {
			controlWriter.write("Version: " + version + NEWLINE);
		}
	}

	/**
	 * Write the summary to the file. The summary is mandatory. If no summary is declared a runtime exception will be thrown.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putSummary(OutputStreamWriter controlWriter)
			throws IOException {
		String summary = rpm.getSummary();
		if(summary == null || summary.length() == 0) {
			throw new RuntimeException("No summary declared in the setup configuration.");
		} else {
			controlWriter.write("Summary: " + summary + NEWLINE);
		}
	}
	
	/**
	 * Write the package to the file. The package is mandatory. If no package is declared a runtime exception will be thrown.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putName(OutputStreamWriter controlWriter)
			throws IOException {
		String packages = setup.getAppIdentifier();
		if(packages == null || packages.length() == 0) {
			throw new RuntimeException("No package declared in the setup configuration.");
		} else {
			controlWriter.write("Name: " + packages + NEWLINE);
		}
	}

	/**
	 * Adds a config file
	 * @param file the config file
	 */
	public void addConfFile(String file) {
	    confFiles.add( file );
	}
	
	 
    /**
	 * Adds a fragment to the install script at the specified section.
	 * These sections are the pre, post, preun and the postun sections.
	 * @param script the install script section
	 * @param scriptFragment the fragment to add
	 */
	public void addScriptFragment(Script script, String scriptFragment) {
	    StringBuilder sb = scriptMap.get( script );
	    if ( sb == null ) {
	        sb = new StringBuilder();
	        scriptMap.put( script, sb );
		    } else {
	        sb.append( "\n\n" );
	    }
	    sb.append( scriptFragment );
	}
	
}
