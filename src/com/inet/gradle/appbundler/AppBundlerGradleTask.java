package com.inet.gradle.appbundler;

import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.util.ConfigureUtil;

import com.inet.gradle.setup.AbstractSetupTask;

import groovy.lang.Closure;

/**
 * Task to create a .app archive  
 * @author gamma
 */
public class AppBundlerGradleTask extends AbstractSetupTask<AppBundler> {

	private OSXCodeSign<AppBundler> codeSign;

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

    
    /**
     * Set the needed information for signing the setup.
     * 
     * @param closue the data for signing
     */
    public void codeSign( Closure<AppBundler> closue ) {
        ProjectInternal project = (ProjectInternal)getProject();
        codeSign = ConfigureUtil.configure( closue, new OSXCodeSign<AppBundler>(this, project.getFileResolver()) );
    }

    /**
     * Get the SignTool configuration if set
     * 
     * @return the settings or null
     */
    public OSXCodeSign<AppBundler> getCodeSign() {
        return codeSign;
    }
}
