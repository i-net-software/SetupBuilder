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

import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.abstracts.DesktopStarter;
import com.inet.gradle.setup.util.ResourceUtils;

/**
 * Create the Lauch4j programs if there any configured.
 * 
 * @author Volker
 */
public class Launch4jCreator {

    private URLClassLoader lauch4jClassLoader;

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
                dependencies.add( configName, "net.sf.launch4j:launch4j:3.8.0" );
                dependencies.add( configName, "net.sf.launch4j:launch4j:3.8.0:workdir-win32" );
                dependencies.add( configName, "com.thoughtworks.xstream:xstream:1.4.8" );
            }

            ArrayList<URL> urls = new ArrayList<>();
            File libDir = new File( buildDir, "launch4jlib" );
            libDir.mkdirs();
            for( File file : config.getFiles() ) {
                String name = file.getName();
                if( name.endsWith( ".jar" ) ) {
                    //https://github.com/TheBoegl/gradle-launch4j/blob/develop/src/main/groovy/edu/sc/seis/launch4j/Launch4jPlugin.groovy
                    if( name.contains( "-workdir-" ) ) {
                        name = name.substring( 0, name.length() - 4 ); // remove ".jar"
                        //binary files must be extracted
                        final String internalName = name;
            			ResourceUtils.unZipIt(file, libDir, (entryName) -> {
                            if( entryName.startsWith( internalName ) ) {
                                entryName = entryName.substring( internalName.length() + 1 );
                            }
            				return entryName;
        				});
                        
                    } else {
                        File target = new File( libDir, file.getName() );
                        Files.copy( file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING );
                        urls.add( target.toURI().toURL() );
                    }
                }
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
