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

import com.inet.gradle.setup.abstracts.AbstractSetupBuilder;
import com.inet.gradle.setup.abstracts.SetupSources;

/**
 * The Gradle extension for appBundler task.
 * 
 * @author Gerry Weißbach
 */
public class AppBundler extends AbstractSetupBuilder implements SetupSources {

    private String jnlpLauncherName;    

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

    /**
     * Get the JNLP Launcher Name
     * This can be used instead of the mainJar
     * @return the launcher file
     */
    public String getJnlpLauncherName() {
        return jnlpLauncherName;
    }

    /**
     * Set the JNLP Launcher file
     * This can be used instead of the mainJar
     * @param jnlpLauncher the launcher file
     */
    public void setJnlpLauncherName(String jnlpLauncher) {
        this.jnlpLauncherName = jnlpLauncher;
    }
}
