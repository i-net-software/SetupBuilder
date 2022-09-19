package com.inet.gradle.setup.dmg;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.codehaus.groovy.control.ConfigurationException;
import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.abstracts.DesktopStarter;
import com.inet.gradle.setup.abstracts.Service;
import com.oracle.appbundler.PlistEntry;

/**
 * Build an OSX Application - service
 *
 * @author gamma
 *
 */
public class OSXApplicationBuilder extends AbstractOSXApplicationBuilder<Dmg, SetupBuilder> {

    private Service service;

    private OSXPrefPaneCreator prefPaneCreator;

    /**
     * Setup this builder.
     *
     * @param task - original task
     * @param setup - original setup
     * @param fileResolver - original fileResolver
     */
    protected OSXApplicationBuilder( Dmg task, SetupBuilder setup, FileResolver fileResolver ) {
        super( task, setup, fileResolver );
    }

    /**
     * Create sub tasks for a service,this must be called in project.afterEvaluate().
     * 
     * @param service the service
     */
    void configSubTasks( Service service ) {
        this.service = service;
        this.prefPaneCreator = new OSXPrefPaneCreator( task, getSetupBuilder(), fileResolver, service );
    }

    /**
     * Get the service for which this builder was created or null if it only an application.
     * 
     * @return the service
     */
    Service getService() {
        return service;
    }

    /**
     * Create Application from service provided. Also create the preference panel
     * and put it into the application. Will also create the installer wrapper package of this application
     *
     * @throws Throwable error.
     */
    void buildService() throws Throwable {

        // We need the executable. It has a different meaning than on other systems.
        if( service.getExecutable() == null || service.getExecutable().isEmpty() ) {
            service.setExecutable( service.getId() );
        }

        System.err.println( "Having executable of: '" + service.getExecutable() + "'" );
        prepareApplication( service, false );
        finishApplication();
        copyBundleFiles( service );
        prefPaneCreator.create();

        // codesigning will be done on the final package.
        if( task.getCodeSign() != null ) {
            task.getCodeSign().signApplication( new File( buildDir, service.getDisplayName() + ".app" ) );
        } else {
            task.getProject().getLogger().info( "Not codesigning the Servce: not configured" );
        }
    }

    /**
     * Create Application from the desktop starter provided
     *
     * @param application - the application
     * @throws Exception on errors
     */
    void buildApplication( DesktopStarter application ) throws Exception {

        String executable = application.getExecutable();
        if ( executable != null && executable.endsWith( ".app" ) ) {
            // This is already a finished app. We do not have to package it again.
            File executableFile = new File( executable );
            if ( !executableFile.exists() ) {
                throw new ConfigurationException( "An executable file '" + executable + "' was defined but did not exist." );
            }

            String executableName = executableFile.getName();
            File destinationFile = new File( buildDir, executableName );
            Files.move( executableFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING );
            
            setApplicationFilePermissions( destinationFile );
            
            if( task.getCodeSign() != null ) {
                task.getCodeSign().signApplication( destinationFile );
            }
            
            return;
        }
        
        // We need the executable. It has a different meaning than on other systems.
        if( executable == null || executable.isEmpty() ) {
            application.setExecutable( getSetupBuilder().getAppIdentifier() );
        }

        prepareApplication( application, false );
        setDocumentTypes( application.getDocumentType() );

        for( String scheme: application.getSchemes() ) {
            addScheme( scheme );
        }

        PlistEntry requiresAquaSystemAppearance = new PlistEntry();
        requiresAquaSystemAppearance.setKey( "NSRequiresAquaSystemAppearance" );
        requiresAquaSystemAppearance.setValue( Boolean.valueOf( task.isAquaSystemAppearanceRequired() ).toString() );
        getAppBundler().addConfiguredPlistEntry( requiresAquaSystemAppearance );

        finishApplication();
        copyBundleFiles( application );

        if( task.getCodeSign() != null ) {
            task.getCodeSign().signApplication( new File( buildDir, application.getDisplayName() + ".app" ) );
        }
    }
}
