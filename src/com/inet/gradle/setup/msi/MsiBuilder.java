/*
 * Copyright 2015 - 2016 i-net software
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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.gradle.api.GradleException;
import org.gradle.api.file.CopySpec;
import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.abstracts.AbstractBuilder;
import com.inet.gradle.setup.util.ResourceUtils;

import groovy.lang.Closure;

/**
 * Build a MSI setup for Windows.
 *
 * @author Volker Berlin
 */
class MsiBuilder extends AbstractBuilder<Msi,SetupBuilder> {

    private SetupBuilder setup;

    /**
     * Create a new instance
     *
     * @param msi the calling task
     * @param setup the shared settings
     * @param fileResolver the file Resolver
     */
    MsiBuilder( Msi msi, SetupBuilder setup, FileResolver fileResolver ) {
        super( msi, fileResolver );
        this.setup = setup;
    }

    /**
     * Build the MSI installer.
     */
    void build() {
        try {

            File wxsFile = getWxsFile();
            URL template = task.getWxsTemplateURL();
            new WxsFileBuilder( task, setup, wxsFile, buildDir, template, false ).build();
            template = wxsFile.toURI().toURL();

            buildLauch4j();
            candle();

            ResourceUtils.extract( getClass(), "sdk/MsiTran.exe", buildDir );
            ResourceUtils.extract( getClass(), "sdk/wilangid.vbs", buildDir );
            ResourceUtils.extract( getClass(), "sdk/wisubstg.vbs", buildDir );

            List<MsiLanguages> languages = task.getLanguages();
            String[] languageResources = getLanguageResources();

            File mui = light( languages.get( 0 ), languageResources );
            HashMap<MsiLanguages, File> translations = new HashMap<>();
            for( int i = 1; i < languages.size(); i++ ) {
                MsiLanguages language = languages.get( i );
                File file = light( language, languageResources );
                patchLangID( file, language );
                File mst = msitran( mui, file, language );
                translations.put( language, mst );
            }

            // Now create a msi with all files
            new WxsFileBuilder( task, setup, wxsFile, buildDir, template, true ).build();
            candle();
            mui = light( languages.get( 0 ), languageResources );

            // Add the translations to the msi with all files
            StringBuilder langIDs = new StringBuilder( languages.get( 0 ).getLangID() );
            for( Entry<MsiLanguages, File> entry : translations.entrySet() ) {
                MsiLanguages language = entry.getKey();
                File mst = entry.getValue();
                addTranslation( mui, mst, language );
                langIDs.append( ',' ).append( language.getLangID() );
            }
            patchLangID( mui, langIDs.toString() );

            // signing and moving the final msi file
            signTool( mui );
            Files.move( mui.toPath(), task.getSetupFile().toPath(), StandardCopyOption.REPLACE_EXISTING );
        } catch( RuntimeException ex ) {
            throw ex;
        } catch( Exception ex ) {
            throw new RuntimeException( ex );
        }
    }

    /**
     * Get a list of matching files for the resource location
     * @param msiLanguages
     * @return String list of files.
     */
    private String[] getLanguageResources() {

        List<MsiLocalizedResource> i18n = task.getI18n();
        List<String> i18nFiles = new ArrayList<>();

        for( MsiLocalizedResource msiLocalizedResource : i18n ) {
            File result = msiLocalizedResource.getResource();
            if ( result != null ) {
                i18nFiles.add( result.getAbsolutePath() );
            }
        }

        return i18nFiles.toArray( new String[i18nFiles.size()] );
    }

