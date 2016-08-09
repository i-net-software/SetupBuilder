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
import java.util.ArrayList;

import org.gradle.api.internal.project.ProjectInternal;

import com.inet.gradle.setup.AbstractSetupTask;

/**
 * The rpm Gradle task. It build a rpm package for Linux.
 * 
 * @author Volker Berlin
 */
public class Rpm extends AbstractSetupTask {
	
    private String                  section;

    private String                  architecture;
    
    private String                 	depends;
    
    private String				   	summary;
    
    private String				   	release;
    
    private String				   	license;
    
    private String				   	url;
    
    private String				   	daemonUser = "root";
    
    private String				   	installationRoot;
    
    private String				   	pamConfigurationFile;
    
    private boolean					backwardCompatible = true;
    
    private ArrayList<String>	   	prep = new ArrayList<String>();

	private ArrayList<String> 	   	build = new ArrayList<String>();

	private ArrayList<String> 	   	install = new ArrayList<String>();

	private ArrayList<String> 	  	clean = new ArrayList<String>();

	private ArrayList<String> 		pre = new ArrayList<String>();
	
	private ArrayList<String> 		post = new ArrayList<String>();

	private ArrayList<String> 		preun = new ArrayList<String>();
	
	private ArrayList<String> 		postun = new ArrayList<String>();

	private Object defaultServiceFile;

	

