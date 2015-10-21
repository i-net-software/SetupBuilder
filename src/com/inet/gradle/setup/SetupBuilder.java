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
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.file.copy.CopySpecInternal;
import org.gradle.util.ConfigureUtil;

/**
 * The Gradle extension for all setup tasks.
 * 
 * @author Volker Berlin
 */
public class SetupBuilder implements SetupSources {

    private final Project          project;

    private final CopySpecInternal rootSpec;

    private Object                 destinationDir;

    private String                 vendor;

    private String                 application;

    private String                 version;

    private String                 description;

    private String                 appIdentifier;

    private String                 archiveName;

    private Object                 licenseFile;

    private Object                 icons;

    private Object                 bundleJre;

    private String                 bundleJreTarget;

    private String                 mainClass;

    private String                 mainJar;

    private DesktopStarter         runAfter, runBeforeUninstall;

    private List<DocumentType>     documentTypes   = new ArrayList<>();

    private List<Service>          services        = new ArrayList<>();

    private final List<DesktopStarter> desktopStarters = new ArrayList<>();

    private List<String>           deleteFiles = new ArrayList<>();

    private List<String>           deleteFolders = new ArrayList<>();

    private boolean                failOnEmptyFrom = true;

    /**
     * Create a new instance.
     * @param project current project
     */
    public SetupBuilder( Project project ) {
        this.project = project;
        this.rootSpec = (CopySpecInternal)project.copySpec( (Closure<CopySpec>)null );

        //init default location
        setDestinationDir( new File( "distributions" ) );
    }

    @Override
    public CopySpecInternal getRootSpec() {
        return rootSpec;
    }

    /**
     * Get the current project.
     * 
     * @return the project
     */
    Project getProject() {
        return project;
    }

    /**
     * Override to remove the annotation OutputDirectories in the Copy task. {@inheritDoc}
     */
    public File getDestinationDir() {
        return new File( project.getBuildDir(), String.valueOf( destinationDir ) );
    }

    public FileTree getSource() {
        return rootSpec.buildRootResolver().getAllSource();
    }

