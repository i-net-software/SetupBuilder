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
	
    private String                 section;

//    private String                 priority;

    private String                 architecture;

//    private String                 installSize;
    
//    private String                 recommends;
    
    private String                 	depends;
    
    private String                 	homepage;
    
    private String				   	maintainerEmail;
    
    private String				   	changes;
    
    private String				   	summary;
    
    private String				   	release;
    
    private String				   	license;
    
    private String				   	url;
    
    private ArrayList<String>	   	prep = new ArrayList<String>();

	private ArrayList<String> 	   	build = new ArrayList<String>();

	private ArrayList<String> 	   	install = new ArrayList<String>();

	private ArrayList<String> 	  	clean = new ArrayList<String>();

	private ArrayList<String> 		post = new ArrayList<String>();

	private ArrayList<String> 		preun = new ArrayList<String>();
	
	private ArrayList<String> 		postun = new ArrayList<String>();

	

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
     * {@inheritDoc}
     */
    @Override
    protected void copyTo(File target) {
    	super.copyTo(target);
    }
      

	public String getSection() {
		return section;
	}


	public void setSection(String section) {
		this.section = section;
	}


//	public String getPriority() {
//		return priority;
//	}
//
//
//	public void setPriority(String priority) {
//		this.priority = priority;
//	}


	public String getArchitecture() {
		return architecture;
	}


	public void setArchitecture(String architecture) {
		this.architecture = architecture;
	}


//	public String getInstallSize() {
//		return installSize;
//	}
//
//
//	public void setInstallSize(String installSize) {
//		this.installSize = installSize;
//	}
//
//
//	public String getRecommends() {
//		return recommends;
//	}
//
//
//	public void setRecommends(String recommends) {
//		this.recommends = recommends;
//	}
//
//
	public String getDepends() {
		return depends;
	}


	public void setDepends(String depends) {
		this.depends = depends;
	}


	public String getHomepage() {
		return homepage;
	}


	public void setHomepage(String homepage) {
		this.homepage = homepage;
	}


	public String getMaintainerEmail() {
		return maintainerEmail;
	}


	public void setMaintainerEmail(String maintainerEmail) {
		this.maintainerEmail = maintainerEmail;
	}


	public String getChanges() {
		return changes;
	}


	public void setChanges(String changes) {
		this.changes = changes;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getRelease() {
		return release;
	}

	public void setRelease(String release) {
		this.release = release;
	}

	public String getLicense() {
		return license;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public ArrayList<String> getPrep() {
		return prep;
	}

	public void setPrep(String prep) {
		this.prep.add( prep );
	}

	public ArrayList<String> getPreun() {
		return preun;
	}
	
	public void setPreun(String preun) {
		this.preun.add( preun );
	}

	public ArrayList<String> getPost() {
		return post;
	}
	
	public void setPost(String post) {
		this.post.add( post );
	}
	
	public ArrayList<String> getPostun() {
		return postun;
	}
	
	public void setPostun(String post) {
		this.postun.add( post );
	}

	public ArrayList<String> getClean() {
		return clean;
	}
	
	public void setClean(String clean) {
		this.clean.add( clean );
	}

	public ArrayList<String> getInstall() {
		return install;
	}
	
	public void setInstall(String install) {
		this.install.add( install );
	}

	public ArrayList<String> getBuild() {
		return build;
	}
	
	public void setBuild(String build) {
		this.build.add( build );
	}
    
}
