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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.ParserConfigurationException;

import com.inet.gradle.setup.SetupBuilder;

/**
 * Builder for the control, postinst and prerm files, that are required for the Debian package tool.
 * <dl>
 * 		<dt>control</dt>
 * 			<dd>contains settings for the package like dependencies, architecture, description</dd>
 * 		<dt>postinst</dt>
 * 			<dd>contains scripts and commands that are executed after the files are copied</dd>
 * 		<dt>prerm</dt>
 * 			<dd>contains scripts and commands that are executed before the files are removed</dd>
 * </dl>
 * 
 * @author Stefan Heidrich
 */
class DebConfigFileBuilder {

    private final Deb          deb;

    private final SetupBuilder setup;

    private File               buildDir;


    DebConfigFileBuilder( Deb deb, SetupBuilder setup, File buildDir ) {
        this.deb = deb;
        this.setup = setup;
        this.buildDir = buildDir;
    }

    /**
     * Create *.wxs file based on the settings in the task.
     * 
     * @throws ParserConfigurationException
     * @throws Exception
     */
    void build() throws Exception {
    	
    	createControlFile();
    	
                // Product node
//        Element product = getOrCreateChildById( wix, "Product", "*", false );
//        addAttributeIfNotExists( product, "Language", "1033" );
//        addAttributeIfNotExists( product, "Manufacturer", setup.getVendor() );
//        addAttributeIfNotExists( product, "Name", setup.getApplication() );
//        addAttributeIfNotExists( product, "Version", setup.getVersion() );
//        addAttributeIfNotExists( product, "UpgradeCode", UUID.randomUUID().toString() );

    }

	private void createControlFile() throws IOException {
		if(buildDir.isDirectory()) {
			throw new IllegalArgumentException("The buildDir parameter must be a directory!");
		}
		if(!buildDir.exists()) {
			buildDir.mkdirs();
		}
		
		FileOutputStream fileoutput = null;
		OutputStreamWriter controlWriter = null;
		
		try {
			File control = new File(buildDir.getAbsolutePath() + File.separatorChar + "control");
			fileoutput = new FileOutputStream(control);
			controlWriter = new OutputStreamWriter(fileoutput, "UTF-8");
			
			
			
			controlWriter.write("Package: " + deb.getPackages()); // Package muss noch implementiert werden
			controlWriter.write("Version: " + setup.getVersion());
			controlWriter.write("Section: " + deb.getSection());
			controlWriter.write("Priority: " + deb.getPriority());
			controlWriter.write("Architecture: " + deb.getArchitecture());
			controlWriter.write("Installed-Size: " + deb.getInstallSize());
			controlWriter.write("Recommends: " + deb.getRecommends());
			controlWriter.write("Depends: " + deb.getDepends());
			controlWriter.write("Maintainer: " + setup.getVendor());
			controlWriter.write("Description: " + deb.getDescription());

			controlWriter.flush();
			
		} finally {
			if(controlWriter != null) {
				try {
					controlWriter.close();
				} catch (IOException e) {
					// IGNORE
				}
			}
			if(fileoutput != null) {
				try {
					fileoutput.close();
				} catch (IOException e) {
					// IGNORE
				}
			}
		}
		
		
	}

}
