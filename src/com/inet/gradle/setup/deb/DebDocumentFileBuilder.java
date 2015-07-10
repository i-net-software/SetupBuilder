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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import com.inet.gradle.setup.SetupBuilder;

/**
 * Builder for the documentation files like changelog that are required for the Debian package tool.
 * 
 * 
 * @author Stefan Heidrich
 */
class DebDocumentFileBuilder {

    private static final String NEWLINE = "\n";

	private final Deb          deb;

    private final SetupBuilder setup;

    private File               buildDir;

    
    /**
     * the constructor setting the fields
     * @param deb the task for the debian package
     * @param setup the generic task for all setups
     * @param buildDir the directory to build the package in
     */
    DebDocumentFileBuilder( Deb deb, SetupBuilder setup, File buildDir ) {
        this.deb = deb;
        this.setup = setup;
        this.buildDir = buildDir;
    }

    /**
     * Create the configuration files for the Debian package based on the settings in the task.
     * 
     * @throws Exception
     */
    void build() throws Exception {
    	
    	createChangelogFile();
    	copyCopyrightFile();

    }

    private void copyCopyrightFile() throws FileNotFoundException, IOException {
    	File copyright = new File(buildDir, "copyright");
		
		if(!copyright.getParentFile().exists()) {
			copyright.getParentFile().mkdirs();
		}
		
		Files.copy(setup.getLicenseFile().toPath(), new FileOutputStream(copyright) );
//		changePermission(copyright);
		DebUtils.setPermissions(copyright, false);
	}

	private void createChangelogFile() throws IOException {
    	FileOutputStream fileoutput = null;
		OutputStreamWriter controlWriter = null;

		try {
			File changelog = new File(buildDir, "changelog.gz");
			
			if(!changelog.getParentFile().exists()) {
				changelog.getParentFile().mkdirs();
			}
			
			fileoutput = new FileOutputStream(changelog);
			GZIPOutputStream gzipstream = new GZIPOutputStream(fileoutput);
			
			controlWriter = new OutputStreamWriter(gzipstream, "UTF-8");
		 
			controlWriter.write( deb.getPackages() + " (" + setup.getVersion() + ") unstable; urgency=low" + NEWLINE);
			
			String changes = deb.getChanges();
			if(changes != null && changes.length() > 0) {
				controlWriter.write( changes + NEWLINE);
			} else {
				controlWriter.write( "  * no changes" + NEWLINE);
			}
			
			controlWriter.write( "  -- " + setup.getVendor() + " <" + deb.getMaintainerEmail() + ">  " + NEWLINE);
			
			controlWriter.flush();
			
			DebUtils.setPermissions(changelog, false);
			
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
