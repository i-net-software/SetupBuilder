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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

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

    public void build() {
        try {
            Project project = task.getProject();

            String name = setup.getBaseName();
            AppBundlerTask appBundler = new AppBundlerTask();
            appBundler.setOutputDirectory( buildDir );
            appBundler.setName( name );
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
            File iconFile = ImageFactory.getImageFile( project, iconData, buildDir, "icns" );
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

            appBundler.execute();

            task.copyTo( new File( buildDir, name + ".app/Contents/Java" ) );
            Files.copy( iconFile.toPath(), new File( buildDir, ".VolumeIcon.icns" ).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );

            createBinary();
        } catch( RuntimeException ex ) {
            throw ex;
        } catch( Exception ex ) {
            throw new RuntimeException( ex );
        }
    }

    /**
     * Create the binary with native tools.
     * 
     * @throws IOException
     */
    private void createBinary() throws IOException {
        setVolumeIcon();
        createTempImage();
        mountImage();
        applescript();
    }

    /**
     * Call SetFile to set the volume icon.
     */
    private void setVolumeIcon() {
        ArrayList<String> command = new ArrayList<>();
        command.add( "SetFile" );
        command.add( "-c" );
        command.add( "icnC" );
        command.add( buildDir + "/.VolumeIcon.icns" );
        exec( command );
    }

    /**
     * Call hdiutil to create a temporary image.
     */
    private void createTempImage() {
        long size = calcDirectorySize( buildDir );
        size = (size + 0x100000) / 0x100000; // size in MB
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/hdiutil" );
        command.add( "create" );
        command.add( "-srcfolder" );
        command.add( buildDir.toString() );
        command.add( "-volname" );
        command.add( setup.getApplication() );
        command.add( "-fs" );
        command.add( "HFS+" );
        command.add( "-fsargs" );
        command.add( "-c c=64,a=16,e=16" );
        command.add( "-format" );
        command.add( "UDRW" );
        command.add( "-size" );
        command.add( size + "M" );
        command.add( setup.getDestinationDir() + "/pack.temp.dmg" );
        exec( command );
    }

    /**
     * Call hdiutil to mount temporary image
     */
    private void mountImage() {
        long size = calcDirectorySize( buildDir );
        size = ((size + 0x100000) / 0x100000) * 0x100000; // size in MB
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/hdiutil" );
        command.add( "attach" );
        command.add( "-readwrite" );
        command.add( "-noverify" );
        command.add( "-noautoopen" );
        command.add( setup.getDestinationDir() + "/pack.temp.dmg" );
        exec( command );
    }

    private void applescript() throws IOException {
        InputStream input = getClass().getResourceAsStream( "applescript.txt" );
        byte[] bytes = new byte[input.available()];
        input.read( bytes );
        input.close();
        String script = new String( bytes, StandardCharsets.UTF_8 );
        script.replace( "${title}", setup.getApplication() );

        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/osascript" );
        exec( command, new ByteArrayInputStream( script.getBytes( StandardCharsets.UTF_8 ) ) );
    }

    /**
     * Calculate the usage size of a directory
     * 
     * @param dir the directory
     * @return the size in bytes
     */
    private long calcDirectorySize( File dir ) {
        long size = 0;
        for( File file : dir.listFiles() ) {
            if( file.isDirectory() ) {
                size += calcDirectorySize( file ) + 0x10000; // add 64 KB
            } else {
                long fileSize = file.length();
                fileSize = ((fileSize + 0xFFFF) / 0x10000) * 0x10000; //round to 64KB
                size += fileSize;
            }
        }
        return size;
    }
}
