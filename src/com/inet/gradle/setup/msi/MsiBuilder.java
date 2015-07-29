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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.AbstractBuilder;
import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.util.ResourceUtils;

/**
 * Build a MSI setup for Windows.
 * 
 * @author Volker Berlin
 */
class MsiBuilder extends AbstractBuilder<Msi> {

    /**
     * Create a new instance
     * 
     * @param msi the calling task
     * @param setup the shared settings
     * @param fileResolver the file Resolver
     */
    MsiBuilder( Msi msi, SetupBuilder setup, FileResolver fileResolver ) {
        super( msi, setup, fileResolver );
    }

    void build() {
        try {
            File wxsFile = getWxsFile();
            new WxsFileBuilder( task, setup, wxsFile, buildDir ).build();
            candle();

            ResourceUtils.extract( getClass(), "sdk/MsiTran.exe", buildDir );
            ResourceUtils.extract( getClass(), "sdk/wilangid.vbs", buildDir );
            ResourceUtils.extract( getClass(), "sdk/wisubstg.vbs", buildDir );

            //TODO 2 languages for testing only
            MsiLanguages[] languages = { MsiLanguages.en_us, MsiLanguages.de_de };

            File mui = light( languages[0] );
            StringBuilder langIDs = new StringBuilder( languages[0].getLangID() );
            for( int i = 1; i < languages.length; i++ ) {
                MsiLanguages language = languages[i];
                File file = light( languages[i] );
                patchLangID( file, language );
                File mst = msitran( mui, file, language );
                addTranslation( mui, mst, language );
                langIDs.append( ',' ).append( language.getLangID() );
            }
            patchLangID( mui, langIDs.toString() );
            signTool( mui );
            Files.move( mui.toPath(), new File( setup.getDestinationDir(), setup.getSetupName() + ".msi" ).toPath(), StandardCopyOption.REPLACE_EXISTING );
        } catch( RuntimeException ex ) {
            throw ex;
        } catch( Exception ex ) {
            throw new RuntimeException( ex );
        }
    }

    /**
     * Call a program from the WIX installation.
     * 
     * @param tool the program name
     * @param parameters the parameters
     */
    private void callWixTool( String tool, ArrayList<String> parameters ) {
        parameters.add( 0, getToolPath( tool ) );
        exec( parameters );
    }

    /**
     * Call the candle.exe tool.
     */
    private void candle() {
        ArrayList<String> command = new ArrayList<>();
        command.add( "-arch" );
        command.add( task.getArch() );
        command.add( "-out" );
        command.add( buildDir.getAbsolutePath() + '\\' );
        command.add( getWxsFile().getAbsolutePath() );

        callWixTool( "candle.exe", command );
    }

    /**
     * Call the light.exe tool.
     */
    private File light( MsiLanguages language ) {
        File out = new File( buildDir, setup.getSetupName() + '_' + language.getCulture() + ".msi" );
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add( "-ext" );
        parameters.add( "WixUIExtension" );
        parameters.add( "-ext" );
        parameters.add( "WixUtilExtension" );
        parameters.add( "-out" );
        parameters.add( out.getAbsolutePath() );
        parameters.add( "-spdb" );
        parameters.add( "-cultures:" + language.getCulture() + ";neutral" );
        parameters.add( "*.wixobj" );
        callWixTool( "light.exe", parameters );
        return out;
    }

    /**
     * Change the language ID of a *.msi file.
     * 
     * @param file a msi file
     * @param language the target language
     */
    private void patchLangID( File file, MsiLanguages language ) {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add( "cscript" );
        parameters.add( new File( buildDir, "sdk/wilangid.vbs" ).getAbsolutePath() );
        parameters.add( file.getAbsolutePath() );
        parameters.add( "Product" );
        parameters.add( language.getLangID() );
        exec( parameters );
    }

    /**
     * Set all languages IDs for which translations was added.
     * 
     * @param mui the multilingual user interface (MUI) installer file
     * @param langIDs a comma separated list of languages IDs
     */
    private void patchLangID( File mui, String langIDs ) {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add( "cscript" );
        parameters.add( new File( buildDir, "sdk/wilangid.vbs" ).getAbsolutePath() );
        parameters.add( mui.getAbsolutePath() );
        parameters.add( "Package" );
        parameters.add( langIDs );
        exec( parameters );
    }

