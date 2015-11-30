package com.inet.gradle.appbundler;

import org.gradle.api.internal.project.ProjectInternal;

import com.inet.gradle.setup.AbstractSetupTask;

/**
 * Task to create a .app archive  
 * @author gamma
 */
public class AppBundlerGradleTask extends AbstractSetupTask<AppBundler> {

	/**
	 * Construct static as .app
	 */
	public AppBundlerGradleTask() {
		super( "app" );
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void build() {
        ProjectInternal project = (ProjectInternal)getProject();
        new AppBundlerBuilder( this, getSetupBuilder(), project.getFileResolver() ).build();
    }
}
