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
package com.inet.gradle.setup.deb;

import java.io.File;

import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.AbstractBuilder;
import com.inet.gradle.setup.SetupBuilder;

public class DebBuilder extends AbstractBuilder<Deb> {

    /**
     * Create a new instance
     * 
     * @param deb the calling task
     * @param setup the shared settings
     * @param fileResolver the file Resolver
     */
    public DebBuilder( Deb deb, SetupBuilder setup, FileResolver fileResolver ) {
        super( deb, setup, fileResolver );
    }

    /**
     * executes all necessary steps from copying to building the Debian package
     */
    public void build() {
    	task.copyTo( new File( buildDir, "/usr/share/" + setup.getBaseName() ) );
    	// create the package config files in the DEBIAN subfolder
    	try {
    		new DebConfigFileBuilder(super.task, setup, new File(buildDir.getAbsolutePath() + File.separatorChar + "DEBIAN")).build();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }

}
