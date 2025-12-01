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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.gradle.api.GradleException;
import org.gradle.api.file.CopySpec;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.internal.os.OperatingSystem;

import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.abstracts.AbstractBuilder;
import com.inet.gradle.setup.util.ResourceUtils;

import groovy.lang.Closure;

/**
 * Build a MSI setup for Windows.
 * Supports both WiX v3 (candle.exe/light.exe) and WiX v4 (wix build) toolchains.
 * Automatically detects which version is available and uses it accordingly.
 * Works on Windows, Linux, and macOS when WiX v4 is installed.
 * 
 * @author Volker Berlin
 */
class MsiBuilder extends AbstractBuilder<Msi, SetupBuilder> {

    private SetupBuilder setup;
    private static final OperatingSystem OS = OperatingSystem.current();
    private static Boolean wixV4Available = null; // Cache the detection result
    private static String wixV4Path = null; // Cache the path to wix command

    /**
     * Create a new instance
     * @param msi the calling task
     * @param setup the shared settings
     * @param fileResolver the file Resolver
     */
    MsiBuilder( Msi msi, SetupBuilder setup, FileResolver fileResolver ) {
        super( msi, fileResolver );
        this.setup = setup;
    }

    /**
     * Check if running on Windows
     * @return true if Windows
     */
    private static boolean isWindows() {
        return OS.isWindows();
    }

    /**
     * Check if running on Linux
     * @return true if Linux
     */
    private static boolean isLinux() {
        return OS.isLinux();
    }

    /**
     * Check if running on macOS
     * @return true if macOS
     */
    private static boolean isMacOs() {
        return OS.isMacOsX();
    }

    /**
     * Detect if WiX v4 is available
     * @return true if WiX v4 is available, false if only v3 is available
     */
    private static boolean isWixV4Available() {
        if( wixV4Available != null ) {
            return wixV4Available;
        }

        // Try to find the 'wix' command (WiX v4)
        String wixCommand = isWindows() ? "wix.exe" : "wix";
        wixV4Path = findCommandInPath( wixCommand );
        
        if( wixV4Path != null ) {
            // Verify it's actually WiX v4 by checking version
            try {
                ArrayList<String> versionCheck = new ArrayList<>();
                versionCheck.add( wixV4Path );
                versionCheck.add( "--version" );
                String output = execForOutput( versionCheck );
                // WiX v4 should output something like "WiX Toolset v4.x.x"
                if( output != null && ( output.contains( "WiX Toolset v4" ) || output.contains( "wix v4" ) || output.matches( ".*\\bv4\\.\\d+.*" ) ) ) {
                    wixV4Available = true;
                    return true;
                }
            } catch( Exception e ) {
                // If version check fails, assume it's not v4
                wixV4Path = null;
            }
        }

        wixV4Available = false;
        return false;
    }

    /**
     * Find a command in the system PATH
     * @param command the command name
     * @return the full path if found, null otherwise
     */
    private static String findCommandInPath( String command ) {
        String path = System.getenv( "PATH" );
        if( path == null ) {
            return null;
        }

        String[] pathDirs = path.split( File.pathSeparator );
        for( String dir : pathDirs ) {
            File commandFile = new File( dir, command );
            if( commandFile.exists() && commandFile.canExecute() ) {
                return commandFile.getAbsolutePath();
            }
        }

        // Also check WIX environment variable
        String wixEnv = System.getenv( "WIX" );
        if( wixEnv != null ) {
            File commandFile = new File( wixEnv, "bin" + File.separator + command );
            if( commandFile.exists() && commandFile.canExecute() ) {
                return commandFile.getAbsolutePath();
            }
        }

        return null;
    }

