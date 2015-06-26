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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

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
    DebConfigFileBuilder( Deb deb, SetupBuilder setup, File buildDir ) {
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
    	
    	createControlFile();

    }

    /**
     * 
     * @throws IOException if 
     */
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
			
			putPackage(controlWriter);
			putVersion(controlWriter);
			putSection(controlWriter);
			putPriority(controlWriter);
			putArchitecture(controlWriter);
			putInstallSize(controlWriter);
			putRecommends(controlWriter);
			putDepends(controlWriter);
			putMaintainer(controlWriter);
			putDescription(controlWriter);

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

	/**
	 * Write to description to the file. If no
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putDescription(OutputStreamWriter controlWriter)
			throws IOException {
		controlWriter.write("Description: " + deb.getDescription() + NEWLINE);
	}

	/**
	 * Write to maintainer to the file
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putMaintainer(OutputStreamWriter controlWriter)
			throws IOException {
		controlWriter.write("Maintainer: " + setup.getVendor() + NEWLINE);
	}
	/**
	 * Write to dependencies to the file
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putDepends(OutputStreamWriter controlWriter)
			throws IOException {
		controlWriter.write("Depends: " + deb.getDepends() + NEWLINE);
	}
	/**
	 * Write to recommends to the file
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putRecommends(OutputStreamWriter controlWriter)
			throws IOException {
		controlWriter.write("Recommends: " + deb.getRecommends() + NEWLINE);
	}

	/**
	 * Write to installation size to the file
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putInstallSize(OutputStreamWriter controlWriter)
			throws IOException {
		controlWriter.write("Installed-Size: " + deb.getInstallSize() + NEWLINE);
	}

	/**
	 * Write to architecture to the file
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putArchitecture(OutputStreamWriter controlWriter)
			throws IOException {
		controlWriter.write("Architecture: " + deb.getArchitecture() + NEWLINE);
	}

	/**
	 * Write to priority to the file
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putPriority(OutputStreamWriter controlWriter)
			throws IOException {
		controlWriter.write("Priority: " + deb.getPriority() + NEWLINE);
	}

	/**
	 * Write to section to the file
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putSection(OutputStreamWriter controlWriter)
			throws IOException {
		controlWriter.write("Section: " + deb.getSection() + NEWLINE);
	}

	/**
	 * Write to version to the file
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putVersion(OutputStreamWriter controlWriter)
			throws IOException {
		controlWriter.write("Version: " + setup.getVersion() + NEWLINE);
	}

	/**
	 * Write to package to the file
	 * @param controlWriter the writer for the file
	 * @throws IOException if the was an error while writing to the file
	 */
	private void putPackage(OutputStreamWriter controlWriter)
			throws IOException {
		controlWriter.write("Package: " + deb.getPackages() + NEWLINE);
	}

}
