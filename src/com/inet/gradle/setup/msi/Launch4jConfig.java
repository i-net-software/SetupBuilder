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
package com.inet.gradle.setup.msi;

import java.io.File;

import org.w3c.dom.Element;

import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.abstracts.DesktopStarter;
import com.inet.gradle.setup.util.XmlFileBuilder;

/**
 * Create a XML configuration file for lauch4j.
 * 
 * @author Volker
 */
class Launch4jConfig extends XmlFileBuilder<Msi> {

    private Launch4j launch;

    /**
     * Create a instance.
     * 
     * @param launch the launch description
     * @param task current task
     * @param setup the SetupBuilder
     * @throws Exception if any error occur
     */
    Launch4jConfig( Launch4j launch, Msi task, SetupBuilder setup ) throws Exception {
        super( task, setup, File.createTempFile( "launch4j", ".xml", task.getTemporaryDir() ), task.getTemporaryDir(), null );
        this.launch = launch;
    }

    /**
     * Create the XML file.
     * 
     * @return the created file
     * @throws Exception if an error occurs on reading the image files
     */
    File build() throws Exception {
        String exe = launch.getExecutable();
        if( exe == null ) {
            throw new RuntimeException( "No executable set for launch4j." );
        }
        File outfile = new File( buildDir, exe );
        Element launch4jConfig = getOrCreateChild( doc, "launch4jConfig" );
        getOrCreateChild( launch4jConfig, "headerType" ).setTextContent( "gui" );
        getOrCreateChild( launch4jConfig, "dontWrapJar" ).setTextContent( "true" );
        String mainClass = launch.getMainClass();
        if( mainClass == null || mainClass.isEmpty() ) {
            getOrCreateChild( launch4jConfig, "jar" ).setTextContent( launch.getMainJar() );
        } else {
            Element classPath = getOrCreateChild( launch4jConfig, "classPath" );
            getOrCreateChild( classPath, "mainClass" ).setTextContent( mainClass );
            getOrCreateChild( classPath, "cp" ).setTextContent( launch.getMainJar() );
        }
        getOrCreateChild( launch4jConfig, "outfile" ).setTextContent( outfile.getAbsolutePath() );
        getOrCreateChild( launch4jConfig, "errTitle" ).setTextContent( launch.getDisplayName() );
        getOrCreateChild( launch4jConfig, "chdir" ).setTextContent( "." );

        //        <manifest>hd.manifest</manifest>
        File ico = launch.getIconForType( buildDir, "ico" );
        if ( ico != null ) {
            getOrCreateChild( launch4jConfig, "icon" ).setTextContent( ico.getAbsolutePath() );
        }

        Element jre = getOrCreateChild( launch4jConfig, "jre" );
        Object bundleJRE = setup.getBundleJre();
        if( bundleJRE != null ) {
            String jreTarget = setup.getBundleJreTarget();
            String workDir = launch.getWorkDir();
            if( workDir != null && !workDir.isEmpty() ) {
                int count = workDir.split( "[/\\\\]" ).length;
                for( int i = 0; i < count; i++ ) {
                    jreTarget = "..\\" + jreTarget;
                }
            }
            getOrCreateChild( jre, "path" ).setTextContent( jreTarget );
        } else {
            getOrCreateChild( jre, "minVersion" ).setTextContent( System.getProperty( "java.version" ) );
        }
        getOrCreateChild( jre, "runtimeBits" ).setTextContent( task.is64Bit() ? "64" : "32" );

        Element versionInfo = getOrCreateChild( launch4jConfig, "versionInfo" );
        getOrCreateChild( versionInfo, "fileVersion" ).setTextContent( normalizeVersionNumber( setup.getVersion() ) );
        getOrCreateChild( versionInfo, "txtFileVersion" ).setTextContent( setup.getVersion() );
        getOrCreateChild( versionInfo, "productVersion" ).setTextContent( normalizeVersionNumber( setup.getVersion() ) );
        getOrCreateChild( versionInfo, "txtProductVersion" ).setTextContent( setup.getVersion() );
        getOrCreateChild( versionInfo, "fileDescription" ).setTextContent( launch.getDescription() );
        getOrCreateChild( versionInfo, "productName" ).setTextContent( setup.getApplication() );
        getOrCreateChild( versionInfo, "companyName" ).setTextContent( setup.getVendor() );
        getOrCreateChild( versionInfo, "copyright" ).setTextContent( setup.getCopyright() );

        getOrCreateChild( versionInfo, "originalFilename" ).setTextContent( exe );
        int idx = exe.lastIndexOf( '.' );
        if( idx > 0 ) {
            exe = exe.substring( 0, idx );
        }
        getOrCreateChild( versionInfo, "internalName" ).setTextContent( exe );

        Launch4jManifest manifest = new Launch4jManifest( launch, task, setup );
        manifest.build();
        manifest.save();
        getOrCreateChild( launch4jConfig, "manifest" ).setTextContent( manifest.xmlFile.getAbsolutePath() );

        return outfile;
    }

    /**
     * Normalize the number in the format x.x.x.x
     * 
     * @param version current version
     * @return normalize version
     */
    static String normalizeVersionNumber( String version ) {
        String[] digits = version.split( "[.]" );
        StringBuilder newVersion = new StringBuilder();
        int count = 0;
        for( int i = 0; i < digits.length && count < 4; i++ ) {
            String digit = digits[i];
            try {
                Integer.parseInt( digit );
            } catch( NumberFormatException e ) {
                digit = "0";
            }
            newVersion.append( '.' ).append( digit );
            count++;
        }
        while( count < 4 ) {
            newVersion.append( ".0" );
            count++;
        }
        return newVersion.substring( 1 );
    }
}
