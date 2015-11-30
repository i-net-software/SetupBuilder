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

import java.util.HashMap;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;

import com.inet.gradle.setup.deb.Deb;
import com.inet.gradle.setup.dmg.Dmg;
import com.inet.gradle.setup.msi.Msi;
import com.inet.gradle.setup.rpm.Rpm;

/**
 * The central plugin class of this Gradle plugin.
 * 
 * @author Volker Berlin
 */
public class SetupBuilderPlugin implements Plugin<Project> {

    @Override
    public void apply( Project project ) {
        // apply the BasePlugin to make the base features like "clean" available by default.
        HashMap<String, Class<?>> plugin = new HashMap<>();
        plugin.put( "plugin", BasePlugin.class );
        project.apply( plugin );
        //        project.getPluginManager().apply( BasePlugin.class ); // API since Gradle 2.3

        project.getExtensions().create( "setupBuilder", SetupBuilder.class, project );
        project.getTasks().create( "deb", Deb.class );
        project.getTasks().create( "dmg", Dmg.class );
        project.getTasks().create( "msi", Msi.class );
        project.getTasks().create( "rpm", Rpm.class );
    }

}
