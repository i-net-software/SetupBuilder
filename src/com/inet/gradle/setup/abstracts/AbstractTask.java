/*
 * Copyright 2015 - 2016 i-net software
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
package com.inet.gradle.setup.abstracts;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.Set;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.artifacts.publish.DefaultPublishArtifact;
import org.gradle.api.internal.file.CopyActionProcessingStreamAction;
import org.gradle.api.internal.file.FileLookup;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionExecuter;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.internal.file.copy.CopySpecInternal;
import org.gradle.api.internal.file.copy.CopySpecResolver;
import org.gradle.api.internal.file.copy.DefaultCopySpec;
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.WorkResult;
/*// if gradleVersion < 4.2
import org.gradle.api.internal.tasks.SimpleWorkResult;
import org.gradle.internal.nativeplatform.filesystem.FileSystem;
*/// else
import org.gradle.api.tasks.WorkResults;
import org.gradle.internal.nativeintegration.filesystem.FileSystem;
//// endif
import org.gradle.internal.reflect.Instantiator;

import com.inet.gradle.setup.util.TempPath;

import groovy.lang.Closure;

/**
 * Base class for all setup task.
 *
 * @author Volker Berlin
 */
public abstract class AbstractTask extends DefaultTask implements SetupSources {

    private final CopySpecInternal rootSpec;

    private AbstractSetupBuilder   setupBuilder;

    private String                 extension, classifier, version;

    /**
     * Constructor with indication to artifact result
     * Runs with the default SetupBuilder for dmg, msi ...
     * @param extension of the setup
     * @param setupType the class of the SetupBuilder
     */
    public AbstractTask( String extension, Class<? extends AbstractSetupBuilder> setupType ) {
        this.extension = extension;
        this.rootSpec = (CopySpecInternal)getProject().copySpec( (Closure<CopySpec>)null );

        ProjectInternal project = (ProjectInternal)getProject();
        setupBuilder = project.getExtensions().getByType( setupType );
        setGroup( "build" ); // for displaying in buildship

        try {
            TempPath.clearTemporaryFolder( getTemporaryDir().toPath() );
        } catch( Exception e ) {
            // Ignore
        }
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
        Configuration archives = getProject().getConfigurations().getByName( "archives" );
        archives.getArtifacts().add( new DefaultPublishArtifact( setupBuilder.getAppIdentifier(), extension, extension, classifier, new Date(setupFile.lastModified()), setupFile, this ) );
    }

    /**
     * Copy all files of this task to the given target.
     * @param target the target directory
     */
    public void copyTo( File target ) {
        processFiles( new CopyActionProcessingStreamAction() {
            @Override
            public void processFile( FileCopyDetailsInternal details ) {
//                details.copyTo( details.getRelativePath().getFile( target ) ); // didn't work with mounted smb devices under Unix
                if( !details.isDirectory() ) {
                    try {
                        File f = details.getRelativePath().getFile( target );
                        if(!f.getParentFile().exists()) {
                            f.getParentFile().mkdirs(); // the parent directory must be created, else the copy fails
                        }
                        try( InputStream input = details.open() ) {
                            Files.copy( input, f.toPath(), StandardCopyOption.REPLACE_EXISTING );
                        }
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
        processFiles( action, setupBuilder.getRootSpec() );
        processFiles( action, rootSpec );
    }

    /**
     * Handle all files of the CopySpec.
     * @param action the action that should be process for every file
     */
    private void processFiles( CopyActionProcessingStreamAction action, CopySpecInternal copySpec ) {
        if( setupBuilder.isFailOnEmptyFrom() ) {
            for( CopySpecInternal cs : copySpec.getChildren() ) {
                CopySpecResolver rootResolver = cs.buildRootResolver();
                Set<File> files = rootResolver.getAllSource().getFiles();
                if( files.size() == 0 ) {
                    throw new IllegalArgumentException( "No files selected by: " + ((DefaultCopySpec)cs).getSourcePaths() + " --> " + rootResolver.getDestPath() + ". This means that there are files missing or your 'from' method in your gradle script is wrong. If an empty 'from' is valid then disable the check with 'setupBuilder.failOnEmptyFrom = false'" );
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

        /*// if gradleVersion < 3.4
        CopyActionExecuter copyActionExecuter = new CopyActionExecuter( getInstantiator(), getFileSystem() );
        */// else
        CopyActionExecuter copyActionExecuter = new CopyActionExecuter( getInstantiator(), getFileSystem(), true );
        //// endif

        CopyAction copyAction = new CopyAction() {
            @Override
            public WorkResult execute( CopyActionProcessingStream stream ) {
                stream.process( action );
                /*// if gradleVersion < 4.2
                return new SimpleWorkResult( true );
                */// else
                return WorkResults.didWork( true );
                //// endif
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

    /**
     * Return the setupBuilder using the specified type
     * @return setupBuilder
     */
    @Internal
    protected AbstractSetupBuilder getAbstractSetupBuilder() {
        return setupBuilder;
    }

    @Override
    @Internal
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

    /**
     * The setup Sources
     * @return FileTree
     */
    @InputFiles
    public FileTree getSetupSource() {
        try {
            return setupBuilder.getSource();
        } catch ( Throwable e ) {
            throw new IllegalArgumentException( "You have to specify input sources for your application", e );
        }
    }

    /**
     * The resulting application
     * @return the application
     */
    @OutputFile
    public File getSetupFile() {
        StringBuilder setupFile = new StringBuilder(setupBuilder.getArchiveName());
        if( getClassifier() != null && !getClassifier().isEmpty() ) {
            setupFile.append( '-' );
            setupFile.append( getClassifier() );
        }
        setupFile.append( '.' );
        setupFile.append( getExtension() );
        return new File( setupBuilder.getDestinationDir(), setupFile.toString() );
    }

    /**
     * Get the file extension.
     *
     * @return the extension
     */
    @Input
    @Optional
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
     * Returns the classifier part of the installer, if any.
     *
     * @return The classifier. May be null.
     */
    @Input
    @Optional
    public String getClassifier() {
        return classifier;
    }

    /**
     * Set the classifier part of the installer.
     *
     * @param classifier The classifier. May be null.
     */
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Input
    public String getDescription() {
        String desc = super.getDescription();
        if( desc != null && !desc.isEmpty() ) {
            return desc;
        }
        return setupBuilder.getDescription();
    }

    /**
     * Get the version of the task. If not set the version of the setup is returned
     * @return the version
     */
    @Input
    public String getVersion() {
        if ( version != null ) {
            return version;
        }

        return setupBuilder.getVersion();
    }

    /**
     * Set the version of the task
     * @param version the version to set
     */
    public void setVersion( String version ) {
        this.version = version;
    }
}
