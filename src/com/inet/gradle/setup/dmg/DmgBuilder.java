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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.tools.ant.types.FileSet;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.internal.file.FileResolver;
import org.w3c.dom.Element;

import com.inet.gradle.setup.AbstractBuilder;
import com.inet.gradle.setup.DocumentType;
import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.image.ImageFactory;
import com.inet.gradle.setup.util.XmlFileBuilder;
import com.oracle.appbundler.AppBundlerTask;
import com.oracle.appbundler.Architecture;
import com.oracle.appbundler.BundleDocument;

/**
 * Build a DMG image for OSX.
 * 
 * @author Volker Berlin
 */
public class DmgBuilder extends AbstractBuilder<Dmg> {

    private String title, applicationName;
	private Path tmp;
	private File iconFile;
	private String imageSourceRoot;

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
     * @throws Throwable 
     */
    public void build() throws RuntimeException {

        try {
        	Project project = task.getProject();

        	tmp = Files.createTempDirectory("SetupBuilder", new FileAttribute[0]);
            title = setup.getSetupName();
            applicationName = setup.getBaseName();
            imageSourceRoot = buildDir.toString() + "/" + applicationName + ".app";

            AppBundlerTask appBundler = new AppBundlerTask();
            appBundler.setOutputDirectory( buildDir );
            appBundler.setName( applicationName );
            appBundler.setDisplayName( setup.getApplication() );

            String version = setup.getVersion();
            appBundler.setVersion( version );
            int idx = version.indexOf( '.' );
            if( idx >= 0 ) {
                idx = version.indexOf( '.', idx + 1 );
                if( idx >= 0 ) {
                    version = version.substring( 0, idx );
                }
            }
            appBundler.setShortVersion( version );

            appBundler.setExecutableName( setup.getBaseName() );
            appBundler.setIdentifier( setup.getMainClass() );
            appBundler.setMainClassName( setup.getMainClass() );
            appBundler.setJarLauncherName( setup.getMainJar() );
            appBundler.setCopyright( setup.getVendor() );

            Object iconData = setup.getIcons();
            iconFile = ImageFactory.getImageFile( project, iconData, buildDir, "icns" );
            appBundler.setIcon( iconFile );
            Architecture x86_64 = new Architecture();
            x86_64.setName( "x86_64" );
            appBundler.addConfiguredArch( x86_64 );

            // add file extensions
            for( DocumentType doc : setup.getDocumentType() ) {
                BundleDocument bundle = new BundleDocument();
                bundle.setExtensions( String.join( ",", doc.getFileExtension() ) );
                bundle.setName( doc.getName() );
                bundle.setRole( doc.getRole() ); //Viewer or Editor
                Object icons = doc.getIcons();
                if( icons == null ) {
                    // nothing
                } else if( icons == iconData ) {
                    bundle.setIcon( iconFile.toString() );
                } else {
                    bundle.setIcon( ImageFactory.getImageFile( project, icons, buildDir, "icns" ).toString() );
                }
                appBundler.addConfiguredBundleDocument( bundle );
            }

            bundleJre( appBundler );

            org.apache.tools.ant.Project antProject = new org.apache.tools.ant.Project();
            appBundler.setProject( antProject );
            appBundler.execute();

            task.copyTo( new File( buildDir, applicationName + ".app/Contents/Java" ) );
            
            setApplicationFilePermissions();
            
            createBinary();

/*
            // Remove temporary folder and content.
            Files.walkFileTree(tmp, new SimpleFileVisitor<Path>() {
         	   @Override
         	   public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
         		   Files.delete(file);
         		   return FileVisitResult.CONTINUE;
         	   }

         	   @Override
         	   public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
         		   Files.delete(dir);
         		   return FileVisitResult.CONTINUE;
         	   }

            });
/*/
//*/
            tmp = null;
        
        } catch( RuntimeException ex ) {
            ex.printStackTrace();
            throw ex;
        } catch( Exception ex ) {
            throw new RuntimeException( ex );
	    } catch( Throwable ex ) {
	        ex.printStackTrace();
        }
    }

	private void setApplicationFilePermissions() throws IOException {
		
		String root = buildDir.toString() + "/" + applicationName + ".app";

		// Set Read on all files and folders
		ArrayList<String> command = new ArrayList<>();
        command.add( "chmod" );
        command.add( "-R" );
        command.add( "a+r" );
		command.add( root );
        exec( command );
        
		// Set execute on all folders.
        command = new ArrayList<>();
        command.add( "find" );
        command.add( root );
        command.add( "-type" );
        command.add( "d" );
        command.add( "-exec" );
        command.add( "chmod" );
        command.add( "a+x" );
        command.add( "{}" );
        command.add( ";" );
        exec( command );
	}

