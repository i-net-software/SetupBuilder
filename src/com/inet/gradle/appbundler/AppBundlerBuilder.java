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

import com.inet.gradle.setup.abstracts.AbstractBuilder;
import com.inet.gradle.setup.abstracts.DesktopStarter;

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
            applicationBuilder.buildApplication( new DesktopStarter(setup) );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
