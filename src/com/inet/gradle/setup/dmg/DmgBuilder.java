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

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;

import org.gradle.api.Project;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.process.internal.DefaultExecAction;

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

    private void createBinary() {
        setFile();
    }

    private void setFile() {
        ArrayList<String> command = new ArrayList<>();
        command.add( "SetFile" );
        command.add( "-c" );
        command.add( "icnC" );
        command.add( buildDir + "/.VolumeIcon.icns" );
        exec( command );
    }
}