    /**
     * Create the lauch4j starter if there was set some and add it to the sources.
     *
     * @throws Exception if any error occur
     */
    private void buildLauch4j() throws Exception {
        if( task.getLaunch4js().size() > 0 ) {
            Launch4jCreator creator = new Launch4jCreator();
            for( Launch4j launch : task.getLaunch4js() ) {
                File file = creator.create( launch, task, setup );
                signTool( file );
                CopySpec copySpec = task.getProject().copySpec( (Closure<CopySpec>)null );
                task.with( copySpec );
                copySpec.from( file );
                String workDir = launch.getWorkDir();
                if( workDir != null && !workDir.isEmpty() ) {
                    copySpec.into( workDir );
                }
            }
            creator.close();
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
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add( "-nologo" );
        parameters.add( "-arch" );
        parameters.add( task.getArch() );
        parameters.add( "-out" );
        parameters.add( buildDir.getAbsolutePath() + '\\' );
        parameters.add( getWxsFile().getAbsolutePath() );
        for(File external : task.getExternals()){
            parameters.add( external.getAbsolutePath() );
        }
        parameters.add( "-ext" );
        parameters.add( "WixUtilExtension" );

        callWixTool( "candle.exe", parameters );
    }

    /**
     * Call the light.exe tool.
     *
     * @param language the target language
     * @param languageResources the language resource files
     * @return the generated msi file
     */
    private File light( MsiLanguages language, String[] languageResources ) {
        File out = new File( buildDir, setup.getArchiveName() + '_' + language.getCulture() + ".msi" );
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add( "-nologo" );
        parameters.add( "-sice:ICE60" ); // accept *.ttf files to install in the install directory
        parameters.add( "-ext" );
        parameters.add( "WixUIExtension" );
        parameters.add( "-ext" );
        parameters.add( "WixUtilExtension" );
        parameters.add( "-out" );
        parameters.add( out.getAbsolutePath() );
        parameters.add( "-spdb" );
        parameters.add( "-cultures:" + language.getCulture() + ";neutral" );

        // Add locations
        if ( languageResources != null ) {
            for( String location : languageResources ) {
                parameters.add( "-loc" );
                parameters.add( location );
            }
        }

        // Set a localized EULA file
        File localizedRtfFile = MsiLocalizedResource.localizedRtfFile( task.getTemporaryDir(), language );
        if ( localizedRtfFile.exists() ) {
            parameters.add( "-dWixUILicenseRtf=\"" + localizedRtfFile.getAbsolutePath() + "\"" );
        }

        // Check if we should skip msi validation
        if ( task.isSkipValidation() ) {
            parameters.add( "-sval" );
        }

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
        parameters.add( "//Nologo" );
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
        parameters.add( "//Nologo" );
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
     * @param language current language
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
        parameters.add( "//Nologo" );
        parameters.add( new File( buildDir, "sdk/wisubstg.vbs" ).getAbsolutePath() );
        parameters.add( mui.getAbsolutePath() );
        parameters.add( mst.getAbsolutePath() );
        parameters.add( language.getLangID() );
        exec( parameters );
        mst.delete(); // after adding the mst file we does not need it anymore
    }

    /**
     * Sign a file if the needed information are set.
     *
     * @param file file to sign
     * @throws IOException If any I/O error occur on loading of the sign tool
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

        parameters.add( "/d" ); // http://stackoverflow.com/questions/4315840/the-uac-prompt-shows-a-temporary-random-program-name-for-msi-can-the-correct-na
        parameters.add( setup.getApplication() );

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
        return new File( buildDir, setup.getArchiveName() + ".wxs" );
    }

    /**
     * Get the calling path (include name) of a WIX tool
     *
     * @param tool the name of the tool file
     * @return the path
     */
    private static String getToolPath( String tool ) {
        // first check the environ variable WIX
        String wix = System.getenv( "WIX" );
        if( wix == null ) {
            wix = System.getProperty( "WIX" ); // try the system property because a property can be set easer from a gradle script
        }
        if( wix != null ) {
            File file = new File( wix );
            file = new File( file, "bin\\" + tool );
            if( file.exists() ) {
                return file.getAbsolutePath();
            }
        }

        // search on well known folders
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

        throw new GradleException( tool + " was not found. You need to install the WiX Toolset or set the environment variable WIX. You can download the WiX Toolset from http://wixtoolset.org/" );
    }
}
