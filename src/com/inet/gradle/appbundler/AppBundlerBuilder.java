package com.inet.gradle.appbundler;

import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.AbstractBuilder;
import com.inet.gradle.setup.DesktopStarter;

/**
 * Actual implementation to create the application bundle
 * @author gamma
 */
public class AppBundlerBuilder extends AbstractBuilder<AppBundlerGradleTask,AppBundler> {

	private AppBundler setup;

	/**
	 * Setup the creator
	 * @param task that was called 
	 * @param setup that was used
	 * @param fileResolver that we might need
	 */
	protected AppBundlerBuilder(AppBundlerGradleTask task, AppBundler setup, FileResolver fileResolver) {
		super(task, fileResolver);
		this.setup = setup;
	}

	/**
	 * now build it
	 */
	public void build() {
		AppBundlerApplicationBuilder applicationBuilder = new AppBundlerApplicationBuilder( task, setup, fileResolver );
		try {
			applicationBuilder.buildApplication( new DesktopStarter<AppBundler>(setup) );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