    /**
     * Execute a command and return its output
     * @param parameters command and parameters
     * @return the output string, or null if execution failed
     */
    private static String execForOutput( ArrayList<String> parameters ) {
        try {
            ProcessBuilder pb = new ProcessBuilder( parameters );
            pb.redirectErrorStream( true );
            Process process = pb.start();
            java.io.InputStream is = process.getInputStream();
            java.io.BufferedReader reader = new java.io.BufferedReader( new java.io.InputStreamReader( is ) );
            StringBuilder output = new StringBuilder();
            String line;
            while( ( line = reader.readLine() ) != null ) {
                output.append( line ).append( "\n" );
            }
            int exitCode = process.waitFor();
            if( exitCode == 0 ) {
                return output.toString();
            }
        } catch( Exception e ) {
            // Ignore exceptions, return null
        }
        return null;
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

            // Extract Windows-specific tools only on Windows (or if Wine/Docker is available)
            // TODO Option A: Replace with Java-based MSI manipulation libraries
            //   - Use libraries like 'msi4j' or 'windows-installer' Java libraries
            //   - Or use COM4J/JNA to call Windows Installer APIs via Wine
            //   - Or implement MSI database manipulation directly using Java
            if( isWindows() ) {
                ResourceUtils.extract( getClass(), "sdk/MsiTran.exe", buildDir );
                ResourceUtils.extract( getClass(), "sdk/wilangid.vbs", buildDir );
                ResourceUtils.extract( getClass(), "sdk/wisubstg.vbs", buildDir );
            } else {
                task.getProject().getLogger().warn( "MSI multi-language support: Windows-only tools (MsiTran.exe, VBScript) are not available on " + 
                    ( isLinux() ? "Linux" : "macOS" ) + ". Multi-language MSI builds will be limited. " +
                    "Only the primary language will be supported. For full multi-language support, build on Windows or use Wine/Docker." );
            }

            List<MsiLanguages> languages = task.getLanguages();
            String[] languageResources = getLanguageResources();

            // Check if multi-language build is requested on non-Windows
            if( !isWindows() && languages.size() > 1 ) {
                task.getProject().getLogger().warn( "Multi-language MSI build requested on " + 
                    ( isLinux() ? "Linux" : "macOS" ) + ", but only single-language builds are supported. " +
                    "Only the first language (" + languages.get( 0 ).getCulture() + ") will be used." );
            }

            File mui = light( languages.get( 0 ), languageResources );
            HashMap<MsiLanguages, File> translations = new HashMap<>();
            
            // Multi-language support: Only process additional languages on Windows
            // TODO Option A: Implement cross-platform multi-language support using Java-based MSI manipulation
            if( isWindows() && languages.size() > 1 ) {
                for( int i = 1; i < languages.size(); i++ ) {
                    MsiLanguages language = languages.get( i );
                    File file = light( language, languageResources );
                    patchLangID( file, language );
                    File mst = msitran( mui, file, language );
                    if( mst != null ) {
                        translations.put( language, mst );
                    }
                }
            }

            // Now create a msi with all files
            new WxsFileBuilder( task, setup, wxsFile, buildDir, template, true ).build();
            candle();
            mui = light( languages.get( 0 ), languageResources );

            // Add the translations to the msi with all files (Windows only)
            if( isWindows() && !translations.isEmpty() ) {
                StringBuilder langIDs = new StringBuilder( languages.get( 0 ).getLangID() );
                for( Entry<MsiLanguages, File> entry : translations.entrySet() ) {
                    MsiLanguages language = entry.getKey();
                    File mst = entry.getValue();
                    addTranslation( mui, mst, language );
                    langIDs.append( ',' ).append( language.getLangID() );
                }
                patchLangID( mui, langIDs.toString() );
            } else if( !isWindows() && languages.size() > 1 ) {
                // On non-Windows, just use the primary language
                task.getProject().getLogger().info( "MSI built with primary language only: " + languages.get( 0 ).getCulture() );
            }

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
            if( result != null ) {
                i18nFiles.add( result.getAbsolutePath() );
            }
        }

