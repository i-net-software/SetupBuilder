/*
 * Copyright 2015 i-net software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.inet.gradle.setup.unix.rpm;

import java.util.ArrayList;

import org.gradle.api.internal.project.ProjectInternal;

import com.inet.gradle.setup.unix.Unix;

/**
 * The rpm Gradle task. It build a rpm package for Linux.
 * 
 * @author Volker Berlin
 */
public class Rpm extends Unix {

    private String            summary;

    private String            release;

    private String            license;

    private boolean           backwardCompatible = true;

    private ArrayList<String> prep               = new ArrayList<String>();

    private ArrayList<String> build              = new ArrayList<String>();

    private ArrayList<String> install            = new ArrayList<String>();

    private ArrayList<String> clean              = new ArrayList<String>();

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
     * 
     * @param summary
     *            the value for the entry
     */
    public void setSummary( String summary ) {
        this.summary = summary;
    }

    /**
     * Returns the release that should be used in the 'Release' entry in the SPEC file.
     * 
     * @return the release specified in the gradle script
     */
    public String getRelease() {
        return release;
    }

    /**
     * Sets the value for the 'Release' entry in the SPEC file.
     * 
     * @param release
     *            the value for the entry
     */
    public void setRelease( String release ) {
        this.release = release;
    }

    /**
     * Returns the license that should be used in the 'License' entry in the SPEC file.
     * 
     * @return the license specified in the gradle script
     */
    public String getLicense() {
        return license;
    }

    /**
     * Sets the value for the 'License' entry in the SPEC file.
     * 
     * @param license
     *            the value for the entry
     */
    public void setLicense( String license ) {
        this.license = license;
    }

    /**
     * Returns the backward compatibility for old rpm versions
     * 
     * @return the backward compatibility for old rpm versions
     */
    public boolean isBackwardCompatible() {
        return backwardCompatible;
    }

    /**
     * Sets the backward compatibility for old rpm versions
     * 
     * @param backwardCompatibility
     *            the backward compatibility for old rpm versions
     */
    public void setBackwardCompatible( boolean backwardCompatibility ) {
        this.backwardCompatible = backwardCompatibility;
    }

    /**
     * Returns the prep that should be used in the '%prep' entry in the SPEC file.
     * 
     * @return the prep specified in the gradle script
     */
    public ArrayList<String> getPrep() {
        return prep;
    }

    /**
     * Sets the value for the '%prep' entry in the SPEC file.
     * 
     * @param prep
     *            the value for the entry
     */
    public void setPrep( String prep ) {
        this.prep.add( prep );
    }

    /**
     * Returns the clean that should be used in the '%clean' entry in the SPEC file.
     * 
     * @return the clean specified in the gradle script
     */
    public ArrayList<String> getClean() {
        return clean;
    }

    /**
     * Sets the value for the '%clean' entry in the SPEC file.
     * 
     * @param clean
     *            the value for the entry
     */
    public void setClean( String clean ) {
        this.clean.add( clean );
    }

    /**
     * Returns the install that should be used in the '%install' entry in the SPEC file.
     * 
     * @return the install specified in the gradle script
     */
    public ArrayList<String> getInstall() {
        return install;
    }

    /**
     * Sets the value for the '%install' entry in the SPEC file.
     * 
     * @param install
     *            the value for the entry
     */
    public void setInstall( String install ) {
        this.install.add( install );
    }

    /**
     * Returns the build that should be used in the '%build' entry in the SPEC file.
     * 
     * @return the build specified in the gradle script
     */
    public ArrayList<String> getBuild() {
        return build;
    }

    /**
     * Sets the value for the '%build' entry in the SPEC file.
     * 
     * @param build
     *            the value for the entry
     */
    public void setBuild( String build ) {
        this.build.add( build );
    }
}
