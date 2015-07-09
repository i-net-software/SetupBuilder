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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.AbstractBuilder;
import com.inet.gradle.setup.Service;
import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.Template;

public class DebBuilder extends AbstractBuilder<Deb> {

    private DebControlFileBuilder controlBuilder;
    private DebDocumentFileBuilder documentBuilder;

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
    	try {
    		task.copyTo( new File( buildDir, "/usr/share/" + setup.getBaseName() ) );
    		// 	create the package config files in the DEBIAN subfolder
    	
    		controlBuilder = new DebControlFileBuilder(super.task, setup, new File(buildDir, "DEBIAN"));
    		
    		for(Service service: setup.getServices()) {
                setupService( service );
    		}
    		
            controlBuilder.build();

    		documentBuilder = new DebDocumentFileBuilder(super.task, setup, new File(buildDir, "/usr/share/doc/" + setup.getBaseName()));
    		documentBuilder.build();

            
    		createDebianPackage();
    		
    		checkDebianPackage();
    		
    	} catch( RuntimeException ex ) {
            throw ex;
        } catch( Exception ex ) {
            throw new RuntimeException( ex );
        }
    }

    /**
     * Creates the files and the corresponding script section for the specified service.
     * @param service the service
     * @throws IOException on errors during creating or writing a file
     */
    public void setupService( Service service ) throws IOException {
        String lowerCaseName = service.getName().toLowerCase();
        Template initScript = new Template( "deb/template/init-service.sh" );
        initScript.setPlaceholder( "test", "test" );
        String initScriptFile = "etc/init.d/" + lowerCaseName;
        initScript.writeTo( createFile( initScriptFile, true ) );
        controlBuilder.addConfFile( initScriptFile );
    }
    
    /**
     * Creates a file in the build path structure.
     * @param path the path relative to the root of the build path
     * @param executable if set to <tt>true</tt> the executable bit will be set in the permission flags
     * @return the created file
     * @throws IOException on errors during creating the file or setting the permissions
     */
    private File createFile( String path, boolean executable ) throws IOException {
        File file = new File( buildDir, path );
        if( !file.getParentFile().exists() ) {
            file.getParentFile().mkdirs();
        }
        file.createNewFile();
        
        setPermissions( file, executable );
        return file;
    }
    
    /**
     * Sets the permissions of the specified file, either to 644 (non-executable) or 755 (executable).
     * @param file the file 
     * @param executable if set to <tt>true</tt> the executable bit will be set
     * @throws IOException on errors when setting the permissions
     */
    private void setPermissions( File file, boolean executable ) throws IOException {
        Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
        perms.add( PosixFilePermission.OWNER_READ );
        perms.add( PosixFilePermission.OWNER_WRITE );
        perms.add( PosixFilePermission.GROUP_READ );
        perms.add( PosixFilePermission.OTHERS_READ );
        if( executable ) {
            perms.add( PosixFilePermission.OWNER_EXECUTE );
            perms.add( PosixFilePermission.GROUP_EXECUTE );
            perms.add( PosixFilePermission.OTHERS_EXECUTE );
        }
        Files.setPosixFilePermissions( file.toPath(), perms );
    }

	/**
     * execute the lintian tool to check the Debian package
     * This will only be executed if the task 'checkPackage' property is set to true
     */
    private void checkDebianPackage() {
    	if(task.getCheckPackage() == null || task.getCheckPackage().equalsIgnoreCase("true")) {
    		ArrayList<String> command = new ArrayList<>();
    		command.add( "lintian" );
//    		command.add( "-d" );
    		command.add( setup.getDestinationDir().getAbsolutePath() + "/" +  setup.getSetupName() + "." + task.getExtension() );
    		exec( command );
    	}
	}

	/**
     * execute the command to generate the Debian package
     */
    private void createDebianPackage() {
        ArrayList<String> command = new ArrayList<>();
        command.add( "fakeroot" );
        command.add( "dpkg-deb" );
        command.add( "--build" );
        command.add( buildDir.getAbsolutePath() );
        command.add( setup.getDestinationDir().getAbsolutePath() + "/" +  setup.getSetupName() + "." + task.getExtension() );
        exec( command );
    }
    
}