    /**
     * Sets the destination of this task.
     *
     * @param destinationDir The destination directory. Must not be null.
     */
    public void setDestinationDir( Object destinationDir ) {
        this.destinationDir = destinationDir;
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

    /**
     * Get the version of the setup. If not set a version of 1.0 is returned
     * @return the version
     */
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

    /**
     * Set the version of the setup.
     * @param version the version
     */
    public void setVersion( String version ) {
        this.version = version;
    }

    /**
     * Get a global description. Never null.
     * @return the description
     */
    public String getDescription() {
        if( description == null ) {
            return "";
        }
        return description;
    }

    /**
     * Set a description for the installer. This is used for the packages but also as default for shortcuts, service, etc.
     * @param description new description
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * Get the short application identifier of the setup file. If not set then the project.archivesBaseName is used.
     * Should not contain spaces if possible.
     * 
     * @return the application identifier
     */
    public String getAppIdentifier() {
        if( appIdentifier != null ) {
            return appIdentifier;
        }
        Object name = project.getProperties().get( "archivesBaseName" );
        if( name != null ) {
            return name.toString();
        }
        return project.getName();
    }

    /**
     * Set the short application identifier for the setup file.
     * @param identifier new base name
     */
    public void setAppIdentifier( String identifier ) {
        this.appIdentifier = identifier;
    }

    /**
     * Get the name of the setup file without extension. If not set then baseName-version is used.
     * 
     * @return the setup file name
     */
    public String getArchiveName() {
        if( archiveName != null ) {
            return archiveName;
        }
        return getAppIdentifier() + '-' + getVersion();
    }

    /**
     * Set the name of the download file for the setup. This should contain the version if needed
     * @param setupName name of the setup
     */
    public void setArchiveName( String setupName ) {
        this.archiveName = setupName;
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

    /**
     * Get the icons for the setup.
     * @return the icons
     */
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

    /**
     * Get the bundle JRE value.
     * 
     * @return the value
     */
    public Object getBundleJre() {
        return bundleJre;
    }

    /**
     * Add a Java VM into your setup. The resulting behavior depends on the platform. This can be a version or a
     * directory to a installed Java VM.
     * 
     * @param bundleJre version or path
     * @see #setBundleJreTarget(String)
     */
    public void setBundleJre( Object bundleJre ) {
        this.bundleJre = bundleJre;
    }

    /**
     * Get the target for a bundle JRE.
     * 
     * @return the target
     */
    public String getBundleJreTarget() {
        if( bundleJreTarget != null ) {
            if( bundleJreTarget.startsWith( "/" ) ) {
                bundleJreTarget = bundleJreTarget.substring( 1 );
            }
            if( bundleJreTarget.endsWith( "/" ) ) {
                bundleJreTarget = bundleJreTarget.substring( 0, bundleJreTarget.length() - 1 );
            }
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

    /**
     * Get the main class.
     * 
     * @return the class name
     */
    public String getMainClass() {
        return mainClass;
    }

    /**
     * Set the main class of the Java application.
     * 
     * @param mainClass the class name
     */
    public void setMainClass( String mainClass ) {
        this.mainClass = mainClass;
    }

    /**
     * Get the main jar file.
     * 
     * @return the main jar
     */
    public String getMainJar() {
        return mainJar;
    }

    /**
     * Set the jar which contains the main class. This jar must contains all references to all other needed jar files in
     * the manifest.
     * 
     * @param mainJar the main jar file
     */
    public void setMainJar( String mainJar ) {
        this.mainJar = mainJar;
    }

    /**
     * A command that run after the installer.
     * @return the command or null
     */
    public DesktopStarter getRunAfter() {
        return runAfter;
    }

    /**
     * Set a command that run after the installer.
     * @param runAfter the command
     */
    public void setRunAfter( String runAfter ) {
        this.runAfter = new DesktopStarter( this );
        this.runAfter.setExecutable( runAfter );
    }

    /**
     * Set a command that run after the installer.
     * @param closue the command
     */
    public void runAfter( Closure<?> closue ) {
        runAfter = ConfigureUtil.configure( closue, new DesktopStarter( this ) );
    }

    /**
     * A command that run before the uninstaller.
     * @return the command or null
     */
    public DesktopStarter getBeforeUninstall() {
        return runBeforeUninstall;
    }

    /**
     * Set a command that run before the uninstaller.
     * @param runAfter the command
     */
    public void setBeforeUninstall( String runAfter ) {
        this.runBeforeUninstall = new DesktopStarter( this );
        this.runBeforeUninstall.setExecutable( runAfter );
    }

    /**
     * Set a command that run run before the uninstaller.
     * @param closue the command
     */
    public void runBeforeUninstall( Closure<?> closue ) {
        runBeforeUninstall = ConfigureUtil.configure( closue, new DesktopStarter( this ) );
    }

    /**
     * Register a file extensions.
     * 
     * @param closue document type
     */
    public void documentType( Closure<?> closue ) {
        DocumentType doc = ConfigureUtil.configure( closue, new DocumentType( this ) );
        if( doc.getFileExtension() == null || doc.getFileExtension().size() == 0 ) {
            throw new GradleException( "documentType must contains minimum one fileExtension." );
        }
        documentTypes.add( doc );
    }

    /**
     * Get the list of document types.
     * @return the list
     */
    public List<DocumentType> getDocumentType() {
        return documentTypes;
    }

    /**
     * Register a service.
     * 
     * @param closue the closure of the service definition
     */
    public void service( Closure closue ) {
        Service service = ConfigureUtil.configure( closue, new Service( this ) );
        services.add( service );
    }

    /**
     * Returns the registered services.
     * 
     * @return the registered services
     */
    public List<Service> getServices() {
        return services;
    }

    /**
     * Register a desktop starter.
     * 
     * @param closue the closure of the desktop starter's definition
     */
    public void desktopStarter( Closure closue ) {
        DesktopStarter service = ConfigureUtil.configure( closue, new DesktopStarter( this ) );
        desktopStarters.add( service );
    }

    /**
     * Returns the registered desktop starters.
     * 
     * @return the registered desktop starters, never null, can be empty.
     */
    public List<DesktopStarter> getDesktopStarters() {
        return desktopStarters;
    }

    /**
     * Get the list pattern for files that should be deleted. 
     * @return the list
     */
    public List<String> getDeleteFiles() {
        return deleteFiles;
    }

    /**
     * Add a file pattern to delete files before install and after uninstall.
     * @param pattern the patter. Can contains * and ? characters
     */
    public void deleteFiles( String pattern ) {
        if( pattern.isEmpty() ) {
            return;
        }
        deleteFiles.add( pattern );
    }

    /**
     * Get the list of folders to delete.
     * @return the list
     */
    public List<String> getDeleteFolders() {
        return deleteFolders;
    }

    /**
     * Add a folder to delete before install and after uninstall.
     * @param folder the folder
     */
    public void deleteFolder( String folder ) {
        this.deleteFolders.add( folder );
    }

    /**
     * If enabled then an empty from definition is failing the build.
     * 
     * @return true, if enabled
     */
    public boolean isFailOnEmptyFrom() {
        return failOnEmptyFrom;
    }

    /**
     * Enable the check.
     * 
     * @param failOnEmptyFrom true, check is enabled
     */
    public void setFailOnEmptyFrom( boolean failOnEmptyFrom ) {
        this.failOnEmptyFrom = failOnEmptyFrom;
    }
}
