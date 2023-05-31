/*
 * Copyright 2015 - 2016 i-net software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.inet.gradle.setup.dmg;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.gradle.api.internal.file.FileResolver;
import org.w3c.dom.Element;

import com.inet.gradle.appbundler.OSXCodeSign;
import com.inet.gradle.appbundler.utils.xmlwise.Plist;
import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.Template;
import com.inet.gradle.setup.abstracts.AbstractBuilder;
import com.inet.gradle.setup.abstracts.DesktopStarter;
import com.inet.gradle.setup.abstracts.LocalizedResource;
import com.inet.gradle.setup.abstracts.Service;
import com.inet.gradle.setup.image.ImageFactory;
import com.inet.gradle.setup.util.TempPath;
import com.inet.gradle.setup.util.XmlFileBuilder;

import com.inet.gradle.setup.util.ResourceUtils;
/**
 * Build a DMG image for OSX.
 * @author Volker Berlin
 */
public class DmgBuilder extends AbstractBuilder<Dmg, SetupBuilder> {

    private String       imageSourceRoot, firstExecutableName;

    private SetupBuilder setup;

    private TempPath     tempPath;

    /**
     * Create a new instance
     * @param dmg the calling task
     * @param setup the shared settings
     * @param fileResolver the file Resolver
     */
    public DmgBuilder( Dmg dmg, SetupBuilder setup, FileResolver fileResolver ) {
        super( dmg, fileResolver );
        this.setup = setup;
    }

    /**
     * Build the dmg file.
     * @throws RuntimeException if any error occur
     */
    public void build() throws RuntimeException {

        task.getProject().getLogger().lifecycle( "\tRunning the build." );
        tempPath = new TempPath( new File( setup.getProject().getBuildDir(), "tmp/SetupBuilder" ).toPath() );

        List<File> filesToPackage = new ArrayList<>();

        try {
            if( setup.getServices().isEmpty() && setup.getDesktopStarters().isEmpty() ) {
                throw new IllegalArgumentException( "No Services or DesktopStarters have been defined. Will stop now." );
            }

            task.getProject().getLogger().lifecycle( "\tPreparing " + task.appBuilders.size() + " services." );

            // Build all services
            for( OSXApplicationBuilder builder : task.appBuilders ) {
                builder.buildService();
                if( firstExecutableName == null ) {
                    firstExecutableName = builder.getService().getDisplayName();
                }
            }

            // Build all standalone applications
            for( DesktopStarter application : setup.getDesktopStarters() ) {
                filesToPackage.add( new OSXApplicationBuilder( task, setup, fileResolver ).buildApplication( application ) );
                if( firstExecutableName == null ) {
                    firstExecutableName = application.getDisplayName();
                }
            }

            imageSourceRoot = buildDir.toString(); // + "/" + setup.getApplication() + ".app";

            // Just in case. If it still has not been set, we do not know what the user intends.
            if( firstExecutableName == null ) {
                firstExecutableName = setup.getApplication();
            }

            if( !setup.getServices().isEmpty() ) {
                // Create installer package
                filesToPackage.add( createPackageFromApp() );
            }

            /*
             * new File ( task.getSetupFile().toString() ).createNewFile();
             * /
             */
            createBinary( filesToPackage );
            //*/
        } catch( RuntimeException ex ) {
            ex.printStackTrace();
            throw ex;
        } catch( Exception ex ) {
            throw new RuntimeException( ex );
        } catch( Throwable ex ) {
            ex.printStackTrace();
        }
    }

