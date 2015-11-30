package com.inet.gradle.appbundler;

import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.AbstractSetupBuilder;
import com.inet.gradle.setup.AbstractSetupTask;
import com.inet.gradle.setup.DesktopStarter;
import com.inet.gradle.setup.dmg.AbstractOSXApplicationBuilder;

/**
 * Build the OSX app bundle
 * @author gamma
 *
 */
public class AppBundlerApplicationBuilder extends AbstractOSXApplicationBuilder<AppBundlerGradleTask, AppBundler> {

	/**
	 * Build the OSX app bundle
	 * @param task task
	 * @param setup setup
	 * @param fileResolver resolver
	 */
	protected AppBundlerApplicationBuilder(AppBundlerGradleTask task, AbstractSetupBuilder<AppBundler> setup,
			FileResolver fileResolver) {
		super(task, setup, fileResolver);
	}

	@Override
	protected AbstractSetupTask<AppBundler> getTask() {
		return task;
	}

	/**
	 * Create Application
	 * @param application - the application
	 * @throws Exception on errors
	 */
	void buildApplication(DesktopStarter application) throws Exception {

		// We need the executable. It has a different meaning than on other systems.
		if ( application.getExecutable() == null || application.getExecutable().isEmpty() ) {
			application.setExecutable( setup.getAppIdentifier() );
		}
		
		prepareApplication( application );
		setDocumentTypes( application.getDocumentType() );
		finishApplication();
		copyBundleFiles( application );
	}	
}
