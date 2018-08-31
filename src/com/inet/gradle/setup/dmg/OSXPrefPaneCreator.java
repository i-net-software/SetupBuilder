package com.inet.gradle.setup.dmg;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.tasks.GradleBuild;

import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.abstracts.Service;
import com.inet.gradle.setup.util.Logging;
import com.inet.gradle.setup.util.ReplacingInputStream;
import com.inet.gradle.setup.util.ResourceUtils;

public class OSXPrefPaneCreator extends AbstractOSXApplicationBuilder<Dmg, SetupBuilder> {

    private File    buildDir;

    private Project project;

    protected OSXPrefPaneCreator( Dmg task, SetupBuilder setup, FileResolver fileResolver ) {
        super( task, setup, fileResolver );
        buildDir = task.getTemporaryDir();
        project = task.getProject();
    }

    /**
     * Create a single Lauch4j launcher.
     *
     * @param launch the launch description
     * @param task the task
     * @param setup the SetupBuilder
     * @return the file to the created pref pane.
     * @throws Exception if any error occur
     */
    void create( Service service ) throws Exception {

        String displayName = service.getDisplayName();
        String internalName = displayName.replaceAll( "[^A-Za-z0-9]", "" );
        File prefPaneSource = unpackAndPatchPrefPaneSource( internalName );

        GradleBuild gradleBuild = project.getTasks().create("OSXPrefPaneBuildTask", GradleBuild.class);
        gradleBuild.setDescription( "Run the xcodebuild task for the prefpane." );
        gradleBuild.setBuildFile( new File( prefPaneSource, "build.gradle" ) );
        gradleBuild.setTasks( Arrays.asList( "clean", "xcodebuild" ) );
        gradleBuild.execute();

        File prefPaneBinary = new File( prefPaneSource, "build/sym/Release/" + internalName + ".prefPane" );

        if( !prefPaneBinary.exists() ) {
            throw new GradleException( "Failed to create the Preferences Pane." );
        }

        // Where will the Result be put
        File resourcesOutput = new File( buildDir, displayName + ".app/Contents/Resources" );

        // Rename to app-name to the final prefpane name in the service
        File prefPaneLocation = new File( resourcesOutput, displayName + ".prefPane" );

        // Rename prefPane
        Files.move( prefPaneBinary.toPath(), prefPaneLocation.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );

        // rename helper Tool in binary of the pref pane
        File prefPaneContents = new File( prefPaneLocation, "Contents" );
        Path iconPath = getApplicationIcon().toPath();

        Files.copy( iconPath, new File( prefPaneContents, "Resources/" + internalName + ".app/Contents/Resources/applet.icns" ).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
        File prefPaneHelper = new File( prefPaneContents, "Resources/" + displayName + " Helper.app" );

        //        Files.move( new File( prefPaneContents, "MacOS/" + internalName ).toPath(), new File( prefPaneContents, "MacOS/" + displayName ).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
        Files.move( new File( prefPaneContents, "Resources/" + internalName + ".app" ).toPath(), prefPaneHelper.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
        Logging.sysout( "Unpacked the Preference Pane to: " + prefPaneContents.getAbsolutePath() );

        // Make applet binary executable
        setApplicationFilePermissions( new File( prefPaneHelper, "Contents/MacOS/applet" ) );

        // Copy Icon
        Files.copy( iconPath, new File( prefPaneLocation, "Contents/Resources/ProductIcon.icns" ).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );

        // Patch Info.plist
        File prefPanePLIST = new File( prefPaneLocation, "Contents/Info.plist" );
        String prefPaneIdentifier = getSetupBuilder().getMainClass() != null ? getSetupBuilder().getMainClass() : getSetupBuilder().getAppIdentifier();
        setPlistProperty( prefPanePLIST, ":CFBundleIdentifier", prefPaneIdentifier + ".prefPane" );
        setPlistProperty( prefPanePLIST, ":CFBundleName", displayName + " Preference Pane" );
        setPlistProperty( prefPanePLIST, ":CFBundleExecutable", internalName );
        setPlistProperty( prefPanePLIST, ":NSPrefPaneIconLabel", displayName + " Helper" ); // Will be used for the sudo app name
        setPlistProperty( prefPanePLIST, ":NSAppleEventsUsageDescription", "Helper application to provide priviledged access to " + displayName );

        File sudoPLIST = new File( prefPaneHelper, "Contents/Info.plist" );
        setPlistProperty( sudoPLIST, ":CFBundleIdentifier", prefPaneIdentifier + ".prefPane.helper" );
        setPlistProperty( sudoPLIST, ":CFBundleName", displayName + " Helper" );
        setPlistProperty( sudoPLIST, ":CFBundleExecutable", "applet" );
        setPlistProperty( sudoPLIST, ":NSAppleEventsUsageDescription", "Helper application to provide priviledged access to " + displayName );

        File servicePLIST = new File( prefPaneLocation, "Contents/Resources/service.plist" );
        setPlistProperty( servicePLIST, ":Name", displayName );
        setPlistProperty( servicePLIST, ":Label", service.getMainClass() != null ? service.getMainClass() : getSetupBuilder().getAppIdentifier() );

        // Program will be set during installation.
        setPlistProperty( servicePLIST, ":Description", service.getDescription() );
        setPlistProperty( servicePLIST, ":Version", task.getVersion() );
        setPlistProperty( servicePLIST, ":KeepAlive", String.valueOf( service.isKeepAlive() ) );
        setPlistProperty( servicePLIST, ":RunAtBoot", String.valueOf( service.isStartOnBoot() ) );
        setPlistProperty( servicePLIST, ":RunAtLoad", "true" );

        if( task.getDaemonUser() != "root" ) {
            // Root by default, will only set if not.
            addPlistProperty( servicePLIST, ":UserName", "String", task.getDaemonUser() );
            addPlistProperty( servicePLIST, ":GroupName", "String", task.getDaemonUser() );
        }

        // Reset the plist.
        deletePlistProperty( servicePLIST, ":starter" );

        // Output the preference link actions to the plist
        for( int i = 0; i < task.getPreferencesLinks().size(); i++ ) {

            if( i == 0 ) {
                addPlistProperty( servicePLIST, ":starter", "array", null );
            }

            PreferencesLink preferencesLink = task.getPreferencesLinks().get( i );
            addPlistProperty( servicePLIST, ":starter:", "dict", null );
            addPlistProperty( servicePLIST, ":starter:" + i + ":title", "string", preferencesLink.getTitle() );
            addPlistProperty( servicePLIST, ":starter:" + i + ":action", "string", preferencesLink.getAction() );
            addPlistProperty( servicePLIST, ":starter:" + i + ":asuser", "string", task.getDaemonUser() );
            addPlistProperty( servicePLIST, ":starter:" + i + ":asroot", "bool", preferencesLink.isRunAsRoot() ? "YES" : "NO" );
        }

        // Keep temporary output directory
        // ResourceUtils.deleteDirectory( prefPaneSource.toPath() );
/*
        // Sign these packages already.
        if( task.getCodeSign() != null ) {
            task.getCodeSign().signApplication( new File( prefPaneLocation, "Contents/Resources/" + displayName + ".app" ) );
            task.getCodeSign().signApplication( prefPaneLocation.getAbsoluteFile() );
        }
*/
    }

    /**
     * Download and unpack the preferences pane setup files
     *
     * @param internalName to unpack it now
     * @return file to prefpane sources
     * @throws Exception in case of errors.
     */
    @SuppressWarnings( "serial" )
    private File unpackAndPatchPrefPaneSource( String internalName ) throws Exception {

        // Create Config and load Dependencies.
        String configName = "prefPaneSource";
        Configuration config = project.getConfigurations().findByName( configName );
        if( config == null ) {
            config = project.getConfigurations().create( configName );
            config.setVisible( false );
            config.setTransitive( false );
            DependencyHandler dependencies = project.getDependencies();
            dependencies.add( configName, "org.openbakery.xcode-plugin:0.15.2" );
        }

        // Prepare output directory
        File outputDir = new File( buildDir, configName );
        outputDir.mkdirs();

        URL dirURL = OSXPrefPaneCreator.class.getClassLoader().getResource( "com/inet/gradle/setup/dmg/preferences/build.gradle" );
        String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file
        File jar = new File(URLDecoder.decode(jarPath, "UTF-8"));

        // Unzip the content
        ResourceUtils.unZipIt( jar, outputDir, "com/inet/gradle/setup/dmg/preferences", ( entryName ) -> {
            return entryName.replaceAll( "SetupBuilderOSXPrefPane", internalName );
        }, ( inputStream ) -> {
            return new ReplacingInputStream( inputStream, new HashMap<byte[], byte[]>() {
                {
                    put( "SetupBuilderOSXPrefPane".getBytes(), internalName.getBytes() );
                }
            } );
        } );
        return outputDir;
    }
}
