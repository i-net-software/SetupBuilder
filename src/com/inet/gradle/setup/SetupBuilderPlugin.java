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
package com.inet.gradle.setup;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import com.inet.gradle.setup.msi.Msi;

/**
 * The central plugin class of this Gradle plugin.
 * 
 * @author Volker Berlin
 */
public class SetupBuilderPlugin implements Plugin<Project> {

    @Override
    public void apply( Project project ) {
        project.getTasks().create( "msi", Msi.class );
        project.getExtensions().create( "setupBuilder", SetupBuilder.class, project );

        //        project.afterEvaluate( arg0 );
    }

}
