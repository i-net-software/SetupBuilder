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
import java.util.ArrayList;

import org.gradle.api.GradleException;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.process.internal.DefaultExecAction;
import org.gradle.util.GUtil;

import com.inet.gradle.setup.SetupBuilder;

/**
 * Build a MSI setup for Windows.
 * 
 * @author Volker Berlin
 */
class MsiBuilder {

    private final Msi          msi;

    private final SetupBuilder setup;

    private FileResolver       fileResolver;

    private File               buildDir;

    /**
     * Create a new instance
     * 
     * @param msi the calling task
     * @param setup the shared settings
     * @param fileResolver the file Resolver
     */
    MsiBuilder( Msi msi, SetupBuilder setup, FileResolver fileResolver ) {
        this.msi = msi;
        this.setup = setup;
        this.fileResolver = fileResolver;
    }

    void build() {
        try {
            buildDir = new File( msi.getProject().getBuildDir(), "setup/msi" );
            File wxsFile = getWxsFile();
            new WxsFileBuilder( msi, setup, wxsFile, buildDir ).build();
            candle();
            light();

            if( !wxsFile.exists() ) {
                throw new GradleException( "Setup file not created: " + wxsFile );
            }
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

        // print command line to the log
        StringBuilder log = new StringBuilder();
        for( String para : parameters ) {
            log.append( '\"' ).append( para );
            if( para.endsWith( "\\" ) ) {
                log.append( '\\' );
            }
            log.append( "\" " );
        }
        msi.getProject().getLogger().lifecycle( log.toString() );

        DefaultExecAction action = new DefaultExecAction( fileResolver );
        action.setCommandLine( parameters );
        action.setWorkingDir( buildDir );
        action.execute();
    }

    /**
     * Call the candle.exe tool.
     */
    private void candle() {
        ArrayList<String> command = new ArrayList<>();
        command.add( "-out" );
        command.add( buildDir.getAbsolutePath() + '\\' );
        command.add( getWxsFile().getAbsolutePath() );

        callWixTool( "candle.exe", command );
    }

    /**
     * Call the light.exe tool.
     */
    private void light() {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add( "-ext" );
        parameters.add( "WixUIExtension" );
        parameters.add( "-out" );
        parameters.add( new File( setup.getDestinationDir(), setup.getSetupName() + ".msi" ).getAbsolutePath() );
        parameters.add( "-spdb" );
        parameters.add( "*.wixobj" );
        callWixTool( "light.exe", parameters );
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
    private String getToolPath( String tool ) {
        String programFilesStr = System.getenv( "ProgramFiles(x86)" );
        if( programFilesStr == null ) {
            programFilesStr = System.getenv( "ProgramFiles" );
        }
        if( programFilesStr == null ) {
            throw new GradleException( "Environment ProgramFiles not found." );
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
