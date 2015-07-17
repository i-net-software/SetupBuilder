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
package com.inet.gradle.setup.dmg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;

import org.apache.tools.ant.types.FileSet;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.AbstractBuilder;
import com.inet.gradle.setup.DocumentType;
import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.image.ImageFactory;
import com.oracle.appbundler.AppBundlerTask;
import com.oracle.appbundler.Architecture;
import com.oracle.appbundler.BundleDocument;

/**
 * Build a DMG image for OSX.
 * 
 * @author Volker Berlin
 */
public class DmgBuilder extends AbstractBuilder<Dmg> {

    private String title, applicationName;
	private Path tmp;
	private File iconFile;

    /**
     * Create a new instance
     * 
     * @param dmg the calling task
     * @param setup the shared settings
     * @param fileResolver the file Resolver
     */
    public DmgBuilder( Dmg dmg, SetupBuilder setup, FileResolver fileResolver ) {
        super( dmg, setup, fileResolver );
    }

    /**
     * Build the dmg file. 
     */
    public void build() {

        try {
        	Project project = task.getProject();

        	tmp = Files.createTempDirectory("SetupBuilder", new FileAttribute[0]);
            title = setup.getSetupName();
            applicationName = setup.getBaseName();
            
            AppBundlerTask appBundler = new AppBundlerTask();
            appBundler.setOutputDirectory( buildDir );
            appBundler.setName( applicationName );
            appBundler.setDisplayName( setup.getApplication() );

            String version = setup.getVersion();
            appBundler.setVersion( version );
            int idx = version.indexOf( '.' );
            if( idx >= 0 ) {
                idx = version.indexOf( '.', idx + 1 );
                if( idx >= 0 ) {
                    version = version.substring( 0, idx );
                }
            }
            appBundler.setShortVersion( version );

            appBundler.setExecutableName( setup.getBaseName() );
            appBundler.setIdentifier( setup.getMainClass() );
            appBundler.setMainClassName( setup.getMainClass() );
            appBundler.setJarLauncherName( setup.getMainJar() );
            appBundler.setCopyright( setup.getVendor() );

            Object iconData = setup.getIcons();
            iconFile = ImageFactory.getImageFile( project, iconData, buildDir, "icns" );
            appBundler.setIcon( iconFile );
            Architecture x86_64 = new Architecture();
            x86_64.setName( "x86_64" );
            appBundler.addConfiguredArch( x86_64 );

            // add file extensions
            for( DocumentType doc : setup.getDocumentType() ) {
                BundleDocument bundle = new BundleDocument();
                bundle.setExtensions( String.join( ",", doc.getFileExtension() ) );
                bundle.setName( doc.getName() );
                bundle.setRole( doc.getRole() ); //Viewer or Editor
                Object icons = doc.getIcons();
                if( icons == null ) {
                    // nothing
                } else if( icons == iconData ) {
                    bundle.setIcon( iconFile.toString() );
                } else {
                    bundle.setIcon( ImageFactory.getImageFile( project, icons, buildDir, "icns" ).toString() );
                }
                appBundler.addConfiguredBundleDocument( bundle );
            }

            bundleJre( appBundler );

            org.apache.tools.ant.Project antProject = new org.apache.tools.ant.Project();
            appBundler.setProject( antProject );
            appBundler.execute();

            task.copyTo( new File( buildDir, applicationName + ".app/Contents/Java" ) );
            
            createBinary();
            
            // Remove temporary Path 
            Files.delete(tmp);
            tmp = null;
        
        } catch( RuntimeException ex ) {
            ex.printStackTrace();
            throw ex;
        } catch( Exception ex ) {
            throw new RuntimeException( ex );
        }
    }

    /**
     * Bundle the Java VM if set.
     * @param appBundler the ANT Task
     */
    private void bundleJre( AppBundlerTask appBundler ) {
        Object jre = setup.getBundleJre();
        if( jre == null ) {
            return;
        }
        File jreDir;
        try {
            jreDir = task.getProject().file( jre );
        } catch( Exception e ) {
            jreDir = null;
        }
        if( jreDir == null || !jreDir.isDirectory() ) {
            ArrayList<String> command = new ArrayList<>();
            command.add( "/usr/libexec/java_home" );
            command.add( "-v" );
            command.add( jre.toString() );
            command.add( "-F" );
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            exec( command, null, baos );
            jreDir = new File( baos.toString().trim() );
            if( !jreDir.isDirectory() ) {
                throw new GradleException( "bundleJre version " + jre + " can not be found in: " + jreDir );
            }
        }
        task.getProject().getLogger().lifecycle( "\tbundle JRE: " + jreDir );
        FileSet fileSet = new FileSet();
        fileSet.setDir( jreDir );
        appBundler.addConfiguredRuntime( fileSet );
    }