    /**
     * Create the binary with native tools.
     * We're using teh dmgbuild project, creating a python file with the settings.
     * 
     * @throws Throwable
     */
    private void createBinary( List<File> filesToPackage ) throws Throwable {

        // Unpack dmgbuild - see https://github.com/dmgbuild/dmgbuild
        // dmgbuild was packed using pyinstaller using:
        // pyinstaller --onefile --target-architecture universal2 dmgbuild

        task.getProject().getLogger().lifecycle( "\tExtracting dmgbuild" );
        File dmgbuild = extractExecutableFile( "template/dmgbuild/dmgbuild", tempPath.get( "dmgbuild" ).toFile() );

        Path settingsFile = tempPath.getTempFile( "dmgbuild", "settings.py" ).toPath();

        // Prepare the python config file
        task.getProject().getLogger().lifecycle( "\tPreparing the configuration" );
        OSXScriptBuilder settings = new OSXScriptBuilder( "template/dmgbuild/settings.00.script.py.txt" );
        settings.addScript( new OSXScriptBuilder( "template/dmgbuild/settings.01.base.py.txt" ) );
        settings.addScript( windowSettings() );
        // The window settings prepare window size which is required in the icon locations
        settings.addScript( iconLocations( !filesToPackage.get( 0 ).getName().endsWith( ".pkg" ) ) );
        settings.addScript( defaultViewSettings() );
        settings.writeTo( settingsFile.toFile() );

        task.getProject().getLogger().debug( "\tSettings" );
        task.getProject().getLogger().debug( settings.toString() );

        ArrayList<String> command = new ArrayList<>();
        command.add( dmgbuild.toString() );
        // Settings
        command.add( "-s" );
        command.add( settingsFile.toString() );

        // Add Icon to command
        File icons = setup.getIconForType( buildDir, "icns" );
        if( icons != null ) {
            command.add( "-D" );
            command.add( "icon=" + icons.getAbsolutePath() );
        }

        command.add( "-D" );
        command.add( "app=" + filesToPackage.get( 0 ).getAbsolutePath() );

        // Name and destination
        command.add( setup.getApplication() );
        command.add( task.getSetupFile().toString() );
        exec( command );

        task.getProject().getLogger().lifecycle( "\tDone with creating the image" );
        codesignFinalImage();
    }

    /**
     * Extract an executable resource and set file permissions accordingly
     * @param source the source, relative to the current class
     * @param destination the destination file
     * @return the actual file it was extracted to
     * @throws IOException in case of errors
     */
    private File extractExecutableFile( String source, File destination ) throws IOException {
        File dmgbuild = ResourceUtils.extract( getClass(), source, destination );
        Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
        perms.add( PosixFilePermission.OWNER_READ );
        perms.add( PosixFilePermission.OWNER_WRITE );
        perms.add( PosixFilePermission.GROUP_READ );
        perms.add( PosixFilePermission.OTHERS_READ );
        perms.add( PosixFilePermission.OWNER_EXECUTE );
        perms.add( PosixFilePermission.GROUP_EXECUTE );
        perms.add( PosixFilePermission.OTHERS_EXECUTE );
        Files.setPosixFilePermissions( dmgbuild.toPath(), perms );
        return dmgbuild;
    }

