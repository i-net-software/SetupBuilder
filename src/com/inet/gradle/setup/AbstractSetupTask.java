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

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.file.copy.CopySpecInternal;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;

/**
 * Base class for all setup task.
 * 
 * @author Volker Berlin
 */
public abstract class AbstractSetupTask extends DefaultTask implements SetupSources {

    private final CopySpecInternal rootSpec;

    private SetupBuilder           setupBuilder;

    private String                 extension;

    public AbstractSetupTask() {
        this.rootSpec = (CopySpecInternal)getProject().copySpec( null );
    }

    public SetupBuilder getSetupBuilder() {
        if( setupBuilder == null ) {
            ProjectInternal project = (ProjectInternal)getProject();
            setupBuilder = project.getExtensions().getByType( SetupBuilder.class );
        }
        return setupBuilder;
    }

    @Override
    public CopySpecInternal getCopySpec() {
        return rootSpec;
    }

    /**
     * Overridden for annotation. {@inheritDoc}
     */
    @InputFiles
    @Override
    public FileTree getSource() {
        return SetupSources.super.getSource();
    }

    @InputFiles
    public FileTree getSetupSource() {
        return getSetupBuilder().getSource();
    }

    @OutputFile
    public File getSetupFile() {
        SetupBuilder setup = getSetupBuilder();
        return new File( setup.getDestinationDir(), setup.getSetupName() + "." + getExtension() );
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension( String extension ) {
        this.extension = extension;
    }
}