    /**
     * Create the binary with native tools.
     * 
     * @throws IOException
     */
    private void createBinary() throws IOException {
    	
        createTempImage();
        attach();

        setVolumeIcon();
        applescript();

        detach();
        finalImage();
        
        // unflatten();

        new File( setup.getDestinationDir(), "pack.temp.dmg" ).delete();
    }

    /**
     * Call hdiutil to create a temporary image.
     */
    private void createTempImage() {
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/hdiutil" );
        command.add( "create" );
        command.add( "-srcfolder" );
        command.add( buildDir.toString() + "/" + applicationName + ".app" );
        command.add( "-format" );
        command.add( "UDRW" );
        command.add( "-volname" );
        command.add( title );
        command.add( setup.getDestinationDir() + "/pack.temp.dmg" );
        exec( command );
    }

    /**
     * Call hdiutil to mount temporary image
     * 
     * @throws UnsupportedEncodingException
     */
    private void attach() throws IOException {
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/hdiutil" );
        command.add( "attach" );
        command.add( "-readwrite" );
        command.add( "-noverify" );
        command.add( "-noautoopen" );
        command.add( setup.getDestinationDir() + "/pack.temp.dmg" );
        command.add( "-mountroot" );
        command.add( tmp.toString() );

        exec( command );
    }

    /**
     * Call hdiutil to detach temporary image
     */
    private void detach() {
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/hdiutil" );
        command.add( "detach" );
        command.add( tmp.toString() + "/" + title );
        exec( command );
    }

    /**
     * Call SetFile to set the volume icon.
     * @throws IOException IOException
     */
    private void setVolumeIcon() throws IOException {
        
    	// Copy Icon as file icon into attached container 
        File iconDestination = new File( tmp.toFile() , "/" + title + "/.VolumeIcon.icns" );
		Files.copy( iconFile.toPath(), iconDestination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
    	
    	ArrayList<String> command = new ArrayList<>();
        command.add( "SetFile" );
        command.add( "-a" );
        command.add( "C" );
        command.add( iconDestination.getParent() );
        exec( command );
        
        if ( task.getBackgroundImage() != null ) {
        	String name = task.getBackgroundImage().getName();
            File backgroundDestination = new File( tmp.toFile() , "/" + title + "/.resources/background" + name.substring(name.lastIndexOf('.')) );
            Files.createDirectories(backgroundDestination.getParentFile().toPath(), new FileAttribute[0]);
        	Files.copy(task.getBackgroundImage().toPath(), backgroundDestination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
        }
    }

    private void applescript() throws IOException {
        InputStream input = getClass().getResourceAsStream( "applescript.txt" );
        byte[] bytes = new byte[input.available()];
        input.read( bytes );
        input.close();
        String script = new String( bytes, StandardCharsets.UTF_8 );
        script = script.replace( "${title}", title );
        script = script.replace( "${executable}", applicationName );
        script = script.replace( "${windowWidth}", task.getWindowWidth().toString() );
        script = script.replace( "${windowHeight}", task.getWindowHeight().toString() );
        script = script.replace( "${iconSize}", task.getIconSize().toString() );
        script = script.replace( "${fontSize}", task.getFontSize().toString() );
        
        if ( task.getBackgroundImage() != null ) {
        	String name = task.getBackgroundImage().getName();
			script = script.replace( "${backgroundExt}", name.substring(name.lastIndexOf('.'))  );
        }

        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/osascript" );
        exec( command, new ByteArrayInputStream( script.getBytes( StandardCharsets.UTF_8 ) ), null );
    }

    /**
     * convert to final image
     */
    private void finalImage() {
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/hdiutil" );
        command.add( "convert" );
        command.add( setup.getDestinationDir() + "/pack.temp.dmg" );
        command.add( "-format" );
        command.add( "UDZO" );
        command.add( "-imagekey" );
        command.add( "zlib-level=9" );
        command.add( "-o" );
        command.add( task.getSetupFile().toString() );
        exec( command );
    }

    /**
     * unflatten for SLA
     */
    private void unflatten() {
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/hdiutil" );
        command.add( "unflatten" );
        command.add( task.getSetupFile().toString() );
        exec( command );
    }

    /**
     * re-flatten for SLA
     */
    private void flatten() {
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/hdiutil" );
        command.add( "flatten" );
        command.add( task.getSetupFile().toString() );
        exec( command );
    }

}
