/*
 * Copyright 2015 i-net software
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
import java.util.Map;

import com.inet.gradle.setup.SetupBuilder;

/**
 * Builder for the control, postinst and prerm files, that are required for the Debian package tool.
 * <dl>
 * 		<dt>control</dt>
 * 			<dd>contains settings for the package like dependencies, architecture, description</dd>
 * 		<dt>postinst</dt>
 * 			<dd>contains scripts and commands that are executed after the files are copied</dd>
 * 		<dt>prerm</dt>
 * 			<dd>contains scripts and commands that are executed before the files are removed</dd>
 * </dl>init.d
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
    
//    Map<Script, StringBuilder> scriptHeadMap = new HashMap<>();
    Map<Script, StringBuilder> scriptMap = new HashMap<>();
    
    
    /**
     * the constructor setting the fields
     * @param deb the task for the debian package
     * @param setup the generic task for all setups
     * @param buildDir the directory to build the package in
     */
    RpmControlFileBuilder( Rpm rpm, SetupBuilder setup, File buildDir ) {
        this.rpm = rpm;
        this.setup = setup;
        this.buildDir = buildDir;
    }

    /**
     * Create the configuration files for the Debian package based on the settings in the task.
     * 
     * @throws Exception
     */
    void build() throws Exception {
    	
    	createControlFile();
//      	createScripts();
    }

    /**
     * Creates the 'control' file for the Debian package
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
			File spec = new File(buildDir, setup.getBaseName() + ".spec");
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
		ArrayList<String> preuns = rpm.getPreun();
		for (String preun : preuns) {
			controlWriter.write(preun + NEWLINE);	
		}
		StringBuilder prep_script = scriptMap.get( Script.PRERM );
		if(prep_script != null) {
			controlWriter.write(prep_script.toString() + NEWLINE);
		}
	}

	/**
	 * This scripts is executes after the package has been installed.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putPost(OutputStreamWriter controlWriter) throws IOException {
		controlWriter.write(NEWLINE + "%post" + NEWLINE);
		ArrayList<String> posts = rpm.getPost();
		for (String post : posts) {
			controlWriter.write(post + NEWLINE);	
		}	
		StringBuilder prep_script = scriptMap.get( Script.POSTINST );
		if(prep_script != null) {
			controlWriter.write(prep_script.toString() + NEWLINE);
		}
	}
	
	/**
	 * This scripts is executes after the package has been installed.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putPostun(OutputStreamWriter controlWriter) throws IOException {
		controlWriter.write(NEWLINE + "%postun" + NEWLINE);
		ArrayList<String> posts = rpm.getPostun();
		for (String post : posts) {
			controlWriter.write(post + NEWLINE);	
		}	
		StringBuilder prep_script = scriptMap.get( Script.POSTRM );
		if(prep_script != null) {
			controlWriter.write(prep_script.toString() + NEWLINE);
		}
	}
	
	/**
	 * Specifies the files that should be installed
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putFiles(OutputStreamWriter controlWriter) throws IOException {
		controlWriter.write(NEWLINE + "%files" + NEWLINE);
		controlWriter.write("/usr/**/*" + NEWLINE);
		if(setup.getServices() != null && setup.getServices().size() > 0) {
			controlWriter.write("%config /etc/init.d/*" + NEWLINE);	
		}
		
	}

	/**
	 * This is used to clean up the build directory tree. Normally RPM does this for you.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putClean(OutputStreamWriter controlWriter) throws IOException {
		controlWriter.write(NEWLINE + "%clean" + NEWLINE);
		String release = rpm.getRelease();
		if(release == null || release.length() == 0) {
			release = "1";
		}
		controlWriter.write("cp ../SRPMS/" + setup.getBaseName() + "-" + setup.getVersion() + "-" + release + ".src.rpm " + setup.getDestinationDir().getAbsolutePath() + NEWLINE);
		controlWriter.write("mv -f ../RPMS/noarch/" + setup.getBaseName() + "-" + setup.getVersion() + "-" + release + ".noarch.rpm " + setup.getDestinationDir().getAbsolutePath() + "/" + setup.getBaseName() + "-" + setup.getVersion() + ".rpm" + NEWLINE);
		ArrayList<String> cleans = rpm.getClean();
		for (String clean : cleans) {
			controlWriter.write(clean + NEWLINE);	
		}
	}

	/**
	 * Contains the necessary steps to install the build software
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putInstall(OutputStreamWriter controlWriter) throws IOException {
		controlWriter.write(NEWLINE + "%install" + NEWLINE);
		controlWriter.write("cp -R usr %{buildroot}" + NEWLINE);
		controlWriter.write("cp -R etc %{buildroot}" + NEWLINE);
		ArrayList<String> installs = rpm.getInstall();
		for (String install : installs) {
			controlWriter.write(install + NEWLINE);	
		}
	}

	/**
	 * This will be executed after the prep scripts
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
		
		StringBuilder prep_script = scriptMap.get( Script.PREINST );
		if(prep_script != null) {
			controlWriter.write(prep_script.toString() + NEWLINE);
		}
		
	}

	/**
	 * Write the prefix to the file.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putPrefix(OutputStreamWriter controlWriter) 
			throws IOException {
		controlWriter.write("Prefix: /usr/share" + NEWLINE);
		
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
	 * Write the description to the file. The description is mandatory. If no description is declared a runtime exception will be thrown.
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
    private void putDescription( OutputStreamWriter controlWriter ) throws IOException {
        String description = setup.getApplication() + "\n " + rpm.getDescription();
        controlWriter.write( NEWLINE + "%define __jar_repack %{nil}" + NEWLINE );
        controlWriter.write( NEWLINE + "%description" + NEWLINE + description + NEWLINE );
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
	 * Write the dependencies to the file. If no dependencies are specified, the jdk >= 1.8 dependencies will be used.
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
	    	depends = depends + ", daemonize";
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
	 * Write the section to the file. If no section is specified then 'java' will be used.
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
		String packages = setup.getBaseName();
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
	 * Adds a fragment to the specified install script at the tail section.
	 * @param script the install script
	 * @param scriptFragment the fragment to add
	 */
