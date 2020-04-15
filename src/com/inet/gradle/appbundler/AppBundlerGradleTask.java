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

import java.io.File;

import org.gradle.api.GradleException;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.ConfigureUtil;

import com.inet.gradle.setup.abstracts.AbstractTask;

import groovy.lang.Closure;

/**
 * Task to create a .app archive  
 * @author gamma
 */
public class AppBundlerGradleTask extends AbstractTask {

    private OSXCodeSign<AppBundlerGradleTask,AppBundler> codeSign;

    /**
     * Construct static as .app
     */
    public AppBundlerGradleTask() {
        super( "app", AppBundler.class );
    }

    /**
     * Get the app builder
     * @return the app builder
     */
    public AppBundler getAppBuilder() {
        return (AppBundler)super.getAbstractSetupBuilder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void build() {
        ProjectInternal project = (ProjectInternal)getProject();
        new AppBundlerBuilder( this, getAppBuilder(), project.getFileResolver() ).build();
    }

    /**
     * The action called from Gradle
     * Will not add an artifact since it would be a directory. 
     */
    @TaskAction
    public void action() {
        build();
        File setupFile = getSetupFile();
        if( !setupFile.exists() ) {
            throw new GradleException( "Setup file was not created: " + setupFile );
        }
    }    

    /**
     * Set the needed information for signing the setup.
     * 
     * @param closue the data for signing
     */
    public void codeSign( Closure<AppBundler> closure ) {
        ProjectInternal project = (ProjectInternal)getProject();
        codeSign = ConfigureUtil.configure( closure, new OSXCodeSign<AppBundlerGradleTask,AppBundler>(this, project.getFileResolver()) );
    }

    /**
     * Get the SignTool configuration if set
     * 
     * @return the settings or null
     */
    public OSXCodeSign<AppBundlerGradleTask,AppBundler> getCodeSign() {
        return codeSign;
    }
}
