package com.inet.gradle.setup.dmg;

import java.io.File;

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
            System.out.println( "Not codesigning the Servce: not configured" );
        }
    }

    /**
     * Create Application from the desktop starter provided
     *
     * @param application - the application
     * @throws Exception on errors
     */
    void buildApplication( DesktopStarter application ) throws Exception {

        // We need the executable. It has a different meaning than on other systems.
        if( application.getExecutable() == null || application.getExecutable().isEmpty() ) {
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