    /**
     * Create the service files and the pre- and post installer scripts
     * @throws IOException in case of errors
     */
    private void createServiceFiles() throws IOException {

        Service core = new Service( setup );
        core.setId( task.getApplicationIdentifier( setup ) );

        // Create Pre and Post install scripts
        DesktopStarter runAfter = setup.getRunAfter();
        DesktopStarter runBeforeUninstall = setup.getRunBeforeUninstall();

        OSXScriptBuilder preinstall = new OSXScriptBuilder( core, "template/preinstall.txt" );

        preinstall.addScript( new OSXScriptBuilder( task.getPreinst() ) );

        OSXScriptBuilder postinstall = new OSXScriptBuilder( core, "template/postinstall.txt" );
        preinstall.addScript( new OSXScriptBuilder( task.getPostinst() ) );

        OSXScriptBuilder uninstall = new OSXScriptBuilder( core, "template/uninstall.txt" );

        uninstall.addScript( new OSXScriptBuilder( task.getPrerm() ) );
        OSXScriptBuilder watchUninstall = new OSXScriptBuilder( core, "service/watchuninstall.plist" );

        // Set the daemon user, so that it can be created and removed.
        if( task.getDaemonUser() != "root" ) {
            // Create
            String home = "/Library/Application Support/" + setup.getApplication();
            OSXScriptBuilder createUser = new OSXScriptBuilder( core, "template/preinstall.createuser.txt" );

            // Remove
            OSXScriptBuilder removeUser = new OSXScriptBuilder( core, "template/postuninstall.remove.user.txt" );

            // Set the daemonUser on each object
            OSXScriptBuilder[] list = { createUser, removeUser, postinstall };
            Arrays.asList( list ).forEach( item -> {
                item.setPlaceholder( "daemonUser", task.getDaemonUser() );
                item.setPlaceholder( "homeDirectory", home );
            } );

            preinstall.addScript( createUser );
            uninstall.addScript( removeUser );
        }

        for( Service service : setup.getServices() ) {

            if( runBeforeUninstall != null ) {
                runBeforeUninstall.setDisplayName( service.getDisplayName() );
                uninstall.addScript( new OSXScriptBuilder( runBeforeUninstall, "template/runBeforeAfter.txt" ).setPlaceholder( "installationSubdirectory", installationSubdirectory() ).setPlaceholder( "inBackground", "NO" ).setPlaceholder( "startArgument", runBeforeUninstall.getStartArguments() ) );
            }

            preinstall.addScript( new OSXScriptBuilder( service, "template/preinstall.remove-service.txt" ) );
            postinstall.addScript( new OSXScriptBuilder( service, "template/postinstall.install-service.txt" ).setPlaceholder( "installationSubdirectory", installationSubdirectory() ) );

            // Unload service in uninstall as well.
            uninstall.addScript( new OSXScriptBuilder( service, "template/preinstall.remove-service.txt" ) );

            // patch runafter
            if( runAfter != null ) {
                runAfter.setDisplayName( service.getDisplayName() );
                postinstall.addScript( new OSXScriptBuilder( runAfter, "template/runBeforeAfter.txt" ).setPlaceholder( "installationSubdirectory", installationSubdirectory() ).setPlaceholder( "startArgument", runAfter.getStartArguments() ) );
            }
        }

        uninstall.addScript( new OSXScriptBuilder( task.getPostrm() ) );
        uninstall.writeTo( tempPath.getTempFile( "scripts", "uninstall.sh" ) );
        watchUninstall.writeTo( tempPath.getTempFile( "scripts", "watchuninstall.plist" ) );
        preinstall.writeTo( tempPath.getTempFile( "scripts", "preinstall" ) );
        postinstall.writeTo( tempPath.getTempFile( "scripts", "postinstall" ) );
    }

