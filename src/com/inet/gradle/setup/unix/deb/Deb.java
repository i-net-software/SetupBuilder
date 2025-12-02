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

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

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
    
    private String  compression;

    /** Creates a new Debian package builder. */
    public Deb() {
        super( "deb" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void build() {
        new DebBuilder( this, getSetupBuilder(), getFileResolver() ).build();
    }

    /**
     * Gets the package priority.
     * @return the package priority
     */
    @Input
    @Optional
    public String getPriority() {
        return priority;
    }

    /**
     * Sets the package priority.
     * @param priority the package priority
     */
    public void setPriority( String priority ) {
        this.priority = priority;
    }

    /**
     * Gets the estimated installation size.
     * @return the estimated installation size
     */
    @Input
    @Optional
    public String getInstallSize() {
        return installSize;
    }

    /**
     * Sets the estimated installation size.
     * @param installSize the estimated installation size
     */
    public void setInstallSize( String installSize ) {
        this.installSize = installSize;
    }

    /** {@inheritDoc} */
    @Input
    @Optional
    @Override
    public String getRecommends() {
        return recommends;
    }

    /** {@inheritDoc} */
    @Override
    public void setRecommends( String recommends ) {
        this.recommends = recommends;
    }

    /**
     * Checks if package validation should be performed.
     * @return true if package validation should be performed
     */
    public boolean shouldCheckPackage() {
        return checkPackage;
    }

    /**
     * Sets whether package validation should be performed.
     * @param checkPackage true if package validation should be performed
     */
    public void setCheckPackage( boolean checkPackage ) {
        this.checkPackage = checkPackage;
    }

    /**
     * Return the options used for the 'lintian' process.
     * They will only have impact when checkPackages is enabled.
     * @return the lintianOptions
     */
    @Input
    public String getLintianOptions() {
        return lintianOptions;
    }

    /**
     * Set the options used for the 'linting' process.
     * They will only have impact when checkPackages is enabled.
     * @param lintianOptions the lintianOptions to set
     */
    public void setLintianOptions( String lintianOptions ) {
        this.lintianOptions = lintianOptions;
    }

    /**
     * Gets the maintainer email address.
     * @return the maintainer email address
     */
    @Input
    @Optional
    public String getMaintainerEmail() {
        return maintainerEmail;
    }

    /**
     * Sets the maintainer email address.
     * @param maintainerEmail the maintainer email address
     */
    public void setMaintainerEmail( String maintainerEmail ) {
        this.maintainerEmail = maintainerEmail;
    }

    /**
     * Gets the changelog entry.
     * @return the changelog entry
     */
    @Input
    @Optional
    public String getChanges() {
        return changes;
    }

    /**
     * Sets the changelog entry.
     * @param changes the changelog entry
     */
    public void setChanges( String changes ) {
        this.changes = changes;
    }
    
    /**
     * Gets the compression method for the package.
     * @return the compression method
     */
    @Input
    @Optional
    public String getCompression() {
        return compression;
    }
    
    /**
     * Sets the compression method for the package.
     * @param compression the compression method
     */
    public void setCompression( String compression ) {
        this.compression = compression;
    }

    @Input
    @Override
    public String getArchitecture() {
        String architecture = super.getArchitecture();
        if( architecture == null || architecture.length() == 0 ) {
            architecture = "all";
        }
        return architecture;
    }
}