//	public void addTailScriptFragment(Script script, String scriptFragment) {
//	    StringBuilder sb = scriptTailMap.get( script );
//	    if ( sb == null ) {
//	        sb = new StringBuilder();
//	        scriptTailMap.put( script, sb );
//	    } else {
//	        sb.append( "\n\n" );
//	    }
//	    sb.append( scriptFragment );
//	}
	
	
	 
    /**
	 * Adds a fragment to the install script at the specified section.
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

	/**
	 * Adds a fragment to the specified install script at the head section.
	 * @param script the install script
	 * @param scriptFragment the fragment to add
	 */
//	public void addHeadScriptFragment(Script script, String scriptFragment) {
//	    StringBuilder sb = scriptHeadMap.get( script );
//	    if ( sb == null ) {
//	        sb = new StringBuilder();
//	        scriptHeadMap.put( script, sb );
//	    } else {
//	        sb.append( "\n\n" );
//	    }
//	    sb.append( scriptFragment );
//	}
	
	/**
	 * Creates the {post|pre}{inst|rm} install script files. Only scripts are generated when
	 * they are required.
	 * @throws IOException on I/O failures
	 */
//    private void createScripts() throws IOException {
//        for(Script script: Script.values()) {
//        	String scriptName = script.toString().toLowerCase();
//            Template tmpl = new Template( "deb/template/"+ scriptName+".sh" );
//            StringBuilder head = scriptHeadMap.get( script );
//            StringBuilder tail = scriptTailMap.get( script );
//            
//            if (head == null && tail == null) {
//                continue;
//            }
//            
//            if (head != null) {
//                tmpl.setPlaceholder( "head", head.toString() );
//            } else {
//            	tmpl.setPlaceholder( "head", "" );
//            }
//            
//            if (tail != null) {
//            	tmpl.setPlaceholder( "tail", tail.toString() );
//            } else {
//            	tmpl.setPlaceholder( "tail", "" );
//            }
//            
//            File file = new File(buildDir, scriptName);
//            tmpl.writeTo( file );
//            Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
//            perms.add( PosixFilePermission.OWNER_READ );
//            perms.add( PosixFilePermission.OWNER_WRITE );
//            perms.add( PosixFilePermission.GROUP_READ );
//            perms.add( PosixFilePermission.OTHERS_READ );
//            perms.add( PosixFilePermission.OWNER_EXECUTE );
//            perms.add( PosixFilePermission.GROUP_EXECUTE );
//            perms.add( PosixFilePermission.OTHERS_EXECUTE );
//            Files.setPosixFilePermissions( file.toPath(), perms );
//        }
//    }
}
