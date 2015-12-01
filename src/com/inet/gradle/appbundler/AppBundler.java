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

import org.gradle.api.Project;

import com.inet.gradle.setup.AbstractSetupBuilder;
import com.inet.gradle.setup.SetupSources;

/**
 * The Gradle extension for appBundler task.
 * 
 * @author Gerry Weißbach
 */
public class AppBundler extends AbstractSetupBuilder<AppBundler> implements SetupSources {

	/**
     * Create a new instance.
     * @param project current project
     */
    public AppBundler( Project project ) {
    	super(project);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getArchiveName() {
    	return getApplication();
    }
}
