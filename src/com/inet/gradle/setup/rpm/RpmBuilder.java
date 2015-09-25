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
package com.inet.gradle.setup.rpm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.AbstractBuilder;
import com.inet.gradle.setup.DesktopStarter;
import com.inet.gradle.setup.Service;
import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.Template;
import com.inet.gradle.setup.image.ImageFactory;
import com.inet.gradle.setup.rpm.RpmControlFileBuilder.Script;

public class RpmBuilder extends AbstractBuilder<Rpm> {
	
	
	private RpmControlFileBuilder  controlBuilder;
    /**
     * Create a new instance
     * 
     * @param rpm the calling task
     * @param setup the shared settings
     * @param fileResolver the file Resolver
     */
    public RpmBuilder( Rpm rpm, SetupBuilder setup, FileResolver fileResolver ) {
        super( rpm, setup, fileResolver );
    }

    /**
     * Build the RedHat package in different steps with the 'rpmbuild'.
     * 
     *  <dl>
     * 		<dt>copy files</dt>
 	 * 			<dd>copy the files specified in the gradle script to the BUILD/usr/share/archivesBaseName directory.</dd>
 	 * 			<dd>The files must be in the BUILD directory because the 'prep' step will copy all the files from there to the BUILDROOT directory.</dd>
 	 * 			<dd>The 'rpmbuild' deletes the BUILDROOT directory before building the package. Thats why we need to copy the files into it. </dd>
 	 * 		<dt>SPEC file creation</dt>
 	 * 			<dd>The 'rpmbuild' requires a configuration files ending with .spec.</dd>
 	 * 			<dd>This spec file contains all required informations (like name, version, dependencies) and scripts that are executed during the creation and installing of the package.</dd>
  	 * 		<dt>change file permissions</dt>
 	 * 			<dd>Before the package is created the permissions of all files need to be set correctly.</dd>
 	 * 			<dd>All directories and executables will be changed to 755 permission and other files to 644.</dd>
  	 * 		<dt>create the package</dt>
 	 * 			<dd>Creates the package with 'rpmbuild'</dd>
  	 * </dl>
     */
    public void build() {
    	try {
    		String release = task.getRelease();
    		if(release == null || release.length() == 0) {
    			release = "1";
    		}
            File filesPath = new File( buildDir.getAbsolutePath() + "/BUILD" + task.getInstallationRoot() + setup.getBaseName());
            task.copyTo( filesPath );
            changeFilePermissionsTo644( filesPath );


            controlBuilder = new RpmControlFileBuilder( super.task, setup, new File( buildDir, "SPECS" ) );

            for( Service service : setup.getServices() ) {
                setupService( service );
            }
            
            for( DesktopStarter starter : setup.getDesktopStarters() ) {
                setupStarter( starter );
            }
            
            controlBuilder.build();

            changeDirectoryPermissionsTo755( buildDir );

            createRpmPackage();


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
    private void setupService( Service service ) throws IOException {
    	String workingDir = null;
    	DesktopStarter starter = setup.getRunAfter();
        if(starter != null ) {
        	workingDir = starter.getWorkDir();
        }	
    	String serviceUnixName = service.getName().toLowerCase().replace( ' ', '-' );
        String mainJarPath;
        
        		
        Template initScript = new Template( "rpm/template/init-service.sh" );
        String installationRoot = task.getInstallationRoot();
        
        if( workingDir != null ) {
			initScript.setPlaceholder( "workdir", installationRoot + setup.getBaseName() + "/" + workingDir );
    		mainJarPath = installationRoot + setup.getBaseName() + "/" + workingDir + "/" + service.getMainJar();
    	} else {	
    		initScript.setPlaceholder( "workdir", installationRoot + setup.getBaseName() );
    		mainJarPath = installationRoot + setup.getBaseName() + "/" + service.getMainJar();
    	}
        
        initScript.setPlaceholder( "name", serviceUnixName );
        initScript.setPlaceholder( "displayName", setup.getApplication() );
        initScript.setPlaceholder( "description", service.getDescription() );
        initScript.setPlaceholder( "wait", "2" );
        initScript.setPlaceholder( "mainJar", mainJarPath );
        
        	
        
        initScript.setPlaceholder( "startArguments",
                                   "-cp "+ mainJarPath + " " + service.getMainClass() + " " + service.getStartArguments() );
        String initScriptFile = "BUILD/etc/init.d/" + serviceUnixName;
        initScript.writeTo( createFile( initScriptFile, true ) );
        controlBuilder.addConfFile( initScriptFile );
        controlBuilder.addScriptFragment( Script.PREINST,  "if [ -f \"/etc/init.d/"+serviceUnixName+"\" ]; then\n  service "+serviceUnixName+ " stop \nfi");
        controlBuilder.addScriptFragment( Script.POSTINST, "if [ -f \"/etc/init.d/"+serviceUnixName+"\" ]; then\n  chkconfig --add "+serviceUnixName+"\nfi" );
        controlBuilder.addScriptFragment( Script.POSTINST, "if [ -f \"/etc/init.d/"+serviceUnixName+"\" ]; then\n  service "+serviceUnixName+ " start >/dev/null\nfi");
        controlBuilder.addScriptFragment( Script.PRERM,    "if [ -f \"/etc/init.d/"+serviceUnixName+"\" ]; then\n  service "+serviceUnixName+ " stop \nfi");
        controlBuilder.addScriptFragment( Script.PRERM,    "if [ -f \"/etc/init.d/"+serviceUnixName+"\" ]; then\n  chkconfig --del "+serviceUnixName+ "\nfi");
    }
    
    /**
     * Changes the permissions of all directories recursively inside the specified path to 755.
     * @param path the path
     * @throws IOException on I/O failures
     */
    // share
    private void changeDirectoryPermissionsTo755( File path ) throws IOException {
     	setPermissions( path, true );
        for( File file : path.listFiles() ) {
            if( file.isDirectory() ) {
                changeDirectoryPermissionsTo755( file );
            }
        }
    }

    /**
     * Changes the permissions of all files recursively inside the specified path to 644.
     * @param path the path
     * @throws IOException on I/O failures
     */
    // share
    private void changeFilePermissionsTo644( File path ) throws IOException {
        for( File file : path.listFiles() ) {
            if( file.isDirectory() ) {
                changeFilePermissionsTo644( file );
            } else {
            	if( file.getName().endsWith(".sh")  ) {
            		setPermissions( file, true );
            	} else {
            		setPermissions( file, false );
            	}
            }
        }
    }
    
    /**
     * Sets the permissions of the specified file, either to 644 (non-executable) or 755 (executable).
     * @param file the file 
     * @param executable if set to <tt>true</tt> the executable bit will be set
     * @throws IOException on errors when setting the permissions
     */
    // share
    static void setPermissions( File file, boolean executable ) throws IOException {
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
     * Creates the files and the corresponding scripts for the specified desktop starter.
     * @param starter the desktop starter
     * @throws IOException on errors during creating or writing a file
     */
    // share
    private void setupStarter( DesktopStarter starter ) throws IOException {
        String unixName = starter.getName().toLowerCase().replace( ' ', '-' );
        String consoleStarterPath = "/usr/bin/" + unixName;
        try (FileWriter fw = new FileWriter( createFile( "BUILD" + consoleStarterPath, true ) )) {
            fw.write( "#!/bin/bash\n" );
            fw.write( "java -cp " + task.getInstallationRoot() + setup.getBaseName() + "/" + starter.getMainJar() + " " + starter.getMainClass() + " "
                + starter.getStartArguments() + " \"$@\"" );
        }
        int[] iconSizes = { 16, 32, 48, 64, 128 };

        for( int size : iconSizes ) {
            File iconDir = new File( buildDir, "BUILD/usr/share/icons/hicolor/" + size + "x" + size + "/apps/" );
            iconDir.mkdirs();
            File scaledFile = ImageFactory.getImageFile( task.getProject(), setup.getIcons(), iconDir, "png" + size );
            if( scaledFile != null ) {
                File iconFile = new File( iconDir, unixName + ".png" );
                scaledFile.renameTo( iconFile );
                setPermissions( iconFile, false );
            }
        }
        try (FileWriter fw = new FileWriter( createFile( "BUILD/usr/share/applications/" + unixName + ".desktop", false ) )) {
            fw.write( "[Desktop Entry]\n" );
            fw.write( "Name=" + starter.getName() + "\n" );
            fw.write( "Comment=" + starter.getDescription().replace( '\n', ' ' ) + "\n" );
            fw.write( "Exec=" + consoleStarterPath + " %F\n" );
            fw.write( "Icon=" + unixName + "\n" );
            fw.write( "Terminal=false\n" );
            fw.write( "StartupNotify=true\n" );
            fw.write( "Type=Application\n" );
            if( starter.getMimeTypes() != null ) {
                fw.write( "MimeType=" + starter.getMimeTypes() + "\n" );
            }
            if( starter.getCategories() != null ) {
                fw.write( "Categories=" + starter.getCategories() + "\n" );
            }
        }
        
    }
    
    /**
     * Creates a file in the build path structure.
     * @param path the path relative to the root of the build path
     * @param executable if set to <tt>true</tt> the executable bit will be set in the permission flags
     * @return the created file
     * @throws IOException on errors during creating the file or setting the permissions
     */
    // share
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
     * execute the command to generate the RPM package
     * 
     * rpmbuild -ba -clean "--define=_topdir buildDir(rpm)" SPECS/basename.spec
     * 
     */
    private void createRpmPackage() {
    	
    	ArrayList<String> command = new ArrayList<>();
        command.add( "rpmbuild" );
        command.add( "-ba" );
        command.add( "-v" );
        command.add( "--clean" );
        command.add( "--define=_topdir " + buildDir.getAbsolutePath() );
        command.add( "SPECS/" + setup.getBaseName() + ".spec" );
        exec( command );
    }


}
