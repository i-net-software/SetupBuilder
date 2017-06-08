package com.inet.gradle.setup.dmg;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.abstracts.Service;
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
     * @return the file to the created exe.
     * @throws Exception if any error occur
     */
    void create( Service service ) throws Exception {

        String displayName = service.getDisplayName();
        String internalName = displayName.replaceAll( "[^A-Za-z0-9]", "" );
        File prefPaneSource = unpackAndPatchPrefPaneSource( internalName );

        ArrayList<String> command = new ArrayList<String>();
        command.add( "gradle" );
        command.add( "-b" );
        command.add( new File( prefPaneSource, "build.gradle" ).getAbsolutePath() );
        command.add( "xcodebuild" );
        exec( command );

        File prefPaneBinary = new File( prefPaneSource, "build/sym/Release/" + internalName + ".prefPane" );

        if( !prefPaneBinary.exists() ) {
            throw new GradleException( "Launch4j failed. " );
        }

        // Where will the Result be put
        File resourcesOutput = new File( buildDir, displayName + ".app/Contents/Resources" );

        // Rename to app-name to the final prefpane name in the service
        File prefPaneLocation = new File( resourcesOutput, displayName + ".prefPane" );

        // rename helper Tool in binary of the pref pane
        File prefPaneContents = new File( prefPaneBinary, "Contents" );
        Path iconPath = getApplicationIcon().toPath();

        Files.copy( iconPath, new File( prefPaneContents, "Resources/" + internalName + ".app/Contents/Resources/applet.icns" ).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
        Files.move( new File( prefPaneContents, "MacOS/" + internalName ).toPath(), new File( prefPaneContents, "MacOS/" + displayName ).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
        Files.move( new File( prefPaneContents, "Resources/" + internalName + ".app" ).toPath(), new File( prefPaneContents, "Resources/" + displayName + ".app" ).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
        System.out.println( "Unpacked the Preference Pane to: " + prefPaneContents.getAbsolutePath() );

        // Make executable
        setApplicationFilePermissions( new File( prefPaneContents, "Resources/" + displayName + ".app/Contents/MacOS/applet" ) );

        // Rename prefPane
        Files.move( prefPaneBinary.toPath(), prefPaneLocation.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );

        // Copy Icon
        Files.copy( iconPath, new File( prefPaneLocation, "Contents/Resources/ProductIcon.icns" ).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );

        // Patch Info.plist
        File prefPanePLIST = new File( prefPaneLocation, "Contents/Info.plist" );
        setPlistProperty( prefPanePLIST, ":CFBundleIdentifier", (getSetupBuilder().getMainClass() != null ? getSetupBuilder().getMainClass() : getSetupBuilder().getAppIdentifier()) + ".prefPane" );
        setPlistProperty( prefPanePLIST, ":CFBundleName", displayName + " Preference Pane" );
        setPlistProperty( prefPanePLIST, ":CFBundleExecutable", displayName );
        setPlistProperty( prefPanePLIST, ":NSPrefPaneIconLabel", displayName );

        setPlistProperty( prefPanePLIST, ":CFBundleExecutable", displayName );

        File servicePLIST = new File( prefPaneLocation, "Contents/Resources/service.plist" );
        setPlistProperty( servicePLIST, ":Name", displayName );
        setPlistProperty( servicePLIST, ":Label", service.getMainClass() != null ? service.getMainClass() : getSetupBuilder().getAppIdentifier() );

        // Program will be set during installation.
        setPlistProperty( servicePLIST, ":Description", service.getDescription() );
        setPlistProperty( servicePLIST, ":Version", getSetupBuilder().getVersion() );
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

        ResourceUtils.deleteDirectory( prefPaneSource.toPath() );
    }

    /**
     * Download and unpack the preferences pane setup files
     * 
     * @param internalName to unpack it now
     * @return file to prefpane sources
     * @throws IOException if an error occurs
     */
    @SuppressWarnings( "serial" )
    private File unpackAndPatchPrefPaneSource( String internalName ) throws IOException {

        // Create Config and load Dependencies.
        String configName = "prefPaneSource";
        Configuration config = project.getConfigurations().findByName( configName );
        if( config == null ) {
            config = project.getConfigurations().create( configName );
            config.setVisible( false );
            config.setTransitive( false );
            DependencyHandler dependencies = project.getDependencies();
            dependencies.add( configName, "de.inetsoftware:SetupBuilderOSXPrefPane:+:sources" );
        }

        // Prepare output directory
        File outputDir = new File( buildDir, configName );
        outputDir.mkdirs();

        // Unzip the content
        for( File file : config.getFiles() ) {
            ResourceUtils.unZipIt( file, outputDir, ( entryName ) -> {
                return entryName.replaceAll( "SetupBuilderOSXPrefPane", internalName );
            }, ( inputStream ) -> {

                return new ReplacingInputStream( inputStream, new HashMap<byte[], byte[]>() {
                    {
                        put( "SetupBuilderOSXPrefPane".getBytes(), internalName.getBytes() );
                    }
                } );
            } );
        }
        return outputDir;
    }
}