	/**
	 * the default constructor
	 */
    public Rpm() {
        super( "rpm" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void build() {
        ProjectInternal project = (ProjectInternal)getProject();
        new RpmBuilder( this, getSetupBuilder(), project.getFileResolver() ).build();
    }
      
    /**
     * Returns the section that specifies the 'Group' entry in the SPEC file. Default is Applications/Productivity
     * 
     * @return the section
     */
    public String getSection() {
        if( section != null ) {
            return section;
        }
        return "Applications/Productivity";
    }

	/**
	 * Sets the value for the 'Group' entry in the SPEC file.
	 * @param section the value for the entry
	 */
	public void setSection(String section) {
		this.section = section;
	}

	/**
     * Returns the architecture that specifies the 'BuildArchitectures' entry in the SPEC file.
     * @return the architecture specified in the gradle script
     */
	public String getArchitecture() {
		return architecture;
	}

	/**
	 * Sets the value for the 'BuildArchitectures' entry in the SPEC file.
	 * @param architecture the value for the entry
	 */
	public void setArchitecture(String architecture) {
		this.architecture = architecture;
	}

	/**
	 * Returns the dependency that should be used in the 'Requires' entry in the SPEC file.
	 * @return the dependency specified in the gradle script
	 */
	public String getDepends() {
		return depends;
	}

	/**
	 * Sets the value for the 'Requires' entry in the SPEC file.
	 * @param depends the value for the entry
	 */
	public void setDepends(String depends) {
		this.depends = depends;
	}

    /**
     * Returns the summary that should be used in the 'Summary' entry in the SPEC file.
     * 
     * @return the summary specified in the gradle script
     */
    public String getSummary() {
        if( summary != null ) {
            return summary;
        }
        return getSetupBuilder().getApplication();
    }

	/**
	 * Sets the value for the 'Summary' entry in the SPEC file.
	 * @param summary the value for the entry
	 */
	public void setSummary(String summary) {
		this.summary = summary;
	}

	/**
	 * Returns the release that should be used in the 'Release' entry in the SPEC file.
	 * @return the release specified in the gradle script
	 */
	public String getRelease() {
		return release;
	}

	/**
	 * Sets the value for the 'Release' entry in the SPEC file.
	 * @param release the value for the entry
	 */
	public void setRelease(String release) {
		this.release = release;
	}

	/**
	 * Returns the license that should be used in the 'License' entry in the SPEC file.
	 * @return the license specified in the gradle script
	 */
	public String getLicense() {
		return license;
	}

	/**
	 * Sets the value for the 'License' entry in the SPEC file.
	 * @param license the value for the entry
	 */
	public void setLicense(String license) {
		this.license = license;
	}

	/**
	 * Returns the url that should be used in the 'URL' entry in the SPEC file.
	 * @return the url specified in the gradle script
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Sets the value for the 'URL' entry in the SPEC file.
	 * @param url the value for the entry
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Returns the user with which the daemon should be running. If no user was specified the default root user will be used. 
	 * @return the user for the daemon
	 */
	public String getDaemonUser() {
		if(daemonUser.trim().isEmpty()) {
			return "root";
		} else {
			return daemonUser;
		}
	}

	/**
	 * Sets the user with which the daemon should be running.
	 */
	public void setDaemonUser(String daemonUser) {
		this.daemonUser = daemonUser;
	}

	/**
	 * Returns the name with path of the PAM configuration file that should be installed 
	 * @return the name with path of the PAM configuration file
	 */
	public String getPamConfigurationFile() {
		return pamConfigurationFile;
	}

	/**
	 * Sets the name of the PAM configuration file
	 * @param pamConfigurationFile the name of the PAM configuration file
	 */
	public void setPamConfigurationFile(String pamConfigurationFile) {
		this.pamConfigurationFile = pamConfigurationFile;
	}

	/**
	 * Returns the backward compatibility for old rpm versions
	 * @return the backward compatibility for old rpm versions
	 */
	public boolean isBackwardCompatible() {
		return backwardCompatible;
	}

	/**
	 * Sets the backward compatibility for old rpm versions
	 * @param backwardCompatibility the backward compatibility for old rpm versions
	 */
	public void setBackwardCompatible(boolean backwardCompatibility) {
		this.backwardCompatible = backwardCompatibility;
	}

	/**
	 * Returns the prep that should be used in the '%prep' entry in the SPEC file.
	 * @return the prep specified in the gradle script
	 */
	public ArrayList<String> getPrep() {
		return prep;
	}

	/**
	 * Sets the value for the '%prep' entry in the SPEC file.
	 * @param prep the value for the entry
	 */
	public void setPrep(String prep) {
		this.prep.add( prep );
	}

	/**
	 * Returns the preun that should be used in the '%preun' entry in the SPEC file.
	 * @return the preun specified in the gradle script
	 */
	public ArrayList<String> getPreun() {
		return preun;
	}
	
	/**
	 * Sets the value for the '%preun' entry in the SPEC file.
	 * @param preun the value for the entry
	 */
	public void setPreun(String preun) {
		this.preun.add( preun );
	}

	/**
	 * Returns the post that should be used in the '%post' entry in the SPEC file.
	 * @return the post specified in the gradle script
	 */
	public ArrayList<String> getPost() {
		return post;
	}
	
	/**
	 * Sets the value for the '%post' entry in the SPEC file.
	 * @param post the value for the entry
	 */
	public void setPost(String post) {
		this.post.add( post );
	}
	
	/**
	 * Returns the postun that should be used in the '%postun' entry in the SPEC file.
	 * @return the postun specified in the gradle script
	 */
	public ArrayList<String> getPostun() {
		return postun;
	}
	
	/**
	 * Sets the value for the '%postun' entry in the SPEC file.
	 * @param postun the value for the entry
	 */
	public void setPostun(String postun) {
		this.postun.add( postun );
	}

	/**
	 * Returns the clean that should be used in the '%clean' entry in the SPEC file.
	 * @return the clean specified in the gradle script
	 */
	public ArrayList<String> getClean() {
		return clean;
	}
	
	/**
	 * Sets the value for the '%clean' entry in the SPEC file.
	 * @param clean the value for the entry
	 */
	public void setClean(String clean) {
		this.clean.add( clean );
	}

	/**
	 * Returns the install that should be used in the '%install' entry in the SPEC file.
	 * @return the install specified in the gradle script
	 */
	public ArrayList<String> getInstall() {
		return install;
	}
	
	/**
	 * Sets the value for the '%install' entry in the SPEC file.
	 * @param install the value for the entry
	 */
	public void setInstall(String install) {
		this.install.add( install );
	}

	/**
	 * Returns the build that should be used in the '%build' entry in the SPEC file.
	 * @return the build specified in the gradle script
	 */
	public ArrayList<String> getBuild() {
		return build;
	}
	
	/**
	 * Sets the value for the '%build' entry in the SPEC file.
	 * @param build the value for the entry
	 */
	public void setBuild(String build) {
		this.build.add( build );
	}

	/**
	 * Returns the pre that should be used in the '%pre' entry in the SPEC file.
	 * @return the pre specified in the gradle script
	 */
	public ArrayList<String> getPre() {
		return pre;
	}

	/**
	 * Sets the value for the '%pre' entry in the SPEC file.
	 * @param pre the value for the entry
	 */
	public void setPre(String pre) {
		this.pre.add(pre);
	}

	/**
	 * Returns the installation root where the program directory should be located. Default is /usr/share +  basename
	 * @return the installation root directory
	 */
	public String getInstallationRoot() {
		if(installationRoot == null) {
			return "/usr/share/" + getSetupBuilder().getApplication();
		} else {
			return installationRoot;
		}
	}

	/**
	 * Sets the installation root directory where the main files should be located.
	 * If the directory ends with a / it will be removed. 
	 * @param installationRoot the installation root directory
	 */
	public void setInstallationRoot(String installationRoot) {		
		this.installationRoot = installationRoot.endsWith("/") ? installationRoot.substring(0, installationRoot.length()-1) : installationRoot;
	}


	/**
	 * Returns the a default service configuration file
	 * This will be included in the service starter
	 * @return the default service file
	 */
	public File getDefaultServiceFile() {
		if ( defaultServiceFile != null ) {
			return getProject().file( defaultServiceFile );
		}
		return null;
	}

	/**
	 * Set the default service configuration file
	 * The file will be put at the "/etc/default/$service$" location
	 * @param defaultServiceFile
	 */
	public void setDefaultServiceFile(Object defaultServiceFile) {
		this.defaultServiceFile = defaultServiceFile;
	}
}
