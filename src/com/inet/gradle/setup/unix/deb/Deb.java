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
package com.inet.gradle.setup.unix.deb;

import org.gradle.api.internal.project.ProjectInternal;

import com.inet.gradle.setup.unix.Unix;

/**
 * The deb Gradle task. It build a deb package for Debian / Ubuntu.
 *
 * @author Stefan Heidrich
 */
public class Deb extends Unix {

    private String  priority;

    private String  installSize;

    private String  recommends;

    private boolean checkPackage = false;

    private String  lintianOptions = "";

    private String  maintainerEmail;

    private String  changes;

    public Deb() {
        super( "deb" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void build() {
        ProjectInternal project = (ProjectInternal)getProject();
        new DebBuilder( this, getSetupBuilder(), project.getFileResolver() ).build();
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority( String priority ) {
        this.priority = priority;
    }

    public String getInstallSize() {
        return installSize;
    }

    public void setInstallSize( String installSize ) {
        this.installSize = installSize;
    }

    @Override
    public String getRecommends() {
        return recommends;
    }

    @Override
    public void setRecommends( String recommends ) {
        this.recommends = recommends;
    }

    public boolean shouldCheckPackage() {
        return checkPackage;
    }

    public void setCheckPackage( boolean checkPackage ) {
        this.checkPackage = checkPackage;
    }

    /**
     * Return the options used for the 'lintian' process.
     * They will only have impact when checkPackages is enabled.
     * @return the lintianOptions
     */
    public String getLintianOptions() {
        return lintianOptions;
    }

    /**
     * Set the options used for the 'lintian' process.
     * They will only have impact when checkPackages is enabled.
     * @param lintianOptions the lintianOptions to set
     */
    public void setLintianOptions( String lintianOptions ) {
        this.lintianOptions = lintianOptions;
    }

    public String getMaintainerEmail() {
        return maintainerEmail;
    }

    public void setMaintainerEmail( String maintainerEmail ) {
        this.maintainerEmail = maintainerEmail;
    }

    public String getChanges() {
        return changes;
    }

    public void setChanges( String changes ) {
        this.changes = changes;
    }

    @Override
    public String getArchitecture() {
        String architecture = super.getArchitecture();
        if( architecture == null || architecture.length() == 0 ) {
            architecture = "all";
        }
        return architecture;
    }
}