    /**
     * Create a package from the specified app files
     * @throws Throwable in case of errors
     */
    private File createPackageFromApp() throws Throwable {

        createServiceFiles();
        extractApplicationInformation();
        createAndPatchDistributionXML();

        imageSourceRoot = tempPath.get( "distribution" ).toString();
        File resultingPackage = new File( imageSourceRoot, setup.getApplication() + ".pkg" );

        // Build Product for packaging
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/productbuild" );
        command.add( "--distribution" );
        command.add( tempPath.getTempString( "distribution.xml" ) );
        command.add( "--package-path" );
        command.add( tempPath.get( "packages" ).toString() );
        command.add( "--resources" );
        command.add( tempPath.get( "resources" ).toString() );

        // Sign the final package
        command.add( resultingPackage.getAbsolutePath() );
        exec( command );

        if( task.getCodeSign() != null ) {
            task.getCodeSign().signProduct( resultingPackage );
        }

        File packageFile = new File( setup.getDestinationDir(), "/" + setup.getApplication() + ".pkg" );
        Files.copy( resultingPackage.toPath(), packageFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
        setIconForPackage( packageFile );
        return packageFile;
    }

    /**
     * Extract the application information to use for the package builder
     * @throws IOException in case of errors
     */
    private void extractApplicationInformation() throws IOException {
        // Create application information plist
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/pkgbuild" );
        command.add( "--analyze" );
        command.add( "--root" );
        command.add( buildDir.toString() );
        command.add( tempPath.getTempString( setup.getAppIdentifier() + ".plist" ) );
        exec( command );

        // set identifier, create package
        command = new ArrayList<>();
        command.add( "/usr/bin/pkgbuild" );
        command.add( "--root" );
        command.add( buildDir.toString() );
        command.add( "--component-plist" );
        command.add( tempPath.getTempString( setup.getAppIdentifier() + ".plist" ) );
        command.add( "--identifier" );
        command.add( task.getApplicationIdentifier( setup ) );
        command.add( "--version" );
        command.add( task.getVersion() );
        command.add( "--scripts" );
        command.add( tempPath.get( "scripts" ).toString() );
        command.add( "--install-location" );

        // Application as default directory except there are more application parts to install.
        command.add( "/Applications/" + installationSubdirectory() );
        command.add( tempPath.getTempString( "packages", setup.getApplication() + ".pkg" ) );
        exec( command );

        Files.copy( tempPath.getTempFile( "packages", setup.getApplication() + ".pkg" ).toPath(), new File( setup.getDestinationDir(), "/" + setup.getAppIdentifier() + ".pkgbuild.pkg" ).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
    }

    /**
     * Returns a sub directory if needed because of the installation
     * @return sub directory or ""
     */
    private String installationSubdirectory() {
        return(setup.getServices().size() + setup.getDesktopStarters().size() > 1 ? setup.getApplication() + "/" : "");
    }

    /**
     * Create and patch the ditribution xml file that defines the package
     * @throws Throwable in case of error
     */
    private void createAndPatchDistributionXML() throws Throwable {
        File productFile = tempPath.getTempFile( "product.plist" );
        Map<String, Object> map = new HashMap<>();
        map.put( "arch", task.getArchitecture() );
        Plist.store( map, productFile );

        ArrayList<String> command;
        // Synthesize Distribution xml
        command = new ArrayList<>();
        command.add( "/usr/bin/productbuild" );
        command.add( "--synthesize" );
        command.add( "--product" );
        command.add( productFile.toString() );
        command.add( "--package" );
        command.add( tempPath.getTempFile( "packages", setup.getApplication() + ".pkg" ).toString() );
        command.add( tempPath.getTempFile( "distribution.xml" ).toString() );
        exec( command );

        patchDistributionXML();
    }

    /**
     * Patch the distribution file with custom settings
     * @throws Throwable in case of errors
     */
    private void patchDistributionXML() throws Throwable {

        File xml = tempPath.getTempFile( "distribution.xml" );
        URL url = xml.toURI().toURL();
        @SuppressWarnings( "rawtypes" )
        XmlFileBuilder xmlFile = new XmlFileBuilder<Dmg>( task, setup, xml, buildDir, url );

        Element distribution = (Element)xmlFile.doc.getFirstChild();
        if( !"installer-gui-script".equals( distribution.getTagName() ) ) {
            throw new IllegalArgumentException( "Template does not contain an installer-gui-script root: " + distribution.getTagName() );
        }

        // The title of the installer
        Element title = xmlFile.getOrCreateChild( distribution, "title" );
        xmlFile.addNodeText( title, setup.getApplication() );

        // Background Image node
        setBackgroundImage( distribution, xmlFile, false, task.getSetupBackgroundImage() );
        setBackgroundImage( distribution, xmlFile, true, task.getSetupDarkBackgroundImage() );

        // Welcome Node
        List<LocalizedResource> welcomePages = task.getWelcomePages();
        for( LocalizedResource localizedResource : welcomePages ) {
            File welcomePage = checkSetupTextFile( localizedResource.getResource() );
            if( welcomePage != null ) {
                Files.copy( welcomePage.toPath(), tempPath.getTempFile( "resources/" + localizedResource.getLanguage() + ".lproj", "Welcome" ).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
                Element license = xmlFile.getOrCreateChild( distribution, "welcome", false );
                xmlFile.addAttributeIfNotExists( license, "file", "Welcome" );
            }
        }

        // License node
        List<LocalizedResource> licenseFiles = setup.getLicenseFiles();
        for( LocalizedResource localizedResource : licenseFiles ) {
            File licenseFile = checkSetupTextFile( localizedResource.getResource() );
            if( licenseFile != null ) {
                Files.copy( licenseFile.toPath(), tempPath.getTempFile( "resources/" + localizedResource.getLanguage() + ".lproj", "License" ).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
                Element license = xmlFile.getOrCreateChild( distribution, "license", false );
                xmlFile.addAttributeIfNotExists( license, "file", "License" );
            }
        }

        // Conclusion Node
        List<LocalizedResource> conclusionPages = task.getConclusionPages();
        for( LocalizedResource localizedResource : conclusionPages ) {
            File welcomePage = checkSetupTextFile( localizedResource.getResource() );
            if( welcomePage != null ) {
                Files.copy( welcomePage.toPath(), tempPath.getTempFile( "resources/" + localizedResource.getLocale().getLanguage() + ".lproj", "Conclusion" ).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
                Element license = xmlFile.getOrCreateChild( distribution, "conclusion", false );
                xmlFile.addAttributeIfNotExists( license, "file", "Conclusion" );
            }
        }
        xmlFile.save();
    }

    /**
     * Set a background image for the package installer
     * @param distribution the root element
     * @param xmlFile the current xml file builder
     * @param isDark true, if this is the dark mode
     * @param backgroundImage the image to set
     * @throws IOException in case the File operation failed
     */
    private void setBackgroundImage( Element distribution, XmlFileBuilder<?> xmlFile, boolean isDark, File backgroundImage ) throws IOException {
        if( backgroundImage != null ) {
            Files.copy( backgroundImage.toPath(), tempPath.getTempFile( "resources", backgroundImage.getName() ).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
            Element background = xmlFile.getOrCreateChild( distribution, "background-darkAqua", false );
            xmlFile.addAttributeIfNotExists( background, "file", backgroundImage.getName() );
            xmlFile.addAttributeIfNotExists( background, "alignment", "left" );
            xmlFile.addAttributeIfNotExists( background, "proportional", "left" );
        }
    }

    /**
     * Check a file for the correct setup text-resource type
     * @param file to check
     * @return file if ok, or null
     */
    private File checkSetupTextFile( File file ) {

        if( file != null ) {

            String name = file.getName();
            for( String format : new String[] { "txt", "rtf", "rtfd", "html" } ) {
                if( name.toLowerCase().endsWith( "." + format ) ) {
                    return file;
                }
            }
            System.err.println( "The provided file must be of type: txt, rtf, rtfd or html. File was: " + name );
        }

        return null;
    }

    /**
     * Prepares the default view settings
     * @return the python script snippet to add
     * @throws Throwable in case of errors
     */
    private OSXScriptBuilder defaultViewSettings() throws Throwable {
        OSXScriptBuilder settings = new OSXScriptBuilder( "template/dmgbuild/settings.04.defaultview.py.txt" );

        settings.setPlaceholder( "iconSize", "" + task.getIconSize() );
        settings.setPlaceholder( "fontSize", "" + task.getFontSize() );

        return settings;
    }

    /**
     * Prepares the window settings, including size and background
     * @return the python script snippet to add
     * @throws Throwable in case of errors
     */
    private OSXScriptBuilder windowSettings() throws Throwable {

        OSXScriptBuilder settings = new OSXScriptBuilder( "template/dmgbuild/settings.03.window.py.txt" );

        String background = task.getBackgroundColor();
        File backgroundFile = prepareBackgroundImage();
        if ( backgroundFile != null ) {
            background = backgroundFile.getAbsolutePath();
        }

        settings.setPlaceholder( "background", background );
        settings.setPlaceholder( "windowWidth", "" + task.getWindowWidth() );
        settings.setPlaceholder( "windowHeight", "" + task.getWindowHeight() );

        return settings;
    }

    /**
     * Prepares the icon positions and add symlink to applications
     * @return the python script snippet to add
     * @throws Throwable in case of errors
     */
    private OSXScriptBuilder iconLocations( boolean requiresApplicationsLink ) throws Throwable {
        OSXScriptBuilder settings = new OSXScriptBuilder( "template/dmgbuild/settings.02.icons.py.txt" );

        int windowWidth = task.getWindowWidth();
        int windowHeight = task.getWindowHeight();
        int iconSize = task.getIconSize();

        int ourXPos = (windowWidth - 2 * iconSize) / 3 + iconSize / 3;
        int appXPos = windowWidth - ourXPos;
        int yPos = (windowHeight - iconSize) / 3 * 2;

        String appLocation = "";
        if ( requiresApplicationsLink ) {
            // Only set the symlinks for applications if required.
            settings.setPlaceholder( "symlinks", "\"Applications\": \"/Applications\"" );

            appLocation += ", \"Applications\": (" + appXPos + ", " + yPos + ")";
        } else {

            ourXPos = windowWidth / 2;
            settings.setPlaceholder( "symlinks", "" );
        }

        settings.setPlaceholder( "iconLocations", "appname: (" + ourXPos + ", " + yPos + ")" + appLocation );

        return settings;
    }

    /**
     * Prepares the background image and update window sizes
     * @return the file, if set, for the background image
     * @throws IOException in case of errors
     */
    private File prepareBackgroundImage() throws IOException {

        File background = task.getBackgroundImage();

        if( background != null ) {
            BufferedImage image = ImageIO.read( task.getBackgroundImage() );

            // The image may be a non-standard one which we do not have Java access to.
            // Still it may work later on. e.g. tiffs are not directly supported in Java 8 and older
            if( image != null ) {
                // Override the values to use the actual image size
                task.setWindowWidth( image.getWidth() );
                task.setWindowHeight( image.getHeight() );
            }
        }

        return background;
    }

    /**
     * run a Script for the Package.
     * @throws IOException exception
     */
    private void setIconForPackage( File packageFile ) throws IOException {

        task.getProject().getLogger().lifecycle( "\tExtracting setIcon.sh" );
        File setIcon = extractExecutableFile( "template/dmgbuild/setIcon.sh", tempPath.get( "setIcon.sh" ).toFile() );

        ArrayList<String> command = new ArrayList<>();
        command.add( setIcon.getAbsolutePath() );
        command.add( ImageFactory.getImageFile( task.getProject(), task.getSetupIcon(), buildDir, "icns" ).getAbsolutePath() );
        command.add( packageFile.getAbsolutePath() );

        task.getProject().getLogger().lifecycle( "\tSetting display options for package." );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exec( true, baos, command.toArray( new String[command.size()] ) );
        task.getProject().getLogger().lifecycle( "\tDone Setting DMG display options for package. Ignoring errors if there were any" );
        task.getProject().getLogger().lifecycle( "\t" + baos.toString() );
    }

    /**
     * Run the code signing if applicable
     */
    private void codesignFinalImage() {
        OSXCodeSign<Dmg, SetupBuilder> codeSign = task.getCodeSign();
        if( codeSign != null ) {
            File finalImage = new File( task.getSetupFile().toString() );
            codeSign.signApplication( finalImage );
            codeSign.notarize( finalImage );
        }
    }
}
