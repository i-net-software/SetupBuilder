package com.inet.gradle.setup.dmg;

import java.io.File;

import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.DesktopStarter;
import com.inet.gradle.setup.Service;
import com.inet.gradle.setup.SetupBuilder;

/**
 * Build an OSX Application - service
 * @author gamma
 *
 */
public class OSXApplicationBuilder extends AbstractOSXApplicationBuilder<Dmg, SetupBuilder> {

	/**
	 * Setup this builder.
	 * @param task - original task
	 * @param setup - original setup
	 * @param fileResolver - original fileResolver
	 */
	protected OSXApplicationBuilder(Dmg task, SetupBuilder setup, FileResolver fileResolver) {
		super(task, setup, fileResolver);
	}

	/**
	 * Create Application from service provided. Also create the preference panel
	 * and put it into the application. Will also create the installer wrapper package of this application
	 * @param service the service
	 * @throws Throwable error.
	 */
	void buildService(Service service) throws Throwable {

		// We need the executable. It has a different meaning than on other systems.
		if ( service.getExecutable() == null || service.getExecutable().isEmpty() ) {
			service.setExecutable( service.getId() );
		}
		
		System.err.println("Having executable of: '" + service.getExecutable() +"'" );
		prepareApplication(service);
		finishApplication();
		copyBundleFiles( service );
		new OSXPrefPaneCreator(task, getSetupBuilder(), fileResolver).create(service);

		// codesigning will be done on the final package.
		// codeSignApplication( service );
	}

	/**
	 * Create Application from the desktop starter provided
	 * @param application - the application
	 * @throws Exception on errors
	 */
	void buildApplication(DesktopStarter application) throws Exception {

		// We need the executable. It has a different meaning than on other systems.
		if ( application.getExecutable() == null || application.getExecutable().isEmpty() ) {
			application.setExecutable( getSetupBuilder().getAppIdentifier() );
		}
		
		prepareApplication( application );
		setDocumentTypes( application.getDocumentType() );
		finishApplication();
		copyBundleFiles( application );

		if ( task.getCodeSign() != null ) {
			task.getCodeSign().signApplication( new File(buildDir, application.getDisplayName() + ".app") );
		}
	}
}
