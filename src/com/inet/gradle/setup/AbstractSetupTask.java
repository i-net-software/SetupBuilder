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

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.file.CopyActionProcessingStreamAction;
import org.gradle.api.internal.file.FileLookup;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionExecuter;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.internal.file.copy.CopySpecInternal;
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.tasks.SimpleWorkResult;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.WorkResult;
import org.gradle.internal.nativeplatform.filesystem.FileSystem;
import org.gradle.internal.reflect.Instantiator;

/**
 * Base class for all setup task.
 * 
 * @author Volker Berlin
 */
public abstract class AbstractSetupTask extends DefaultTask implements SetupSources {

    private final CopySpecInternal rootSpec;

    private SetupBuilder           setupBuilder;

    private String                 extension;

    public AbstractSetupTask( String extension ) {
        this.extension = extension;
        this.rootSpec = (CopySpecInternal)getProject().copySpec( null );
    }

    /**
     * The action called from Gradle
     */
    @TaskAction
    public void action() {
        build();
        File setupFile = getSetupFile();
        if( !setupFile.exists() ) {
            throw new GradleException( "Setup file was not created: " + setupFile );
        }
        getProject().getArtifacts().add( "archives", setupFile );
    }

    /**
     * Copy all files of this task to the given target.
     * @param target the target directory
     */
    protected void copyTo( File target ) {
        processFiles( new CopyActionProcessingStreamAction() {
            @Override
            public void processFile( FileCopyDetailsInternal details ) {
                details.copyTo( details.getRelativePath().getFile( target ) );
            }
        } );
    }

    /**
     * Handle all files of this task.
     * @param action the action that should be process for every file
     */
    protected void processFiles( CopyActionProcessingStreamAction action ) {
        processFiles( action, getSetupBuilder().getRootSpec() );
        processFiles( action, rootSpec );
    }

    /**
     * Handle all files of the CopySpec.
     * @param action the action that should be process for every file
     */
    private void processFiles( CopyActionProcessingStreamAction action, CopySpecInternal copySpec ) {
        CopyActionExecuter copyActionExecuter = new CopyActionExecuter( getInstantiator(), getFileSystem() );
        CopyAction copyAction = new CopyAction() {
            @Override
            public WorkResult execute( CopyActionProcessingStream stream ) {
                stream.process( action );
                return new SimpleWorkResult( true );
            }
        };
        copyActionExecuter.execute( copySpec, copyAction );
    }

    @Inject
    protected Instantiator getInstantiator() {
        throw new UnsupportedOperationException();
    }

    @Inject
    protected FileSystem getFileSystem() {
        throw new UnsupportedOperationException();
    }

    @Inject
    protected FileResolver getFileResolver() {
        throw new UnsupportedOperationException();
    }

    @Inject
    protected FileLookup getFileLookup() {
        throw new UnsupportedOperationException();
    }

    /**
     * The platform depending build.
     */
    public abstract void build();

    public SetupBuilder getSetupBuilder() {
        if( setupBuilder == null ) {
            ProjectInternal project = (ProjectInternal)getProject();
            setupBuilder = project.getExtensions().getByType( SetupBuilder.class );
        }
        return setupBuilder;
    }

    @Override
    public CopySpecInternal getRootSpec() {
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
