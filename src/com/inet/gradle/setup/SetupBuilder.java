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
package com.inet.gradle.setup;

import java.io.File;

import org.gradle.api.Project;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.file.copy.CopySpecInternal;

/**
 * The Gradle extension for all setup tasks.
 * 
 * @author Volker Berlin
 */
public class SetupBuilder implements SetupSources {

    private final Project          project;

    private final CopySpecInternal rootSpec;

    private String                 vendor;

    private String                 application;

    private String                 version;

    private String                 baseName;

    private String                 setupName;

    private Object                 licenseFile;

    private Object                 icons;

    private Object                 bundleJre;

    private String                 bundleJreTarget;

    public SetupBuilder( Project project ) {
        this.project = project;
        this.rootSpec = (CopySpecInternal)project.copySpec( null );

        //init default location
        into( new File( "distributions" ) );
    }

    @Override
    public CopySpecInternal getCopySpec() {
        return rootSpec;
    }

    /**
     * Override to remove the annotation OutputDirectories in the Copy task. {@inheritDoc}
     */
    public File getDestinationDir() {
        return rootSpec.buildRootResolver().getDestPath().getFile( project.getBuildDir() );
    }

    public FileTree getSource() {
        return rootSpec.buildRootResolver().getAllSource();
    }

    /**
     * Sets the directory to copy files into. This is the same as calling {@link #into(Object)} on this task.
     *
     * @param destinationDir The destination directory. Must not be null.
     */
    public void setDestinationDir( File destinationDir ) {
        into( destinationDir );
    }

    public String getVendor() {
        if( vendor != null ) {
            return vendor;
        }
        return "My Company";
    }

    public void setVendor( String vendor ) {
        this.vendor = vendor;
    }

    public String getApplication() {
        if( application != null ) {
            return application;
        }
        return project.getName();
    }

    public void setApplication( String application ) {
        this.application = application;
    }

    public String getVersion() {
        if( version != null ) {
            return version;
        }
        Object version = project.getProperties().get( "version" );
        if( version != null ) {
            return version.toString();
        }
        return "1.0";
    }

    public void setVersion( String version ) {
        this.version = version;
    }

    /**
     * Get the base name of the setup file. If not set then the project name is used.
     * 
     * @return the base name
     */
    public String getBaseName() {
        if( baseName != null ) {
            return baseName;
        }
        return project.getName();
    }

    public void setBaseName( String baseName ) {
        this.baseName = baseName;
    }

    /**
     * Get the name of the setup file without extension. If not set then baseName-version is used.
     * 
     * @return the setup file name
     */
    public String getSetupName() {
        if( setupName != null ) {
            return setupName;
        }
        return getBaseName() + '-' + getVersion();
    }

    public void setSetupName( String setupName ) {
        this.setupName = setupName;
    }

    public File getLicenseFile() {
        if( licenseFile != null ) {
            return project.file( licenseFile );
        }
        return null;
    }

    public void setLicenseFile( Object licenseFile ) {
        this.licenseFile = licenseFile;
    }

    public Object getIcons() {
        return icons;
    }

    /**
     * Set the icons for the setup. This can be one or multiple images in different size. The usage depends on the
     * platform. This can be an *.ico file, *.icns file or an list of Java readable image files like *.png or *.jpeg.
     * 
     * @param icons the icons
     */
    public void setIcons( Object icons ) {
        this.icons = icons;
    }

    public Object getBundleJre() {
        return bundleJre;
    }

    public void setBundleJre( Object bundleJre ) {
        this.bundleJre = bundleJre;
    }

    public String getBundleJreTarget() {
        if( bundleJreTarget != null ) {
            return bundleJreTarget;
        }
        return "jre";
    }

    /**
     * The target directory in the install directory for a bundled JRE. The default is "jre".
     * 
     * @param bundleJreTarget the new value
     */
    public void setBundleJreTarget( String bundleJreTarget ) {
        this.bundleJreTarget = bundleJreTarget;
    }
}
