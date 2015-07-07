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

    private String                 baseName;

    private String                 setupName;

    private Object                 licenseFile;

    private Object                 icons;

    private Object                 bundleJre;

    private String                 bundleJreTarget;

    private String                 mainClass;

    private String                 mainJar;

    private List<DocumentType>     documentTypes = new ArrayList<>();
    private List<Service>          services      = new ArrayList<>();

    public SetupBuilder( Project project ) {
        this.project = project;
        this.rootSpec = (CopySpecInternal)project.copySpec( null );

        //init default location
        setDestinationDir( new File( "distributions" ) );
    }

    @Override
    public CopySpecInternal getCopySpec() {
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
     * @param bundleJre
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
     * Set the jar which contains the main class.
     * 
     * @param mainJar the main jar file
     */
    public void setMainJar( String mainJar ) {
        this.mainJar = mainJar;
    }

    /**
     * Register a file extensions.
     * @param closue document type
     */
    public void documentType( Closure closue ) {
        DocumentType doc = ConfigureUtil.configure( closue, new DocumentType( this ) );
        if( doc.getFileExtension() == null || doc.getFileExtension().size() == 0 ) {
            throw new GradleException( "documentType must contains minimum one fileExtension." );
        }
        documentTypes.add( doc );
    }

    public List<DocumentType> getDocumentType() {
        return documentTypes;
    }
    
    /**
     * Register a service.
     * @param closue the closure of the service definition
     */
    public void service( Closure closue ) {
        Service service = ConfigureUtil.configure( closue, new Service() );
        services.add( service );
    }
    
    /**
     * Returns the registered services.
     * @return the registered services
     */
    public List<Service> getServices() {
        return services;
    }
}