    /**
     * Call the msitran.exe tool and create a transform file (*.mst).
     * 
     * @param mui the multilingual user interface (MUI) installer file
     * @param file the current msi file
     * @param current language
     * @return the *.mst file
     */
    private File msitran( File mui, File file, MsiLanguages language ) {
        File mst = new File( buildDir, language.getCulture() + ".mst" );
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add( new File( buildDir, "sdk/MsiTran.exe" ).getAbsolutePath() );
        parameters.add( "-g" );
        parameters.add( mui.getAbsolutePath() );
        parameters.add( file.getAbsolutePath() );
        parameters.add( mst.getAbsolutePath() );
        exec( parameters );
        file.delete(); // after creation of the mst file we does not need it anymore
        return mst;
    }

    /**
     * Add a transform file to the msi file
     * @param mui the multilingual user interface (MUI) installer file
     * @param mst the transform file
     * @param language current language
     */
    private void addTranslation( File mui, File mst, MsiLanguages language ) {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add( "cscript" );
        parameters.add( new File( buildDir, "sdk/wisubstg.vbs" ).getAbsolutePath() );
        parameters.add( mui.getAbsolutePath() );
        parameters.add( mst.getAbsolutePath() );
        parameters.add( language.getLangID() );
        exec( parameters );
        mst.delete(); // after adding the mst file we does not need it anymore
    }

    /**
     * Sign a file if the needed information are set.
     * @param file
     * @throws IOException
     */
    private void signTool( File file ) throws IOException {
        SignTool sign = task.getSignTool();
        if( sign == null ) {
            return; // no sign information set
        }
        String tool = ResourceUtils.extract( getClass(), "sdk/signtool.exe", buildDir ).getAbsolutePath();

        // signing the file
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add( tool );
        parameters.add( "sign" );
        if( sign.getCertificate() != null ) {
            parameters.add( "/f" );
            parameters.add( task.getProject().file( sign.getCertificate() ).getAbsolutePath() );
        }
        if( sign.getPassword() != null ) {
            parameters.add( "/p" );
            parameters.add( sign.getPassword() );
        }
        if( sign.getSha1() != null ) {
            parameters.add( "/sha1" );
            parameters.add( sign.getSha1() );
        }
        parameters.add( file.getAbsolutePath() );
        exec( parameters );

        // timestamp the signing
        List<String> servers = sign.getTimestamp();
        if( servers != null ) {
            RuntimeException allEx = null;
            for( String server : servers ) {
                parameters = new ArrayList<>();
                parameters.add( tool );
                parameters.add( "timestamp" );
                parameters.add( "/t" );
                parameters.add( server );
                parameters.add( file.getAbsolutePath() );
                try {
                    exec( parameters );
                    allEx = null;
                    break; // timestamp is ok, if no exception occur
                } catch( RuntimeException ex ) {
                    if( allEx == null ) {
                        allEx = ex;
                    } else {
                        allEx.addSuppressed( ex );
                    }
                    task.getProject().getLogger().lifecycle( "Timestamp failed: " + ex );
                }
            }
            if( allEx != null ) {
                throw allEx;
            }
        }
    }

    /**
     * Get the name of the wxs file
     * 
     * @return the xml file
     */
    private File getWxsFile() {
        return new File( buildDir, setup.getSetupName() + ".wxs" );
    }

    /**
     * Get the calling path (include name) of a WIX tool
     * 
     * @param tool the name of the tool file
     * @return the path
     */
    private static String getToolPath( String tool ) {
        String programFilesStr = System.getenv( "ProgramFiles(x86)" );
        if( programFilesStr == null ) {
            programFilesStr = System.getenv( "ProgramW6432" );
        }
        if( programFilesStr == null ) {
            throw new GradleException( "Environment variable ProgramFiles not found." );
        }

        File programFiles = new File( programFilesStr );
        String[] programs = programFiles.list();

        // Searching the WiX Toolset
        for( String program : programs ) {
            if( program.toLowerCase().startsWith( "wix toolset" ) ) {
                File file = new File( programFiles, program + "\\bin\\" + tool );
                if( file.exists() ) {
                    return file.getAbsolutePath();
                }
            }
        }

        // Searching the WixEdit
        for( String program : programs ) {
            if( program.equalsIgnoreCase( "WixEdit" ) ) {
                File wixEdit = new File( programFiles, program );
                String[] wixEditFiles = wixEdit.list();
                for( String wixEditFile : wixEditFiles ) {
                    if( wixEditFile.toLowerCase().startsWith( "wix" ) ) {
                        File file = new File( wixEdit, wixEditFile + "\\" + tool );
                        if( file.exists() ) {
                            return file.getAbsolutePath();
                        }
                    }
                }
            }
        }

        return tool;
    }
}