        return i18nFiles.toArray( new String[i18nFiles.size()] );
    }

    /**
     * Create the lauch4j starter if there was set some and add it to the sources.
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
     * @param tool the program name
     * @param parameters the parameters
     */
    private void callWixTool( String tool, ArrayList<String> parameters ) {
        parameters.add( 0, getToolPath( tool ) );
        exec( parameters );
    }

    /**
     * Call WiX v4 build command (unified build process)
     * @param wxsFile the WiX source file
     * @param outputFile the output MSI file
     * @param language the target language
     * @param languageResources the language resource files
     */
    private void callWixV4Build( File wxsFile, File outputFile, MsiLanguages language, String[] languageResources ) {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add( wixV4Path );
        parameters.add( "build" );
        parameters.add( "-arch" );
        parameters.add( task.getArch() );
        parameters.add( "-out" );
        parameters.add( outputFile.getAbsolutePath() );

        // Add extensions
        for( String extension : task.getWixExtensions() ) {
            parameters.add( "-ext" );
            parameters.add( extension );
        }
        parameters.add( "-ext" );
        parameters.add( "WixUIExtension" );
        parameters.add( "-ext" );
        parameters.add( "WixUtilExtension" );

        // Add culture/locale
        parameters.add( "-cultures:" + language.getCulture() );

        // Add language resources
        if( languageResources != null ) {
            for( String location : languageResources ) {
                parameters.add( "-loc" );
                parameters.add( location );
            }
        }

        // Set a localized EULA file
        File localizedRtfFile = MsiLocalizedResource.localizedRtfFile( task.getTemporaryDir(), language );
        if( localizedRtfFile.exists() ) {
            parameters.add( "-dWixUILicenseRtf=" + localizedRtfFile.getAbsolutePath() );
        }

        // Add external files
        for( File external : task.getExternals() ) {
            parameters.add( external.getAbsolutePath() );
        }

        // Add the main wxs file
        parameters.add( wxsFile.getAbsolutePath() );

        // Check if we should skip msi validation
        if( task.isSkipValidation() ) {
            parameters.add( "-sval" );
        }

        exec( parameters );
    }

    /**
     * Call the candle.exe tool (WiX v3) or skip if using WiX v4.
     */
    private void candle() {
        // WiX v4 doesn't need a separate candle step - it's integrated into 'wix build'
        if( isWixV4Available() ) {
            task.getProject().getLogger().debug( "Using WiX v4 - skipping candle step (integrated into build)" );
            return;
        }

        // WiX v3: compile .wxs to .wixobj
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add( "-nologo" );
        parameters.add( "-arch" );
        parameters.add( task.getArch() );
        parameters.add( "-out" );
        parameters.add( buildDir.getAbsolutePath() + File.separator );
        parameters.add( getWxsFile().getAbsolutePath() );
        for( File external : task.getExternals() ) {
            parameters.add( external.getAbsolutePath() );
        }
        for( String extension : task.getWixExtensions() ) {
            parameters.add( "-ext" );
            parameters.add( extension );
        }
        parameters.add( "-ext" );
        parameters.add( "WixUtilExtension" );

        // Use platform-appropriate tool name
        String toolName = isWindows() ? "candle.exe" : "candle";
        callWixTool( toolName, parameters );
    }

    /**
     * Call the light.exe tool (WiX v3) or use wix build (WiX v4).
     * @param language the target language
     * @param languageResources the language resource files
     * @return the generated msi file
     */
    private File light( MsiLanguages language, String[] languageResources ) {
        File out = new File( buildDir, setup.getArchiveName() + '_' + language.getCulture() + ".msi" );

        if( isWixV4Available() ) {
            // WiX v4: Use unified 'wix build' command
            task.getProject().getLogger().info( "Using WiX v4 to build MSI" );
            callWixV4Build( getWxsFile(), out, language, languageResources );
            return out;
        }

        // WiX v3: Use separate light.exe tool
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add( "-nologo" );
        parameters.add( "-sice:ICE60" ); // accept *.ttf files to install in the install directory
        for( String extension : task.getWixExtensions() ) {
            parameters.add( "-ext" );
            parameters.add( extension );
        }
        parameters.add( "-ext" );
        parameters.add( "WixUIExtension" );
        parameters.add( "-ext" );
        parameters.add( "WixUtilExtension" );
        parameters.add( "-out" );
        parameters.add( out.getAbsolutePath() );
        parameters.add( "-spdb" );
        parameters.add( "-cultures:" + language.getCulture() + ";neutral" );

        // Add locations
        if( languageResources != null ) {
            for( String location : languageResources ) {
                parameters.add( "-loc" );
                parameters.add( location );
            }
        }

        // Set a localized EULA file
        File localizedRtfFile = MsiLocalizedResource.localizedRtfFile( task.getTemporaryDir(), language );
        if( localizedRtfFile.exists() ) {
            parameters.add( "-dWixUILicenseRtf=" + localizedRtfFile.getAbsolutePath() );
        }

        // Check if we should skip msi validation
        if( task.isSkipValidation() ) {
            parameters.add( "-sval" );
        }

        parameters.add( "*.wixobj" );
        // Use platform-appropriate tool name
        String toolName = isWindows() ? "light.exe" : "light";
        callWixTool( toolName, parameters );
        return out;
    }

    /**
     * Change the language ID of a *.msi file.
     * 
     * Windows-only: Uses VBScript (cscript) to modify MSI database.
     * 
     * TODO Option A: Java-based implementation
     *   To implement cross-platform support, consider:
     *   1. Use MSI database manipulation libraries:
     *      - 'msi4j' or similar Java libraries for MSI file manipulation
     *      - Direct MSI database access using Java (MSI files are SQL databases)
     *   2. Use JNA/COM4J to call Windows Installer APIs via Wine:
     *      - MsiDatabaseOpenView, MsiViewExecute, MsiRecordSetString
     *   3. Implement direct MSI database modification:
     *      - MSI files are OLE Structured Storage (compound documents)
     *      - Use libraries like Apache POI for OLE or direct binary manipulation
     *      - Modify the _SummaryInformation stream or Property table directly
     * 
     * @param file a msi file
     * @param language the target language
     */
    private void patchLangID( File file, MsiLanguages language ) {
        if( !isWindows() ) {
            task.getProject().getLogger().warn( "patchLangID: Skipping language ID patch on non-Windows platform. " +
                "MSI will use default language. For full multi-language support, build on Windows." );
            return;
        }
        
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
     * Windows-only: Uses VBScript (cscript) to modify MSI database.
     * 
     * TODO Option A: See patchLangID(File, MsiLanguages) for implementation hints.
     *   This method modifies the Package language IDs in the MSI _SummaryInformation stream.
     * 
     * @param mui the multilingual user interface (MUI) installer file
     * @param langIDs a comma separated list of languages IDs
     */
    private void patchLangID( File mui, String langIDs ) {
        if( !isWindows() ) {
            task.getProject().getLogger().warn( "patchLangID: Skipping package language ID patch on non-Windows platform. " +
                "MSI will use default language. For full multi-language support, build on Windows." );
            return;
        }
        
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
     * Windows-only: Uses MsiTran.exe to create transform files for multi-language support.
     * 
     * TODO Option A: Java-based implementation
     *   To implement cross-platform support, consider:
     *   1. Use MSI transform generation libraries:
     *      - Implement MST file creation using Java (MST files are MSI databases with differences)
     *      - Use MSI database APIs to compute differences between two MSI files
     *   2. Use Wine to run MsiTran.exe:
     *      - Check if Wine is available: `which wine` or `wine --version`
     *      - Execute: `wine MsiTran.exe -g mui.msi file.msi output.mst`
     *   3. Use WiX v4 tools (if available):
     *      - Check if WiX v4 has cross-platform transform tools
     *      - May have `wix transform` or similar command
     *   4. Direct MSI database manipulation:
     *      - Compare two MSI databases and create transform
     *      - Transform = differences between base MSI and localized MSI
     * 
     * @param mui the multilingual user interface (MUI) installer file
     * @param file the current msi file
     * @param language current language
     * @return the *.mst file, or null on non-Windows
     */
    private File msitran( File mui, File file, MsiLanguages language ) {
        if( !isWindows() ) {
            task.getProject().getLogger().warn( "msitran: Skipping transform file creation on non-Windows platform. " +
                "Multi-language MSI support requires Windows or Wine. Returning null." );
            file.delete(); // Clean up the file since we can't process it
            return null;
        }
        
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
     * 
     * Windows-only: Uses VBScript (cscript) to add transform files to MSI.
     * 
     * TODO Option A: Java-based implementation
     *   To implement cross-platform support, consider:
     *   1. Use MSI database manipulation to add transforms:
     *      - MSI transforms are stored in the _Storages table
     *      - Use MSI database APIs to insert transform storage entries
     *      - Transform storage name format: language code (e.g., "1033" for en-US)
     *   2. Use JNA/COM4J to call Windows Installer APIs via Wine:
     *      - MsiDatabaseApplyTransform, MsiGetSummaryInformation
     *   3. Direct MSI database modification:
     *      - Add transform to _Storages table in MSI database
     *      - Update _SummaryInformation stream with transform references
     * 
     * @param mui the multilingual user interface (MUI) installer file
     * @param mst the transform file
     * @param language current language
     */
    private void addTranslation( File mui, File mst, MsiLanguages language ) {
        if( !isWindows() ) {
            task.getProject().getLogger().warn( "addTranslation: Skipping transform addition on non-Windows platform. " +
                "Multi-language MSI support requires Windows. Transform file will be deleted." );
            if( mst != null && mst.exists() ) {
                mst.delete();
            }
            return;
        }
        
        if( mst == null || !mst.exists() ) {
            task.getProject().getLogger().warn( "addTranslation: Transform file does not exist, skipping." );
            return;
        }
        
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
     * @param file file to sign
     * @throws IOException If any I/O error occur on loading of the sign tool
     */
    private void signTool( File file ) throws IOException {
        if( task.getExternalSignTool() != null ) {
            task.getExternalSignTool().call( file );
            return;
        }

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

    private static boolean isValidEncoding( String input, Charset charset ) {
        return Charset.forName( charset.name() ).newEncoder().canEncode( input );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void exec( ArrayList<String> parameters, InputStream input, OutputStream output, OutputStream error, boolean ignoreExitValue ) {

        // check for non-iso parameters
        for( String string : parameters ) {
            if( !isValidEncoding( string, StandardCharsets.ISO_8859_1 ) ) {
                task.getProject().getLogger().error( String.format( "The following parameter contains illegal non-ISO characters which are not supported: '%s'", string ) );
            }
        }

        super.exec( parameters, input, output, error, ignoreExitValue );
    }

    /**
     * Get the name of the wxs file
     * @return the xml file
     */
    private File getWxsFile() {
        return new File( buildDir, setup.getArchiveName() + ".wxs" );
    }

    /**
     * Get the calling path (include name) of a WIX tool
     * @param tool the name of the tool file
     * @return the path
     */
    private static String getToolPath( String tool ) {
        // First check the environment variable WIX (works on all platforms)
        String wix = System.getenv( "WIX" );
        if( wix == null ) {
            wix = System.getProperty( "WIX" ); // try the system property because a property can be set easier from a gradle script
        }
        if( wix != null ) {
            File file = new File( wix );
            // Use platform-appropriate path separator
            file = new File( file, "bin" + File.separator + tool );
            if( file.exists() ) {
                return file.getAbsolutePath();
            }
        }

        if( isWindows() ) {
            // Windows-specific search paths
            return getWindowsToolPath( tool );
        } else {
            // Linux/macOS search paths
            return getUnixToolPath( tool );
        }
    }

    /**
     * Get WiX tool path on Windows
     * @param tool the tool name
     * @return the path
     */
    private static String getWindowsToolPath( String tool ) {
        // search on well known folders
        String programFilesStr = System.getenv( "ProgramFiles(x86)" );
        if( programFilesStr == null ) {
            programFilesStr = System.getenv( "ProgramW6432" );
        }
        if( programFilesStr == null ) {
            throw new GradleException( "Environment variable ProgramFiles not found. Please set the WIX environment variable to point to your WiX installation." );
        }

        File programFiles = new File( programFilesStr );
        String[] programs = programFiles.list();
        if( programs == null ) {
            throw new GradleException( tool + " was not found. Please set the WIX environment variable to point to your WiX installation." );
        }

        // Searching the WiX Toolset
        for( String program : programs ) {
            if( program.toLowerCase().startsWith( "wix toolset" ) ) {
                File file = new File( programFiles, program + File.separator + "bin" + File.separator + tool );
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
                if( wixEditFiles != null ) {
                    for( String wixEditFile : wixEditFiles ) {
                        if( wixEditFile.toLowerCase().startsWith( "wix" ) ) {
                            File file = new File( wixEdit, wixEditFile + File.separator + tool );
                            if( file.exists() ) {
                                return file.getAbsolutePath();
                            }
                        }
                    }
                }
            }
        }

        throw new GradleException( tool + " was not found. You need to install the WiX Toolset or set the environment variable WIX. You can download the WiX Toolset from http://wixtoolset.org/" );
    }

    /**
     * Get WiX tool path on Linux/macOS
     * @param tool the tool name
     * @return the path
     */
    private static String getUnixToolPath( String tool ) {
        // Check common installation paths for WiX v4 (cross-platform)
        List<String> searchPaths = new ArrayList<>();
        
        // Check if tool is in PATH
        String path = System.getenv( "PATH" );
        if( path != null ) {
            String[] pathDirs = path.split( File.pathSeparator );
            for( String dir : pathDirs ) {
                File toolFile = new File( dir, tool );
                if( toolFile.exists() && toolFile.canExecute() ) {
                    return toolFile.getAbsolutePath();
                }
            }
        }

        // Common installation locations
        if( isMacOs() ) {
            // macOS: Homebrew installation
            searchPaths.add( "/usr/local/bin/" + tool );
            searchPaths.add( "/opt/homebrew/bin/" + tool );
            searchPaths.add( System.getProperty( "user.home" ) + "/.local/bin/" + tool );
        } else if( isLinux() ) {
            // Linux: Package manager installations
            searchPaths.add( "/usr/bin/" + tool );
            searchPaths.add( "/usr/local/bin/" + tool );
            searchPaths.add( System.getProperty( "user.home" ) + "/.local/bin/" + tool );
        }

        // Check user home directory
        String userHome = System.getProperty( "user.home" );
        searchPaths.add( userHome + "/wix/bin/" + tool );
        searchPaths.add( userHome + "/.wix/bin/" + tool );

        // Check if WIX environment variable points to a directory
        String wixEnv = System.getenv( "WIX" );
        if( wixEnv != null ) {
            searchPaths.add( 0, wixEnv + "/bin/" + tool ); // Check this first
        }

        for( String pathStr : searchPaths ) {
            File toolFile = new File( pathStr );
            if( toolFile.exists() && toolFile.canExecute() ) {
                return toolFile.getAbsolutePath();
            }
        }

        throw new GradleException( tool + " was not found. Please install WiX v4 (cross-platform) or set the WIX environment variable. " +
            "On macOS: brew install wix. On Linux: install via package manager or download from http://wixtoolset.org/" );
    }
}