    /**
     * Bundle the Java VM if set.
     * @param appBundler the ANT Task
     */
    private void bundleJre( AppBundlerTask appBundler ) {
        Object jre = setup.getBundleJre();
        if( jre == null ) {
            return;
        }
        File jreDir;
        try {
            jreDir = task.getProject().file( jre );
        } catch( Exception e ) {
            jreDir = null;
        }
        if( jreDir == null || !jreDir.isDirectory() ) {
            ArrayList<String> command = new ArrayList<>();
            command.add( "/usr/libexec/java_home" );
            command.add( "-v" );
            command.add( jre.toString() );
            command.add( "-F" );
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            exec( command, null, baos );
            jreDir = new File( baos.toString().trim() );
            if( !jreDir.isDirectory() ) {
                throw new GradleException( "bundleJre version " + jre + " can not be found in: " + jreDir );
            }
        }
        task.getProject().getLogger().lifecycle( "\tbundle JRE: " + jreDir );
        FileSet fileSet = new FileSet();
        fileSet.setDir( jreDir );
        
        fileSet.appendIncludes(new String[] {
                "jre/*",
                "jre/lib/",
                "jre/bin/java"
        });

        fileSet.appendExcludes(new String[] {
            "jre/lib/deploy/",
            "jre/lib/deploy.jar",
            "jre/lib/javaws.jar",
            "jre/lib/libdeploy.dylib",
            "jre/lib/libnpjp2.dylib",
            "jre/lib/plugin.jar",
            "jre/lib/security/javaws.policy"
        });

        appBundler.addConfiguredRuntime( fileSet );
    }

    /**
     * Create the binary with native tools.
     * @throws Throwable 
     */
    private void createBinary() throws Throwable {
    	
    	if ( !setup.getServices().isEmpty() ) {
    		createPackageFromApp();
    	}
    	
        createTempImage();
        attach();

        setVolumeIcon();
        applescript();

        detach();
        finalImage();
        
        new File( setup.getDestinationDir(), "pack.temp.dmg" ).delete();
    }
    
    private void patchServiceFiles( String resource, File destination ) {
    	
    	try {
        	String launchdScriptName = setup.getMainClass();
            InputStream input = getClass().getResourceAsStream( resource );
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            copyData(input, bos);
            input.close();

            String script = new String( bos.toByteArray(), StandardCharsets.UTF_8 );
            script = script.replace( "${serviceName}", launchdScriptName );
            script = script.replace( "${applicationName}", applicationName );
            script = script.replace( "${programName}", "/Library/" + applicationName + "/" + applicationName + ".app/Contents/MacOS/" + setup.getBaseName() );
            script = script.replace( "${installName}", "/Library/" + applicationName + "/" + applicationName + ".app" );
            
            OutputStream output = new FileOutputStream( destination );
            output.write( script.getBytes() );
            output.close();

    		// Set Read Execute on Destination File
    		ArrayList<String> command = new ArrayList<>();
            command.add( "chmod" );
            command.add( "a+rx" );
    		command.add( destination.toString() );
            exec( command );
            
    	} catch (Throwable e) {
    		System.err.println("Error while patching " + resource);
    		e.printStackTrace();
    	}
    }
    
    /**
     * Copy the data from the input to the output
     * @param input the input data
     * @param output the target
     * @throws IOException if IO issues
     */
    public static void copyData( InputStream input, OutputStream output ) throws IOException{
        byte[] buf = new byte[4096];
        int len;
        while ((len = input.read(buf)) > 0) {
            output.write(buf, 0, len);
        }
    }
	
