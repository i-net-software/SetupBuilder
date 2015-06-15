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
        return "1.0.0.0";
    }

    public void setVersion( String version ) {
        this.version = version;
    }

    public String getBaseName() {
        if( baseName != null ) {
            return baseName;
        }
        return project.getName();
    }

    public void setBaseName( String baseName ) {
        this.baseName = baseName;
    }
}
