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
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.internal.os.OperatingSystem;

import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.util.ResourceUtils;

/**
 * Create the Lauch4j programs if there any configured.
 * Supports cross-platform execution - can create Windows executables on any platform.
 *
 * @author Volker
 */
public class Launch4jCreator {

    private URLClassLoader lauch4jClassLoader;
    private static final OperatingSystem OS = OperatingSystem.current();

    /**
     * Create a single Lauch4j launcher.
     *
     * @param launch the launch description
     * @param task the task
     * @param setup the SetupBuilder
     * @return the file to the created exe.
     * @throws Exception if any error occur
     */
    File create( Launch4j launch, Msi task, SetupBuilder setup ) throws Exception {
        Launch4jConfig config = new Launch4jConfig( launch, task, setup );
        File outfile = config.build();
        config.save();

        Class<?> clazz = Class.forName( "net.sf.launch4j.Main", true, getClassLoader( task.getProject(), task.getTemporaryDir() ) );
        String[] args = { config.xmlFile.getAbsolutePath() };
        clazz.getMethod( "main", String[].class ).invoke( null, (Object)args );

        if( !outfile.exists() ) {
            throw new GradleException( "Launch4j failed. " );
        }
        return outfile;
    }

    /**
     * Get the platform-specific workdir classifier for Launch4j
     * Based on available packages in Maven Central: https://repo1.maven.org/maven2/net/sf/launch4j/launch4j/3.50/
     * Available classifiers: workdir-win32, workdir-linux, workdir-linux64, workdir-mac
     * 
     * @return the classifier string (e.g., "workdir-win32", "workdir-linux64", "workdir-mac")
     */
    private static String getWorkdirClassifier() {
        if( OS.isWindows() ) {
            // Launch4j uses "win32" for both 32 and 64-bit Windows
            return "workdir-win32";
        } else if( OS.isLinux() ) {
            String arch = System.getProperty( "os.arch" );
            // Check if 64-bit Linux
            if( arch != null && ( arch.contains( "64" ) || arch.equals( "amd64" ) || arch.equals( "x86_64" ) ) ) {
                return "workdir-linux64"; // 64-bit Linux
            }
            return "workdir-linux"; // 32-bit or unknown Linux
        } else if( OS.isMacOsX() ) {
            // macOS uses "workdir-mac" (not "workdir-macos")
            return "workdir-mac";
        }
        // Fallback to Windows workdir (may work or may fail gracefully)
        return "workdir-win32";
    }

    /**
     * Download the lauch4j and create a classloader
     *
     * @param project current project
     * @param buildDir current temp directory
     * @return the ClassLoader for lauch4j
     * @throws IOException if any error occur
     */
    private ClassLoader getClassLoader( Project project, File buildDir ) throws IOException {
        if( lauch4jClassLoader == null ) {
            String configName = "setupLaunch4j";
            Configuration config = project.getConfigurations().findByName( configName );
            if( config == null ) {
                config = project.getConfigurations().create( configName );
                config.setVisible( false );
                config.setTransitive( false );
                DependencyHandler dependencies = project.getDependencies();
                dependencies.add( configName, "net.sf.launch4j:launch4j:3.50" );
                
                // Add platform-specific workdir package
                // Available classifiers: workdir-win32, workdir-linux, workdir-linux64, workdir-mac
                String workdirClassifier = getWorkdirClassifier();
                try {
                    dependencies.add( configName, "net.sf.launch4j:launch4j:3.50:" + workdirClassifier );
                    project.getLogger().debug( "Launch4j: Using workdir classifier: " + workdirClassifier );
                } catch( Exception e ) {
                    // If platform-specific workdir doesn't exist, try to continue without it
                    // Launch4j may work without workdir on some platforms
                    project.getLogger().warn( "Launch4j: Platform-specific workdir '" + workdirClassifier + "' not available. " +
                        "Trying to continue without it. If Launch4j fails, you may need to install the workdir package manually. " +
                        "Available classifiers: workdir-win32, workdir-linux, workdir-linux64, workdir-mac" );
                }
                
                dependencies.add( configName, "com.thoughtworks.xstream:xstream:1.4.20" );
            }

            ArrayList<URL> urls = new ArrayList<>();
            File libDir = new File( buildDir, "launch4jlib" );
            libDir.mkdirs();
            boolean workdirFound = false;
            
            for( File file : config.getFiles() ) {
                String name = file.getName();
                if( name.endsWith( ".jar" ) ) {
                    //https://github.com/TheBoegl/gradle-launch4j/blob/develop/src/main/groovy/edu/sc/seis/launch4j/Launch4jPlugin.groovy
                    if( name.contains( "-workdir-" ) ) {
                        workdirFound = true;
                        name = name.substring( 0, name.length() - 4 ); // remove ".jar"
                        //binary files must be extracted
                        final String internalName = name;
                        try {
                            ResourceUtils.unZipIt(file, libDir, (entryName) -> {
                                if( entryName.startsWith( internalName ) ) {
                                    entryName = entryName.substring( internalName.length() + 1 );
                                }
                                return entryName;
                            });
                            project.getLogger().debug( "Launch4j: Extracted workdir from " + file.getName() );
                        } catch( Exception e ) {
                            project.getLogger().warn( "Launch4j: Failed to extract workdir from " + file.getName() + ": " + e.getMessage() );
                            // Continue without workdir - Launch4j may still work
                        }
                    } else {
                        File target = new File( libDir, file.getName() );
                        Files.copy( file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING );
                        urls.add( target.toURI().toURL() );
                    }
                }
            }
            
            // Warn if workdir was requested but not found (non-critical for cross-platform)
            if( !workdirFound && !OS.isWindows() ) {
                project.getLogger().info( "Launch4j: No workdir package found. Launch4j may work without it on this platform, " +
                    "but if you encounter issues, you may need to install the platform-specific workdir package manually." );
            }
            
            lauch4jClassLoader = new URLClassLoader( urls.toArray( new URL[urls.size()] ), getClass().getClassLoader() );
        }
        return lauch4jClassLoader;
    }

    /**
     * Close the ClassLoader
     *
     * @throws IOException if any error occur
     */
    void close() throws IOException {
        if( lauch4jClassLoader != null ) {
            lauch4jClassLoader.close();
            lauch4jClassLoader = null;
        }
    }
}
