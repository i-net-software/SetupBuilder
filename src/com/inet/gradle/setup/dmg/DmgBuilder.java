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
package com.inet.gradle.setup.dmg;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.gradle.api.internal.file.FileResolver;
import org.w3c.dom.Element;

import com.inet.gradle.setup.AbstractBuilder;
import com.inet.gradle.setup.DesktopStarter;
import com.inet.gradle.setup.Service;
import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.Template;
import com.inet.gradle.setup.util.XmlFileBuilder;

/**
 * Build a DMG image for OSX.
 * 
 * @author Volker Berlin
 */
public class DmgBuilder extends AbstractBuilder<Dmg> {

    private String title, applicationName, imageSourceRoot;
	private File iconFile;

    /**
     * Create a new instance
     * 
     * @param dmg the calling task
     * @param setup the shared settings
     * @param fileResolver the file Resolver
     */
    public DmgBuilder( Dmg dmg, SetupBuilder setup, FileResolver fileResolver ) {
        super( dmg, setup, fileResolver );
    }

    /**
     * Build the dmg file. 
     * @throws RuntimeException if any error occur 
     */
    public void build() throws RuntimeException {

        try {
        	OSXApplicationBuilder applicationBuilder = new OSXApplicationBuilder( task, setup, fileResolver );
        	
        	// Build all services 
        	for (Service service : setup.getServices() ) {
        		applicationBuilder.buildService( service );
			}
        	
        	// Build all standalone applications
        	for (DesktopStarter application : setup.getDesktopStarters() ) {
        		applicationBuilder.buildApplication( application );
			}

            title = setup.getArchiveName();
            applicationName = setup.getAppIdentifier();
            imageSourceRoot = buildDir.toString() + "/" + setup.getApplication() + ".app";
            iconFile = applicationBuilder.getApplicationIcon();

        	if ( !setup.getServices().isEmpty() ) {
        		// Create installer package
        		createPackageFromApp();
        	}

            createBinary();
        
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
     * @throws Throwable 
     */
    private void createBinary() throws Throwable {
    	
        createTempImage();
        attach();

        setVolumeIcon();
        applescript();

        detach();
        finalImage();
        
        new File( setup.getDestinationDir(), "pack.temp.dmg" ).delete();
    }
    
    private void createServiceFiles( ) throws IOException {
    	
    	// Create Pre and Post install scripts
    	DesktopStarter runAfter = setup.getRunAfter();
		OSXScriptBuilder preinstall = new OSXScriptBuilder( "template/preinstall.txt" );
		OSXScriptBuilder postinstall = new OSXScriptBuilder( "template/postinstall.txt" );
		for (Service service : setup.getServices() ) {
			
			preinstall.addScript(new OSXScriptBuilder(service, "template/preinstall.remove-service.txt" ));
			postinstall.addScript(new OSXScriptBuilder(service, "template/postinstall.install-service.txt" ));
			
			// patch runafter
			if ( runAfter != null ) {
				service.setDisplayName(service.getDisplayName());
				postinstall.addScript(new OSXScriptBuilder(runAfter, "template/postinstall.runafter.txt" ));
			}
		}

    	preinstall.writeTo( TempPath.getTempFile("scripts", "preinstall"));
    	postinstall.writeTo( TempPath.getTempFile("scripts", "postinstall"));
    }
    

    private void createPackageFromApp() throws Throwable {
		
    	createServiceFiles();
        extractApplicationInformation();
        createAndPatchDistributionXML();
    	
        imageSourceRoot = TempPath.get( "distribution" ).toString();

		// Build Product for packaging
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/productbuild" );
        command.add( "--distribution" );
        command.add( TempPath.getTempString( "distribution.xml" ) );
        command.add( "--package-path" );
        command.add( TempPath.get( "packages" ).toString() );
        command.add( imageSourceRoot + "/" + applicationName + ".pkg" );
    	exec( command );
    	
    	Files.copy( new File(imageSourceRoot + "/" + applicationName + ".pkg").toPath() , new File(setup.getDestinationDir(), "/" + applicationName + ".pkg").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
    }

	private void extractApplicationInformation() throws IOException {
		// Create application information plist
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/pkgbuild" );
        command.add( "--analyze" );
        command.add( "--root" );
        command.add( buildDir.toString() );
        command.add( TempPath.getTempString( applicationName + ".plist" ) );
    	exec( command );
    	
    	// set identifier, create package
        command = new ArrayList<>();
        command.add( "/usr/bin/pkgbuild" );
        command.add( "--root" );
        command.add( buildDir.toString() );
        command.add( "--component-plist" );
        command.add( TempPath.getTempString( applicationName + ".plist" ) );
        command.add( "--identifier" );
        command.add( setup.getMainClass() != null ? setup.getMainClass() : setup.getAppIdentifier() );
        command.add( "--version" );
        command.add( setup.getVersion() );
        command.add( "--scripts" );
        command.add( TempPath.get( "scripts" ).toString() );
        command.add( "--install-location" );
        command.add( "/Library/" + setup.getApplication() + "/" );
        command.add(  TempPath.getTempString( "packages", applicationName + ".pkg" ) );
    	exec( command );
    	
    	Files.copy( TempPath.getTempFile( "packages", applicationName + ".pkg" ).toPath() , new File(setup.getDestinationDir(), "/" + applicationName + ".pkgbuild.pkg").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
	}

	private void createAndPatchDistributionXML() throws Throwable {
		ArrayList<String> command;
		// Synthesize Distribution xml
        command = new ArrayList<>();
        command.add( "/usr/bin/productbuild" );
        command.add( "--synthesize" );
        command.add( "--package" );
        command.add( TempPath.getTempFile( "packages", applicationName + ".pkg").toString() );
        command.add( TempPath.getTempFile( "distribution.xml" ).toString() );
    	exec( command );

        patchDistributionXML();
	}

	private void patchDistributionXML() throws Throwable {

        File xml = TempPath.getTempFile( "distribution.xml" );
        URL url = xml.toURI().toURL();
		@SuppressWarnings("rawtypes")
		XmlFileBuilder xmlFile = new XmlFileBuilder<Dmg>(task, setup, xml, buildDir, url);
        
        Element distribution = (Element)xmlFile.doc.getFirstChild();
        if( !"installer-gui-script".equals( distribution.getTagName() ) ) {
            throw new IllegalArgumentException( "Template does not contains a installer-gui-script root: " + distribution.getTagName() );
        }

        // Product node
        Element background = xmlFile.getOrCreateChildById( distribution, "background", "*", false );
        xmlFile.addAttributeIfNotExists( background, "file", "background.png" );
        xmlFile.addAttributeIfNotExists( background, "alignment", "left" );
        xmlFile.addAttributeIfNotExists( background, "proportional", "left" );

        // Welcome Node
        Element welcome = xmlFile.getOrCreateChildById( distribution, "welcome", "*", false );
        xmlFile.addAttributeIfNotExists( welcome, "file", "welcome.rtf" );

        // License node
        Element license = xmlFile.getOrCreateChildById( distribution, "license", "*", false );
        xmlFile.addAttributeIfNotExists( license, "file", "license.txt" );
    
        xmlFile.save();
	}

	/**
     * Call hdiutil to create a temporary image.
     */
    private void createTempImage() {
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/hdiutil" );
        command.add( "create" );
        command.add( "-srcfolder" );
		command.add( imageSourceRoot );
        command.add( "-format" );
        command.add( "UDRW" );
        command.add( "-volname" );
        command.add( title );
        command.add( setup.getDestinationDir() + "/pack.temp.dmg" );
        exec( command );
    }

    /**
     * Call hdiutil to mount temporary image
     * 
     * @throws UnsupportedEncodingException
     */
    private void attach() throws IOException {
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/hdiutil" );
        command.add( "attach" );
        command.add( "-readwrite" );
        command.add( "-noverify" );
        command.add( "-noautoopen" );
        command.add( setup.getDestinationDir() + "/pack.temp.dmg" );
        command.add( "-mountroot" );
        command.add( TempPath.get() );

        exec( command );
    }

    /**
     * Call hdiutil to detach temporary image
     */
    private void detach() {
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/hdiutil" );
        command.add( "detach" );
        command.add( TempPath.get() + "/" + title );
        exec( command );
    }

    /**
     * Call SetFile to set the volume icon.
     * @throws IOException IOException
     */
    private void setVolumeIcon() throws IOException {
        
    	// Copy Icon as file icon into attached container 
        File iconDestination = TempPath.getTempFile(title, ".VolumeIcon.icns" );
		Files.copy( iconFile.toPath(), iconDestination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
    	
    	ArrayList<String> command = new ArrayList<>();
        command.add( "SetFile" );
        command.add( "-a" );
        command.add( "C" );
        command.add( iconDestination.getParent() );
        exec( command, null, null, true );
        
        if ( task.getBackgroundImage() != null ) {
        	String name = task.getBackgroundImage().getName();
            File backgroundDestination = TempPath.getTempFile(title, "/.resources/background" + name.substring(name.lastIndexOf('.')) );
            Files.createDirectories(backgroundDestination.getParentFile().toPath(), new FileAttribute[0]);
        	Files.copy(task.getBackgroundImage().toPath(), backgroundDestination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
        	BufferedImage image = ImageIO.read( backgroundDestination );
        	
        	// Override the values to use the acutal image size
        	task.setWindowWidth(image.getWidth());
        	task.setWindowHeight(image.getHeight());
        }
    }

    private void applescript() throws IOException {
    	
    	Template applescript = new Template( "dmg/template/applescript.txt" );
    	applescript.setPlaceholder("displayName", setup.getApplication() );
    	applescript.setPlaceholder("executable", setup.getApplication() );

    	applescript.setPlaceholder("windowWidth", task.getWindowWidth().toString() );
    	applescript.setPlaceholder("windowHeight", task.getWindowHeight().toString() );
    	applescript.setPlaceholder("iconSize",  task.getIconSize().toString() );
    	applescript.setPlaceholder("fontSize", task.getFontSize().toString() );
    	
        if ( task.getBackgroundImage() != null ) {
        	String name = task.getBackgroundImage().getName();
        	applescript.setPlaceholder("backgroundExt", name.substring(name.lastIndexOf('.')) );
        }

        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/osascript" );
        exec( command, new ByteArrayInputStream( applescript.toString().getBytes( StandardCharsets.UTF_8 ) ), null, true );
    }

    /**
     * convert to final image
     */
    private void finalImage() {
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/hdiutil" );
        command.add( "convert" );
        command.add( setup.getDestinationDir() + "/pack.temp.dmg" );
        command.add( "-format" );
        command.add( "UDZO" );
        command.add( "-imagekey" );
        command.add( "zlib-level=9" );
        command.add( "-o" );
        command.add( task.getSetupFile().toString() );
        exec( command );
    }
}