	/**
	 * * Unpack the preference pane from the SetupBuilder,
	 * * Modify the icon and text
	 * * Put into the main application bundle
	 * 
	 * @throws Throwable
	 */
	private void createPreferencePane() throws Throwable {

		// Unpack
		File setPrefpane = new File(tmp.toFile(), "packages/SetupBuilderOSXPrefPane.prefPane.zip");

		InputStream input = getClass().getResourceAsStream("service/SetupBuilderOSXPrefPane.prefPane.zip");
		FileOutputStream output = new FileOutputStream( setPrefpane );
        copyData(input, output);
        input.close();
        output.close();

        File resourcesOutput = new File(buildDir, applicationName + ".app/Contents/Resources");
		unZipIt(setPrefpane, resourcesOutput);

		// Rename to app-name
        File prefPaneLocation = new File(resourcesOutput, applicationName + ".prefPane");

        // rename helper Tool
        File prefPaneContents = new File(resourcesOutput, "SetupBuilderOSXPrefPane.prefPane/Contents");
		Files.copy(iconFile.toPath(), new File(prefPaneContents, "Resources/SetupBuilderOSXPrefPane.app/Contents/Resources/applet.icns").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		Files.move(new File(prefPaneContents, "MacOS/SetupBuilderOSXPrefPane").toPath(), new File( prefPaneContents, "MacOS/"+ applicationName ).toPath(),  java.nio.file.StandardCopyOption.REPLACE_EXISTING );
		Files.move(new File(prefPaneContents, "Resources/SetupBuilderOSXPrefPane.app").toPath(), new File(prefPaneContents, "Resources/"+ applicationName +".app").toPath(),  java.nio.file.StandardCopyOption.REPLACE_EXISTING );

		// Make executable
        ArrayList<String> command = new ArrayList<>();
        command.add( "chmod" );
        command.add( "a+x" );
        command.add(  new File(prefPaneContents, "Resources/"+ applicationName +".app/Contents/MacOS/applet").getAbsolutePath() );
    	exec( command );
    	
		// Rename prefPane
		Files.move(new File(resourcesOutput, "SetupBuilderOSXPrefPane.prefPane").toPath(), prefPaneLocation.toPath(),  java.nio.file.StandardCopyOption.REPLACE_EXISTING );
        Files.delete(setPrefpane.toPath());

		// Copy Icon
		Files.copy(iconFile.toPath(), new File(prefPaneLocation, "Contents/Resources/ProductIcon.icns").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		
		// Patch Info.plist
		File prefPanePLIST = new File(prefPaneLocation, "Contents/Info.plist");
		setPlistProperty( prefPanePLIST, ":CFBundleIdentifier", setup.getMainClass() + ".prefPane" );
		setPlistProperty( prefPanePLIST, ":CFBundleName", title + " Preference Pane" );
		setPlistProperty( prefPanePLIST, ":CFBundleExecutable", applicationName );
		setPlistProperty( prefPanePLIST, ":NSPrefPaneIconLabel", title );

		setPlistProperty( prefPanePLIST, ":CFBundleExecutable", applicationName );
		
		File servicePLIST = new File(prefPaneLocation, "Contents/Resources/service.plist");
		setPlistProperty( servicePLIST, ":Name", title );
		setPlistProperty( servicePLIST, ":Label", setup.getMainClass() );
		setPlistProperty( servicePLIST, ":Program", "/Library/" + applicationName + "/" + applicationName + ".app/Contents/MacOS/" + setup.getBaseName() );
		setPlistProperty( servicePLIST, ":Description", setup.getServices().get(0).getDescription() );
		setPlistProperty( servicePLIST, ":Version", setup.getVersion() );
	}
	
	private void setPlistProperty(File plist, String property, String value) {
		
		// Set Property in plist file
		// /usr/libexec/PlistBuddy -c "Set PreferenceSpecifiers:19:Titles:0 $buildDate" "$BUNDLE/Root.plist"
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/libexec/PlistBuddy" );
        command.add( "-c" );
        command.add( "Set " + property +  " " + value );
        command.add( plist.getAbsolutePath() );
    	exec( command );
	}

    private void createPackageFromApp() throws Throwable {

        Files.createDirectories(new File(tmp.toFile(), "distribution").toPath(), new FileAttribute[0]);
        Files.createDirectories(new File(tmp.toFile(), "packages").toPath(), new FileAttribute[0]);
    	imageSourceRoot = tmp.toString() + "/distribution";
    	
		Files.createDirectories(new File(tmp.toFile(), "scripts").toPath(), new FileAttribute[0]);
		patchServiceFiles( "scripts/preinstall", new File(tmp.toFile(), "scripts/preinstall") );
		patchServiceFiles( "scripts/postinstall", new File(tmp.toFile(), "scripts/postinstall") );

		createPreferencePane();
        extractApplicationInformation();
    	createAndPatchDistributionXML();

		// Build Product for packaging
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/productbuild" );
        command.add( "--distribution" );
        command.add( tmp.toString() + "/distribution.xml" );
        command.add( "--package-path" );
        command.add( tmp.toString() + "/packages" );
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
        command.add( tmp.toString() + "/" + applicationName + ".plist" );
    	exec( command );
    	
    	// set identifier, create package
        command = new ArrayList<>();
        command.add( "/usr/bin/pkgbuild" );
        command.add( "--root" );
        command.add( buildDir.toString() );
        command.add( "--component-plist" );
        command.add( tmp.toString() + "/" + applicationName + ".plist" );
        command.add( "--identifier" );
        command.add( setup.getMainClass() );
        command.add( "--version" );
        command.add( setup.getVersion() );
        command.add( "--scripts" );
        command.add( tmp.toString() + "/scripts" );
        command.add( "--install-location" );
        command.add( "/Library/" + applicationName + "/" );
        command.add( tmp.toString() + "/packages/" + applicationName + ".pkg" );
    	exec( command );
    	
    	Files.copy( new File(tmp.toFile(), "/packages/" + applicationName + ".pkg").toPath() , new File(setup.getDestinationDir(), "/" + applicationName + ".pkgbuild.pkg").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
	}

	private void createAndPatchDistributionXML() throws Throwable {
		ArrayList<String> command;
		// Synthesize Distribution xml
        command = new ArrayList<>();
        command.add( "/usr/bin/productbuild" );
        command.add( "--synthesize" );
        command.add( "--package" );
        command.add( tmp.toString() + "/packages/" + applicationName + ".pkg" );
        command.add( tmp.toString() + "/distribution.xml" );
    	exec( command );

        patchDistributionXML();
	}

	private void patchDistributionXML() throws Throwable {

        File xml = new File( tmp.toString() + "/distribution.xml" );
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
        command.add( tmp.toString() );

        exec( command );
    }

    /**
     * Call hdiutil to detach temporary image
     */
    private void detach() {
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/hdiutil" );
        command.add( "detach" );
        command.add( tmp.toString() + "/" + title );
        exec( command );
    }

    /**
     * Call SetFile to set the volume icon.
     * @throws IOException IOException
     */
    private void setVolumeIcon() throws IOException {
        
    	// Copy Icon as file icon into attached container 
        File iconDestination = new File( tmp.toFile() , "/" + title + "/.VolumeIcon.icns" );
		Files.copy( iconFile.toPath(), iconDestination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
    	
    	ArrayList<String> command = new ArrayList<>();
        command.add( "SetFile" );
        command.add( "-a" );
        command.add( "C" );
        command.add( iconDestination.getParent() );
        exec( command, null, null, true );
        
        if ( task.getBackgroundImage() != null ) {
        	String name = task.getBackgroundImage().getName();
            File backgroundDestination = new File( tmp.toFile() , "/" + title + "/.resources/background" + name.substring(name.lastIndexOf('.')) );
            Files.createDirectories(backgroundDestination.getParentFile().toPath(), new FileAttribute[0]);
        	Files.copy(task.getBackgroundImage().toPath(), backgroundDestination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
        }
    }

    private void applescript() throws IOException {
        InputStream input = getClass().getResourceAsStream( "applescript.txt" );
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        copyData(input, bos);
        input.close();

        String script = new String( bos.toByteArray(), StandardCharsets.UTF_8 );
        script = script.replace( "${title}", title );
        script = script.replace( "${executable}", applicationName );
        script = script.replace( "${windowWidth}", task.getWindowWidth().toString() );
        script = script.replace( "${windowHeight}", task.getWindowHeight().toString() );
        script = script.replace( "${iconSize}", task.getIconSize().toString() );
        script = script.replace( "${fontSize}", task.getFontSize().toString() );
        
        if ( task.getBackgroundImage() != null ) {
        	String name = task.getBackgroundImage().getName();
			script = script.replace( "${backgroundExt}", name.substring(name.lastIndexOf('.'))  );
        }

        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/bin/osascript" );
        exec( command, new ByteArrayInputStream( script.getBytes( StandardCharsets.UTF_8 ) ), null, true );
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

	/**
	 * Unzip it
	 * 
	 * @param file input zip file
	 * @param output zip file output folder
	 */
	private void unZipIt(File file, File folder) {

		try {

			// create output directory is not exists
			if (!folder.exists()) {
				folder.mkdir();
			}

			// get the zip file content
			ZipFile zipFile = new ZipFile(file);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = (ZipEntry) entries.nextElement();
				
				String fileName = zipEntry.getName();
				if (zipEntry.isDirectory() ) {
					new File( folder, fileName).mkdir();
				} else {
					
					File target = new File(folder, fileName);
					InputStream inputStream = zipFile.getInputStream(zipEntry);
					FileOutputStream output = new FileOutputStream(target);
					copyData(inputStream, output);
					output.close();
					inputStream.close();
				}
			}
			
			zipFile.close();

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
