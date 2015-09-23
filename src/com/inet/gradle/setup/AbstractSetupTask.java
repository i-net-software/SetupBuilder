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

import groovy.lang.Closure;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Set;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.file.CopyActionProcessingStreamAction;
import org.gradle.api.internal.file.FileLookup;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionExecuter;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.internal.file.copy.CopySpecInternal;
import org.gradle.api.internal.file.copy.DefaultCopySpec;
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
        this.rootSpec = (CopySpecInternal)getProject().copySpec( (Closure<CopySpec>)null );
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
//                details.copyTo( details.getRelativePath().getFile( target ) ); // didn't work with mounted smb devises under Unix
                if( !details.isDirectory() ) {
                    try {
                    	File f = details.getRelativePath().getFile( target );
                    	if(!f.getParentFile().exists()) {
                    		f.getParentFile().mkdirs(); // the parent directory must be created, else the copy fails
                    	}
						Files.copy(details.getFile().toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING );
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
                }
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
        if( getSetupBuilder().isFailOnEmptyFrom() ) {
            for( CopySpecInternal cs : copySpec.getChildren() ) {
                Set<File> files = cs.buildRootResolver().getAllSource().getFiles();
                if( files.size() == 0 ) {
                    throw new IllegalArgumentException( "No files selected by: " + ((DefaultCopySpec)cs).getSourcePaths()  + ". This means that there are files missing or your 'from' method in your gradle script is wrong. If an empty 'from' is valid then disable the check with 'setupBuilder.failOnEmptyFrom = false'" );
                }
                int includeCount = cs.getIncludes().size();
                if( files.size() < includeCount ) {
                    StringBuilder msg = new StringBuilder( "Not every 'include' match a file by: " );
                    msg.append( ((DefaultCopySpec)cs).getSourcePaths() );
                    msg.append( "\n\tDeclared includes:");
                    for( String include : cs.getIncludes() ) {
                        msg.append( "\n\t\t" ).append( include );
                    }
                    msg.append( "\n\tMatching files:" );
                    for( File file : files ) {
                        msg.append( "\n\t\t" ).append( file );
                    }
                    throw new IllegalArgumentException( msg.toString()  );
                }
            }
        }

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

    /**
     * Get the file extension.
     * 
     * @return the extension
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Set the file extension of the installer. The default is equals the task name.
     * 
     * @param extension the file extension
     */
    public void setExtension( String extension ) {
        this.extension = extension;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        String desc = super.getDescription();
        if( desc != null && !desc.isEmpty() ) {
            return desc;
        }
        return getSetupBuilder().getDescription();
    }
}
