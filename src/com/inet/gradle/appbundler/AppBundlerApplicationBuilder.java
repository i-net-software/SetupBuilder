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

package com.inet.gradle.appbundler;

import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.AbstractSetupBuilder;
import com.inet.gradle.setup.AbstractTask;
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
	protected AppBundlerApplicationBuilder(AppBundlerGradleTask task, AbstractSetupBuilder setup,
			FileResolver fileResolver) {
		super(task, setup, fileResolver);
		buildDir = setup.getDestinationDir();
	}

	@Override
	protected AbstractTask getTask() {
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
			application.setExecutable( getAbstractSetupBuilder().getAppIdentifier() );
		}
		
		prepareApplication( application );
		setDocumentTypes( application.getDocumentType() );
		finishApplication();
		copyBundleFiles( application );
	}	
}
