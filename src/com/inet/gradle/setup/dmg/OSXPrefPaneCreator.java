package com.inet.gradle.setup.dmg;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;

import com.inet.gradle.setup.Service;
import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.util.ResourceUtils;

import aQute.bnd.osgi.Clazz;

public class OSXPrefPaneCreator {

    private URLClassLoader xcodeClassLoader;

    /**
     * Create a single Lauch4j launcher.
     * 
     * @param launch the launch description
     * @param task the task
     * @param setup the SetupBuilder
     * @return the file to the created exe.
     * @throws Exception if any error occur
     */
    File create( Service service, Dmg task, SetupBuilder setup ) throws Exception {
    	
    	// Download dependencies + Classloader
    	File prefPaneSource = unpackAndPatchPrefPaneSource(task, service);
    	File outfile = new File( "asd" );
    	
    	Project project = task.getProject();
    	project.setProperty("xcodebuild.objRoot", prefPaneSource);
    	
		Class<?> clazz = Class.forName( "org.openbakery.XcodePlugin", true, getClassLoader( task.getProject(), task.getTemporaryDir() ) );
    	Object xcodePlugin = clazz.getConstructor().newInstance();
        clazz.getMethod( "apply", Project.class ).invoke( xcodePlugin, project );

        return outfile;
    }

    /**
     * Download and unpack the preferences pane setup files
     * @param project current project
     * @param buildDir the build directory
     * @return file to prefpane sources
     * @throws IOException if an error occurs
     */
    private File unpackAndPatchPrefPaneSource( Dmg task, Service service ) throws IOException {
    	
    	// Create Config and load Dependencies.
    	String configName = "prefPaneSource";
    	Project project = task.getProject();
    	File buildDir = task.getTemporaryDir();
    	
    	Configuration config = project.getConfigurations().findByName( configName );
    	if ( config == null ) {
    		config = project.getConfigurations().create( configName );
            config.setVisible( false );
            config.setTransitive( false );
            DependencyHandler dependencies = project.getDependencies();
            dependencies.add( configName, "de.inetsoftware:SetupBuilderOSXPrefPane:+:sources" );
    	}
    	
    	// Prepare output directory
    	File outputDir = new File( buildDir, configName );
    	outputDir.mkdirs();
    	
    	String internalName = service.getDisplayName().replaceAll("[^A-Za-z0-9]", "");
    	
    	// Unzip the content
    	for( File file : config.getFiles() ) {
			ResourceUtils.unZipIt(file, outputDir, (entryName) -> {
				return entryName.replace("SetupBuilderOSXPrefPane", internalName);
			});
    	}
    	return outputDir;
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
        if( xcodeClassLoader == null ) {
            String configName = "setupXCode";

            // add a repository
            RepositoryHandler repositories = project.getRepositories();
			repositories.add( repositories.maven(mavenArtifactRepository -> {
			    mavenArtifactRepository.setName( configName );
			    mavenArtifactRepository.setUrl( "http://repository.openbakery.org/" );
			}));
			
            Configuration config = project.getConfigurations().findByName( configName );
            
            // Add the plugin
            if( config == null ) {
                config = project.getConfigurations().create( configName );
                config.setVisible( false );
                config.setTransitive( false );
                DependencyHandler dependencies = project.getDependencies();
                dependencies.add( configName, "org.openbakery:xcode-plugin:0.13.+" );
            }

            ArrayList<URL> urls = new ArrayList<>();
            File libDir = new File( buildDir, configName );
            libDir.mkdirs();
            for( File file : config.getFiles() ) {
                String name = file.getName();
                if( name.endsWith( ".jar" ) ) {
                    //https://github.com/TheBoegl/gradle-launch4j/blob/develop/src/main/groovy/edu/sc/seis/launch4j/Launch4jPlugin.groovy
                    File target = new File( libDir, file.getName() );
                    Files.copy( file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING );
                    urls.add( target.toURI().toURL() );
                }
            }
            xcodeClassLoader = new URLClassLoader( urls.toArray( new URL[urls.size()] ), getClass().getClassLoader() );
        }
        return xcodeClassLoader;
    }
    
    /**
     * Close the ClassLoader
     * 
     * @throws IOException if any error occur
     */
    void close() throws IOException {
        if( xcodeClassLoader != null ) {
            xcodeClassLoader.close();
            xcodeClassLoader = null;
        }
    }
}
