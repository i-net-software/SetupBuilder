package com.inet.gradle.appbundler;

import java.io.File;

import org.gradle.api.internal.project.ProjectInternal;

import com.inet.gradle.setup.AbstractSetupTask;

public class AppBundlerGradleTask extends AbstractSetupTask {

	public AppBundlerGradleTask() {
		super( "app" );
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void build() {
        ProjectInternal project = (ProjectInternal)getProject();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void copyTo( File target ) {
        super.copyTo( target );
    }
}
