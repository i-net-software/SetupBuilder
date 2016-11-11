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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import javax.swing.JEditorPane;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.EditorKit;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.xml.parsers.ParserConfigurationException;

import org.gradle.api.GradleException;
import org.gradle.api.internal.file.CopyActionProcessingStreamAction;
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal;
import org.w3c.dom.Element;

import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.abstracts.DesktopStarter;
import com.inet.gradle.setup.abstracts.DocumentType;
import com.inet.gradle.setup.abstracts.Service;
import com.inet.gradle.setup.util.ResourceUtils;
import com.inet.gradle.setup.util.XmlFileBuilder;

/**
 * Builder for a *.wsx file. A *.wsx file is a XML that described MSI setup and is needed for the Wix tool.
 * 
 * @author Volker Berlin
 */
class WxsFileBuilder extends XmlFileBuilder<Msi> {

    private static final String     ICON_ID = "icon.ico";

    private Set<String>             components = new LinkedHashSet<>();

    private HashMap<String, String> ids        = new HashMap<>();

    private String                  jvmDll;

    private String                  javaDir;

    private boolean                 isAddFiles;

    /**
     * The product node in the XML.
     */
    private Element                 product;

    /**
     * Reference to INSTALLDIR
     */
    private Element                 installDir;

    /**
     * Create a new instance.
     * @param msi the MSI task
     * @param setup the SetupBuilder extension
     * @param wxsFile the file name
     * @param buildDir the temporary directory of the task
     * @param template a template file
     * @param addFiles if files should be added in this phase
     * @throws Exception if any error occur
     */
    WxsFileBuilder( Msi msi, SetupBuilder setup, File wxsFile, File buildDir, URL template, boolean addFiles ) throws Exception {
        super( msi, setup, wxsFile, buildDir, template );
        this.isAddFiles = addFiles;
    }

    /**
     * Create *.wxs file based on the settings in the task.
     * 
     * @throws ParserConfigurationException
     * @throws Exception if any error occur
     */
    void build() throws Exception {
        // Wix node
        Element wix = (Element)doc.getFirstChild();
        if( !"Wix".equals( wix.getTagName() ) ) {
            throw new IllegalArgumentException( "Template does not contains a Wix root: " + wix.getTagName() );
        }
        wix.setAttributeNS( "http://www.w3.org/2000/xmlns/", "xmlns", "http://schemas.microsoft.com/wix/2006/wi" );
        wix.setAttributeNS( "http://www.w3.org/2000/xmlns/", "xmlns:util", "http://schemas.microsoft.com/wix/UtilExtension" );

        // Product node
        product = getOrCreateChild( wix, "Product" );
        addAttributeIfNotExists( product, "Id", UUID.randomUUID().toString() ); // for multiple instances, every language transformation must have the same ProductCode
        addAttributeIfNotExists( product, "Language", "1033" );
        addAttributeIfNotExists( product, "Manufacturer", setup.getVendor() );
        addAttributeIfNotExists( product, "Name", setup.getApplication() );
        addAttributeIfNotExists( product, "Version", setup.getVersion() );
        addAttributeIfNotExists( product, "UpgradeCode", getGuid( "UpgradeCode" ) );

        // Package node
        Element media = getOrCreateChildById( product, "Media", "1", false ); // must be the second in Product
        addAttributeIfNotExists( media, "Cabinet", "media1.cab" );
        addAttributeIfNotExists( media, "EmbedCab", "yes" );
        Element packge = getOrCreateChild( product, "Package", false ); // must be the first in Product
        if( product.getFirstChild() != packge ) {
            product.insertBefore( packge, product.getFirstChild() );
        }
        addAttributeIfNotExists( packge, "Compressed", "yes" );
        if( !setup.getDescription().isEmpty() ) {
            addAttributeIfNotExists( packge, "Comments", setup.getDescription() );
        }
        addAttributeIfNotExists( packge, "InstallScope", task.getInstallScope().name() );

        // MajorUpgrade
        if( task.getMultiInstanceCount() <= 1 ) { 
            Element update = getOrCreateChild( product, "MajorUpgrade" );
            addAttributeIfNotExists( update, "AllowDowngrades", "yes" );
        }

        // Directory
        Element directory = getOrCreateChildById( product, "Directory", "TARGETDIR" );
        addAttributeIfNotExists( directory, "Name", "SourceDir" );
        Element programFiles = getOrCreateChildById( directory, "Directory", task.is64Bit() ? "ProgramFiles64Folder" : "ProgramFilesFolder" );
        Element appDirectory = getOrCreateChildById( programFiles, "Directory", "INSTALLDIR" );
        addAttributeIfNotExists( appDirectory, "Name", setup.getApplication() );

        //Files
        installDir = getOrCreateChildById( product, "DirectoryRef", "INSTALLDIR" );
        task.processFiles( new CopyActionProcessingStreamAction() {
            @Override
            public void processFile( FileCopyDetailsInternal details ) {
                try {
                    if( !details.isDirectory() ) {
                        String[] segments = details.getRelativePath().getSegments();
                        File file;
                        try {
                            file = details.getFile();
                        } catch( UnsupportedOperationException ex ) {
                            // if there is set an filter then we need to copy it
                            File buildDir = task.getTemporaryDir();
                            file = new File( buildDir, details.getRelativePath().getPathString() );
                            details.copyTo( file );
                        }
                        addFile( file, segments );
                    }
                } catch( Exception ex ) {
                    throw new GradleException( "Can't add file: " + details, ex );
                }
            }
        } );

        setMinimumOsVersion();
        setOnly32BitCondition();
        saveLoadLastInstallDir();
        addMultiInstanceTransforms();
        addBundleJre();
        addGUI();
        addIcon();
        addServices();
        addShortcuts();
        addRunBeforeUninstall();
        addRunAfter();
        addDeleteFiles();
        addPreAndPostScripts();

        //Feature
        Element feature = getOrCreateChildById( product, "Feature", "MainApplication" );
        for( String compID : components ) {
            getOrCreateChildById( feature, "ComponentRef", compID );
        }

        save();
    }

    /**
     * Add a condition for the minimum OS version if needed
     */
    private void setMinimumOsVersion() {
        double version = task.getMinOS();
        if( version == 0 ) {
            return;
        }
        int intVersion = 100 * (int)version + (int)((version * 10) % 10);
        String os;
        switch( intVersion ) {
            case 1000:
                os = "Windows 10, Windows Server 2016";
                break;
            case 603:
                os = "Windows 8.1, Windows Server 2012 R2";
                break;
            case 602:
                os = "Windows 8, Windows Server 2012";
                break;
            case 601:
                os = "Windows 7, Windows Server 2008 R2";
                break;
            case 600:
                os = "Windows Vista, Windows Server 2008";
                break;
            case 502:
                os = "Windows Server 2003";
                break;
            case 501:
                os = "Windows XP";
                break;
            case 500:
                os = "Windows 2000";
                break;
            default:
                throw new RuntimeException("Unsupported minimum OS version: " + version + ", " + intVersion );
        }
        String msg = java.text.MessageFormat.format( "{0} is only supported on {1}, or higher.", setup.getApplication(), os );
        Element condition = getOrCreateChildByKeyValue( product, "Condition", "Message", msg );
        condition.setTextContent( "Installed OR (VersionNT >= " + intVersion + ")" );
    }

    /**
     * Add condition for 32 bit only if needed.
     */
    private void setOnly32BitCondition() {
        if( !task.isOnly32Bit() ) {
            return;
        }
        String msg = "You are attempting to run the 32-bit installer on a 64-bit version of Windows.";
        Element condition = getOrCreateChildByKeyValue( product, "Condition", "Message", msg );
        condition.setTextContent( "NOT Msix64" );
    }

    /**
     * Save the directory of the last installation and load it on an update as default install dir.
     */
    private void saveLoadLastInstallDir() {
        String key = "Software\\" + setup.getVendor() + '\\' + setup.getApplication() + '\\' + "LastInstallDir";

        // save the INSTALLDIR to the registry
        Element component = getComponent( installDir, "install_path" );
        Element regkey = addRegistryKey( component, "HKLM", "install_path_reg", key );
        addRegistryValue( regkey, null, "string", "[INSTALLDIR]" );

        // load the INSTALLDIR from the registry
        Element lastInstalldir = getOrCreateChildById( product, "Property", "INSTALLDIR" );
        addAttributeIfNotExists( lastInstalldir, "Secure", "yes" );
        Element search = getOrCreateChildById( lastInstalldir, "RegistrySearch", "SearchInstallDir" );
        addAttributeIfNotExists( search, "Root", "HKLM" );
        addAttributeIfNotExists( search, "Key", key );
        addAttributeIfNotExists( search, "Type", "directory" );
    }

    /**
     * Get or create a directory node.
     * 
     * @param segments the segments of the path in the target. The last segment contains the file name.
     * @return the directory node
     */
    private Element getDirectory( String[] segments ) {
        return getDirectory( segments, segments.length - 1 );
    }

    /**
     * Get or create a directory node.
     * 
     * @param segments the segments of the path in the target. The last segment contains the file name.
     * @param length the used length from the segments
     * @return the directory node
     */
    private Element getDirectory( String[] segments, int length ) {
        Element parent = installDir;
        for( int i = 0; i < length; i++ ) {
            String seg = segments[i];
            parent = getOrCreateChildById( parent, "Directory", id( segments, i + 1 ) );
            addAttributeIfNotExists( parent, "Name", seg );
        }
        return parent;
    }

    /**
     * Get or Create a component node
     * 
     * @param dir the parent directory node.
     * @param compID the ID of the component
     * @return the component node
     */
    private Element getComponent( Element dir, String compID ) {
        components.add( compID );
        Element component = getOrCreateChildById( dir, "Component", compID );
        addAttributeIfNotExists( component, "Guid", getGuid( compID ) );
        if( task.getMultiInstanceCount() > 1 ) {
            addAttributeIfNotExists( component, "MultiInstance", "yes" );
        }
        return component;
    }

    /**
     * Add a file to the setup
     * 
     * @param file the file to add.
     * @param segments the segments of the path in the target. The last segment contains the file name.
     */
    private String addFile( File file, String[] segments ) {
        Element parent = getDirectory( segments );

        String pathID = id( segments, segments.length - 1 );
        String compID = id( pathID + "_Comp");
        Element component = getComponent( parent, compID );

        String name = segments[segments.length - 1];
        String id = addFile( component, file, pathID, name );

        // save the jvm.dll position
        if( name.equals( "jvm.dll" ) ) {
            StringBuilder jvm = new StringBuilder();
            for( String segment : segments ) {
                if( jvm.length() > 0 ) {
                    jvm.append( '\\' );
                }
                jvm.append( segment );
            }
            jvmDll = jvm.toString();
        }
        return id;
    }

    /**
     * Add a file.
     * 
     * @param component the parent component node
     * @param file the source file
     * @param pathID an ID of the parent path
     * @param name the target file name
     */
    private String addFile( Element component, File file, String pathID, String name ) {
        return addFile( component, file, pathID, name, isAddFiles );
    }

    /**
     * Add a file.
     * 
     * @param component the parent component node
     * @param file the source file
     * @param pathID an ID of the parent path
     * @param name the target file name
     * @param isAddFiles if the file should added or not. On creating the multi language translations the files will not
     *            added to improve the performance.
     */
    private String addFile( Element component, File file, String pathID, String name, boolean isAddFiles ) {
        String id = id( pathID + '_' + name );
        if( isAddFiles ) {
            Element fileEl = getOrCreateChildById( component, "File", id );
            addAttributeIfNotExists( fileEl, "Source", file.getAbsolutePath() );
            addAttributeIfNotExists( fileEl, "Name", name );
        } else {
            getOrCreateChild( component, "CreateFolder" );
        }
        return id;
    }

    /**
     * Add all files in a directory.
     * 
     * @param dir the source directory
     * @param baseLength the base length of the directory. This length will be cut from the absolute path.
     * @param target the target directory
     */
    private void addDirectory( File dir, int baseLength, String target ) {
        for( File file : dir.listFiles() ) {
            if( file.isDirectory() ) {
                addDirectory( file, baseLength, target );
            } else {
                String name = file.getAbsolutePath().substring( baseLength );
                addFile( file, segments( (target + name) ) );
            }
        }
    }

    /**
     * Bundle a JRE if setup.
     */
    private void addBundleJre() {
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
            // bundleJRE is not a directory, we interpret it as a version number
            String programFiles = System.getenv( task.is64Bit() ? "ProgramW6432" : "ProgramFiles(x86)" );
            if( programFiles == null ) {
                throw new GradleException( "Environment ProgramFiles not found." );
            }
            File java = new File( new File( programFiles ), "Java" );
            if( !java.isDirectory() ) {
                throw new GradleException( "No installed Java VMs found: " + java );
            }

            List<File> versions = new ArrayList<File>();
            String [] s = { "jre", "jdk" };
            Arrays.asList( s ).forEach( prefix -> {
                versions.addAll( getDirectories( java, prefix + jre ) );
                if( versions.size() == 0 ) {
                    // if the java version is like "1.8" then search also for "jre8"
                    String jreStr = jre.toString();
                    if( jreStr.length() > 2 && jreStr.startsWith( "1." ) ) {
                        versions.addAll( getDirectories( java, "jre" + jreStr.substring( 2 ) ) );
                    }
                }
            });

            if( versions.size() == 0 ) {
                throw new GradleException( "bundleJre version " + jre + " can not be found in: '" + java + "' Its search for an folder that starts with: jre" + jre );
            }

            Collections.sort( versions );
            jreDir = versions.get( versions.size() - 1 );
            if ( jreDir.getName().startsWith( "jdk" ) ) {
                // if this was a jdk, we have to use the jre subdirectory
                jreDir = new File( jreDir, "jre" );
            }
        }

        task.getProject().getLogger().lifecycle( "\tbundle jre: " + jreDir );

        int baseLength = jreDir.getAbsolutePath().length();
        javaDir = setup.getBundleJreTarget().replace( '/', '\\' );
        if( javaDir.endsWith( "\\" ) ) {
            baseLength++;
        }

        addDirectory( jreDir, baseLength, javaDir );
    }

    /**
     * Get all directories from the directory that start with the prefix.
     * 
     * @param parent parent directory
     * @param prefix the searching prefix
     * @return list of directories
     */
    private static List<File> getDirectories( File parent, String prefix ) {
        ArrayList<File> files = new ArrayList<File>();
        for( File file : parent.listFiles() ) {
            if( file.isDirectory() && file.getName().startsWith( prefix ) ) {
                files.add( file );
            }
        }
        return files;
    }

    /**
     * Add the GUI to the Setup.
     * 
     * @throws Exception if any exception occur
     */
    private void addGUI() throws Exception {
        Element wixUiInstalldir = getOrCreateChildById( product, "Property", "WIXUI_INSTALLDIR" );
        addAttributeIfNotExists( wixUiInstalldir, "Value", "INSTALLDIR" );
        Element uiRef = getOrCreateChild( product, "UIRef" );
        addAttributeIfNotExists( uiRef, "Id", "WixUI_InstallDir" );

        boolean isLicense = addLicense( product );
        if( !isLicense ) {
            // skip license dialog because we does no hat a license text
            Element ui = getOrCreateChild( product, "UI" );
            Element child = getOrCreateChildByKeyValue( ui, "Publish", "Dialog", "WelcomeDlg" );
            child.setAttribute( "Control", "Next" );
            child.setAttribute( "Event", "NewDialog" );
            child.setAttribute( "Value", "InstallDirDlg" );
            child.setAttribute( "Order", "2" );
            child.setTextContent( "1" );

            child = getOrCreateChildByKeyValue( ui, "Publish", "Dialog", "InstallDirDlg" );
            child.setAttribute( "Control", "Back" );
            child.setAttribute( "Event", "NewDialog" );
            child.setAttribute( "Value", "WelcomeDlg" );
            child.setAttribute( "Order", "2" );
            child.setTextContent( "1" );
        }

        File file = task.getBannerBmp();
        if( file != null ) {
            Element licenseNode = getOrCreateChildById( product, "WixVariable", "WixUIBannerBmp" );
            addAttributeIfNotExists( licenseNode, "Value", file.getAbsolutePath() );
        }
        file = task.getDialogBmp();
        if( file != null ) {
            Element licenseNode = getOrCreateChildById( product, "WixVariable", "WixUIDialogBmp" );
            addAttributeIfNotExists( licenseNode, "Value", file.getAbsolutePath() );
        }
    }

    /**
     * Add an icon in Add/Remove Programs
     * 
     * @param appDirRef
     * @throws IOException if an error occur on reading the image files
     */
    private void addIcon() throws IOException {
        File iconFile = setup.getIconForType( buildDir, "ico" );
        if( iconFile == null ) {
            // no icon was set
            return;
        }
        Element icon = getOrCreateChildById( product, "Icon", ICON_ID );
        addAttributeIfNotExists( icon, "SourceFile", iconFile.getAbsolutePath() );
        Element appProduction = getOrCreateChildById( product, "Property", "ARPPRODUCTICON" );
        addAttributeIfNotExists( appProduction, "Value", ICON_ID );
    }

    /**
     * Add a license file to the setup. If the license file is not an RTF file then it convert it to RTF.
     * 
     * @param product the product node in the XML.
     * @return true, if license was added; false if license was not added.
     * @throws Exception if any exception occur
     */
    private boolean addLicense( Element product ) throws Exception {
    	// TODO: Internationalize
        File license = setup.getLicenseFile( "en" );
        if( license == null ) {
            return false;
        }
        boolean isRtf;
        try( FileInputStream fis = new FileInputStream( license ) ) {
            byte[] bytes = new byte[5];
            fis.read( bytes );
            isRtf = "{\rtf".equals( new String( bytes ) );
        }
        if( !isRtf ) {
            // Convert a txt file in a rtf file
            JEditorPane p = new JEditorPane();
            EditorKit kit = p.getEditorKitForContentType( "text/plain" );
            p.setContentType( "text/rtf" );
            DefaultStyledDocument doc = (DefaultStyledDocument)p.getDocument();

            try( FileInputStream fis = new FileInputStream( license ) ) {
                kit.read( fis, doc, 0 );
            }
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setFontSize( attrs, 9 );
            StyleConstants.setFontFamily( attrs, "Courier New" );
            doc.setCharacterAttributes( 0, doc.getLength(), attrs, false );

            kit = p.getEditorKitForContentType( "text/rtf" );
            license = new File( buildDir, "license.rtf" );
            try( FileOutputStream output = new FileOutputStream( license ) ) {
                kit.write( output, doc, 0, doc.getLength() );
            }
        }

        Element licenseNode = getOrCreateChildById( product, "WixVariable", "WixUILicenseRtf" );
        addAttributeIfNotExists( licenseNode, "Value", license.getAbsolutePath() );
        return true;
    }

    /**
     * Add the windows services.
     * 
     * @throws IOException if an I/O error occurs when reading or writing
     */
    private void addServices() throws IOException {
        List<Service> services = setup.getServices();
        if( services == null || services.isEmpty() ) {
            return;
        }

        File prunsrv = ResourceUtils.extract( getClass(), task.getArch() + "/prunsrv.exe", buildDir );
        File prunmgr = ResourceUtils.extract( getClass(), "x86/prunmgr.exe", buildDir );

        for( Service service : services ) {
            String name = service.getId();
            String id = id( name.replace( '-', '_' ) ) + "_service";
            String exe = service.getWrapper().replace( '\\', '/' ) + ".exe";

            String subdir = service.getWorkDir();
            if ( subdir != null && !subdir.isEmpty() ) {
                exe = new File( new File( subdir ), exe ).getPath();
                if( !subdir.endsWith( "\\" ) ) {
                    subdir += '\\';
                }
            } else {
                subdir = "";
            }

            // add the service file
            String[] segments = segments( exe );
            Element directory = getDirectory( segments );
            Element component = getComponent( directory, id );
            addFile( component, prunsrv, id, segments[segments.length - 1], true );

            // install the windows service
            Element install = getOrCreateChildById( component, "ServiceInstall", id + "_install" );
            addAttributeIfNotExists( install, "Name", name );
            addAttributeIfNotExists( install, "DisplayName", service.getDisplayName() );
            addAttributeIfNotExists( install, "Description", service.getDescription() );
            addAttributeIfNotExists( install, "Start", service.isStartOnBoot() ? "auto" : "demand" );
            addAttributeIfNotExists( install, "Type", "ownProcess" );
            addAttributeIfNotExists( install, "ErrorControl", "normal" );
            addAttributeIfNotExists( install, "Arguments", " \"//RS//" + name + "\"");

            // add an empty parameters registry key
            Element regkey = getOrCreateChildById( component, "RegistryKey", id + "_RegParameters" );
            addAttributeIfNotExists( regkey, "Root", "HKLM" );
            addAttributeIfNotExists( regkey, "Key", "SYSTEM\\CurrentControlSet\\Services\\" + name + "\\Parameters" );
            addAttributeIfNotExists( regkey, "ForceDeleteOnUninstall", "yes" );
            addAttributeIfNotExists( regkey, "ForceCreateOnInstall", "yes" );

            // Java parameter of the service
            String baseKey =
                            task.is64Bit() ? "SOFTWARE\\Wow6432Node\\Apache Software Foundation\\ProcRun 2.0\\"
                                            : "SOFTWARE\\Apache Software Foundation\\ProcRun 2.0\\";
            regkey = addRegistryKey( component, "HKLM", id + "_RegJava", baseKey + name + "\\Parameters\\Java" );
            addRegistryValue( regkey, "Classpath", "string", service.getMainJar() );
            if( setup.getBundleJre() != null ) {
                addRegistryValue( regkey, "JavaHome", "string", "[INSTALLDIR]" + setup.getBundleJreTarget() );
                addRegistryValue( regkey, "Jvm", "string", "[INSTALLDIR]" + jvmDll );
            }
            regkey = addRegistryKey( component, "HKLM", id + "_RegStart", baseKey + name + "\\Parameters\\Start" );
            addRegistryValue( regkey, "Class", "string", service.getMainClass() );
            addRegistryValue( regkey, "Mode", "string", "jvm" );
            addRegistryValue( regkey, "WorkingPath", "string", "[INSTALLDIR]" + subdir );
            regkey = addRegistryKey( component, "HKLM", id + "_RegLog", baseKey + name + "\\Parameters\\Log" );
            addRegistryValue( regkey, "Path", "string", "[INSTALLDIR]" + subdir );
            addRegistryValue( regkey, "Prefix", "string", "service" );
            regkey = addRegistryKey( component, "HKLM", id + "_RegStop", baseKey + name + "\\Parameters\\Stop" );
            addRegistryValue( regkey, "Class", "string", "java.lang.System" ); // call System.exit() on stop to support Runtime.getRuntime().addShutdownHook()
            addRegistryValue( regkey, "Mode", "string", "jvm" );

            // start the service
            if( service.isStartOnBoot() ) {
                Element start = getOrCreateChildById( component, "ServiceControl", id + "_start" );
                addAttributeIfNotExists( start, "Name", name );
                addAttributeIfNotExists( start, "Start", "install" );
                addAttributeIfNotExists( start, "Stop", "both" );
                addAttributeIfNotExists( start, "Remove", "uninstall" );
                addAttributeIfNotExists( start, "Wait", "yes" );

                // MSI does not restart a service if it was stopped before the setup
                DesktopStarter run = new DesktopStarter( setup );
                run.setExecutable( "net" );
                run.setStartArguments( "start \"" + name + "\"" );
                addRun( run, id + "Restart", "ignore", null );
                addCustomActionToSequence( id + "Restart", true, "InstallFiles", true, "NOT REMOVE" );
            }

            // Add the prunmgr.exe and change it name dynamically to the service name. Dynamically is important for multiple instances.
            String target = name.replace( '[', '_' ).replace( ']', '_' );
            addFile( component, prunmgr, id + "GUI", target + ".exe", true );
            renameFileIfDynamic( id, subdir, target + ".exe", name + ".exe" );

            // delete log files on uninstall
            addDeleteFiles( subdir + "service.*.log" );
        }
    }

    /**
     * Rename a file after it was copy.
     * 
     * @param id the base ID for the actions
     * @param directory the directory of the file
     * @param sourceName the sourcename. This should not contain any placeholder in brackets []
     * @param targetName the targetname, this can contain place holder
     */
    private void renameFileIfDynamic( String id, String directory, String sourceName, String targetName ) {
        int idx = targetName.indexOf( '[' );
        if( idx < 0 || targetName.indexOf( ']' ) < idx ) {
            // not a dynamic name 
            return;
        }

        DesktopStarter run = new DesktopStarter( setup );
        run.setExecutable( "ren" );
        run.setStartArguments( "\"" + directory + sourceName + "\" \"" + targetName + "\"" );
        addRun( run, id + "Rename", "ignore", null );
        addCustomActionToSequence( id + "Rename", true, "CreateShortcuts", true, null );

        run = new DesktopStarter( setup );
        run.setExecutable( "del" );
        run.setStartArguments( "/Q /F \"" + directory + targetName + "\"" );
        addRun( run, id + "Delete", "ignore", null );
        addCustomActionToSequence( id + "Delete", true, "InstallFiles", false, "NOT Installed OR REINSTALL OR REMOVE" );
    }

    /**
     * Get the parent Component for a shortcut.
     * 
     * @param starter the shortcut description
     * @param product the node of the product
     * @return the component node
     */
    private Element getShortcutComponent( DesktopStarter starter, Element product ) {
        Element targetDir = getOrCreateChildById( product, "Directory", "TARGETDIR" );
        String refDirID;
        boolean removeOnUninstall = false;
        switch( starter.getLocation() ) {
            default:
            case StartMenu:
                refDirID = "ProgramMenuFolder";
                getOrCreateChildById( targetDir, "Directory", refDirID );
                getOrCreateChildById( product, "DirectoryRef", refDirID );
                break;
            case ApplicationMenu:
                refDirID = "ApplicationProgramsFolder";
                Element menuFolders = getOrCreateChildById( targetDir, "Directory", "ProgramMenuFolder" );
                Element appProgrammsFolder = getOrCreateChildById( menuFolders, "Directory", "ApplicationProgramsFolder" );
                addAttributeIfNotExists( appProgrammsFolder, "Name", setup.getApplication() );
                removeOnUninstall = true;
                break;
            case InstallDir:
                refDirID = "INSTALLDIR";
                break;
        }
        Element dirRef = getOrCreateChildById( product, "DirectoryRef", refDirID );

        Element component = getComponent( dirRef, "shortcuts_" + refDirID );
        if( removeOnUninstall ) {
            Element removeFolder = getOrCreateChildById( component, "RemoveFolder", "ApplicationProgramsFolder" );
            addAttributeIfNotExists( removeFolder, "On", "uninstall" );
        }
        Element reg = addRegistryKey( component, "HKCU", "shortcuts_reg_" + refDirID, "Software\\" + setup.getVendor() + "\\" + setup.getApplication() );
        reg = addRegistryValue( reg, "shortcut_" + refDirID, "string", "" );
        addAttributeIfNotExists( reg, "KeyPath", "yes" );

        return component;
    }

    /**
     * Add the shortcuts if which was define.
     * 
     * @throws IOException If any I/O exception occur on icon loading
     */
    private void addShortcuts() throws IOException {
        List<DesktopStarter> starters = setup.getDesktopStarters();
        if( starters.isEmpty() ) {
            return;
        }

        for( DesktopStarter starter : starters ) {
            Element component = getShortcutComponent( starter, product );
            String id = id( starter.getLocation() + "_" + starter.getDisplayName() );
            Element shortcut = getOrCreateChildById( component, "Shortcut", id );
            String name = starter.getDisplayName().replace( '[', '_' ).replace( ']', '_' );
            addAttributeIfNotExists( shortcut, "Name", name );
            addAttributeIfNotExists( shortcut, "Description", starter.getDescription() );

            addAttributeIfNotExists( shortcut, "WorkingDirectory", getWoringDirID( starter ) );

            // find the optional id for an icon
            String target = starter.getExecutable();
            String iconID;
            if( starter.getIcons() != null ) {
                if( starter.getIcons().equals( setup.getIcons() ) ) {
                    iconID = ICON_ID;
                } else {
                    File iconFile = starter.getIconForType( buildDir, "ico" );
                    iconID = id( starter.getDisplayName() + ".ico" );
                    Element icon = getOrCreateChildById( product, "Icon", iconID );
                    addAttributeIfNotExists( icon, "SourceFile", iconFile.getAbsolutePath() );
                }
            } else {
                iconID = null;
            }
            if( target == null || target.isEmpty() ) {
                // if target is empty then it must be a Java application
                if( iconID == null ) {
                    iconID = ICON_ID;
                }
            }

            CommandLine cmd = new CommandLine( starter, javaDir );
            addAttributeIfNotExists( shortcut, "Target", cmd.target );
            if( !cmd.arguments.isEmpty() ) {
                addAttributeIfNotExists( shortcut, "Arguments", cmd.arguments );
            }
            if( iconID != null ) {
                addAttributeIfNotExists( shortcut, "Icon", iconID );
            }


            String linkLocation;
            switch( starter.getLocation() ) {
                default:
                case StartMenu:
                    linkLocation = "[ProgramMenuFolder]";
                    break;
                case ApplicationMenu:
                    linkLocation = "[ApplicationProgramsFolder]";
                    break;
                case InstallDir:
                    linkLocation = "[INSTALLDIR]";
                    break;
            }
            if( starter.getWorkDir() != null ) {
                linkLocation += starter.getWorkDir();
            }
            renameFileIfDynamic( id, linkLocation, name + ".lnk", starter.getDisplayName() + ".lnk" );

            registerFileExtension( starter, cmd );
        }
    }

    /**
     * Register file extension.
     * 
     * @param starter desktop starter
     * @param cmd the command line to execute
     * @throws IOException if any IOException occur
     */
    private void registerFileExtension( DesktopStarter starter, CommandLine cmd ) throws IOException {
        if( isAddFiles ) {
            for( DocumentType docType : starter.getDocumentType() ) {
                for( String fileExtension : docType.getFileExtension() ) {
                    if( fileExtension.startsWith( "." ) ) {
                        fileExtension = fileExtension.substring( 1 );
                    }
                    String pID = id( setup.getAppIdentifier() + "." + fileExtension );
                    Element component = getComponent( installDir, "_file_extension" );
                    getOrCreateChild( component, "CreateFolder" );
                    Element progID = getOrCreateChildById( component, "ProgId", pID );
                    addAttributeIfNotExists( progID, "Description", docType.getName() );

                    File iconFile = starter.getIconForType( buildDir, "ico" );
                    if( iconFile != null ) {
                        String iconID = addFile( iconFile, new String[] { iconFile.getName() } );
                        addAttributeIfNotExists( progID, "Icon", iconID );
                    }
                    Element extension = getOrCreateChildById( progID, "Extension", fileExtension );
                    addAttributeIfNotExists( extension, "ContentType", docType.getMimetype() );
                    Element verb = getOrCreateChildById( extension, "Verb", "open" );
                    Element reg = addRegistryKey( component, "HKCR", id(pID + "\\shell\\open"), pID + "\\shell\\open" );
                    addRegistryValue( reg, "FriendlyAppName", "string", setup.getApplication() );
                    String[] segments = segments( cmd.relativTarget );
                    addAttributeIfNotExists( verb, "TargetFile", id( segments, segments.length ) );
                    addAttributeIfNotExists( verb, "Argument", cmd.arguments + "\"%1\"" );
                }
            }
        }
    }

    /**
     * Get the ID of the working directory.
     * 
     * @param starter the shortcut definition
     * @return the id
     */
    private String getWoringDirID( DesktopStarter starter ) {
        String workDir = starter.getWorkDir();
        if( workDir != null && !workDir.isEmpty() ) {
            String[] segments = segments( workDir );
            Element dir = getDirectory( segments, segments.length );
            return dir.getAttribute( "Id" );
        } else {
            return "INSTALLDIR";
        }
    }

    /**
     * Add a runner without command line window.
     * 
     * @param run the runner
     * @param id the id that should be used
     * @param Return the Return attribute, can be null (default "check"), possible values: "asyncNoWait", "asyncWait", "check" or "ignore"
     * @param execute the Execute attribute
     */
    private void addRun( DesktopStarter run, String id, String Return, String execute ) {
        CommandLine cmd = new CommandLine( run, javaDir );
        Element action = getOrCreateChildById( product, "CustomAction", id );

        // run with elevated privileges
        addAttributeIfNotExists( action, "Execute", execute == null ? "deferred" : execute );
        addAttributeIfNotExists( action, "Impersonate", "no" );
        if( Return != null ) {
            addAttributeIfNotExists( action, "Return", Return );
        }

        String targetID;
        String dllEntry;
        String command;
        if( !"asyncNoWait".equals( Return ) ) {
            if( cmd.arguments.isEmpty() ) {
                targetID = "WixShellExecTarget";
                dllEntry = "WixShellExec";
                command = cmd.full;
            } else {
                targetID = id;
                dllEntry = "CAQuietExec";
                command = "\"[SystemFolder]cmd.exe\" /C \"cd /D \"[INSTALLDIR]" + cmd.workDir + "\" & " + cmd.full + '\"';
            }
        } else {
            addAttributeIfNotExists( action, "Directory", getWoringDirID( run ) );
            addAttributeIfNotExists( action, "ExeCommand", '\"' + cmd.target + "\" " + cmd.arguments ); // full quoted path + arguments 
            return;
        }
        Element target = getOrCreateChildByKeyValue( product, "SetProperty", "Action", id + "_target" );
        addAttributeIfNotExists( target, "Id", targetID );
        addAttributeIfNotExists( target, "Before", id );
        addAttributeIfNotExists( target, "Sequence", "execute" );
        addAttributeIfNotExists( target, "Value", command );

        addAttributeIfNotExists( action, "BinaryKey", "WixCA" );
        addAttributeIfNotExists( action, "DllEntry", dllEntry );
    }

    /**
     * Add an action that is executed before uninstall.
     */
    private void addRunBeforeUninstall() {
        DesktopStarter runBeforeUninstall = setup.getRunBeforeUninstall();
        if( runBeforeUninstall == null ) {
            return;
        }
        String id = "runBeforeUninstall";
        addRun( runBeforeUninstall, id, "ignore", null );

        // http://stackoverflow.com/questions/320921/how-to-add-a-wix-custom-action-that-happens-only-on-uninstall-via-msi
        addCustomActionToSequence( id, true, "StopServices", true, "REMOVE=\"ALL\" AND NOT UPGRADINGPRODUCTCODE" );
    }

    /**
     * Add an action that is executed after the setup.
     */
    private void addRunAfter() {
        DesktopStarter runAfter = setup.getRunAfter();
        if( runAfter == null ) {
            return;
        }
        String id = "runAfter";
        addRun( runAfter, id, "asyncNoWait", "immediate" );

        Element ui = getOrCreateChild( product, "UI" );
        Element exitDialog = getOrCreateChildByKeyValue( ui, "Publish", "Dialog", "ExitDialog" );
        addAttributeIfNotExists( exitDialog, "Control", "Finish" );
        addAttributeIfNotExists( exitDialog, "Event", "DoAction" );
        addAttributeIfNotExists( exitDialog, "Value", id );
        // http://stackoverflow.com/questions/320921/how-to-add-a-wix-custom-action-that-happens-only-on-uninstall-via-msi
        exitDialog.setTextContent( "NOT Installed OR REINSTALL OR UPGRADINGPRODUCTCODE" );
    }

    /**
     * Add a CustomAction to one of the sequences.
     * 
     * @param id the ID of the action
     * @param execute true, InstallExecuteSequence; false, InstallUISequence
     * @param sequenceAction the name of an existing action in sequence after which it should be added.
     *            https://msdn.microsoft.com/en-us/library/aa372038(v=vs.85).aspx
     *            https://msdn.microsoft.com/en-us/library/aa372039(v=vs.85).aspx
     * @param after true, After the action; false, Before
     * @param condition the condition under which it should run
     * @return the Custom element node
     */
    private Element addCustomActionToSequence( String id, boolean execute, String sequenceAction, boolean after, String condition ) {
        Element executeSequence = getOrCreateChild( product, execute ? "InstallExecuteSequence" : "InstallUISequence" );
        Element custom = getOrCreateChildByKeyValue( executeSequence, "Custom", "Action", id );
        addAttributeIfNotExists( custom, after ? "After" : "Before", sequenceAction );
        if( condition != null ) {
            custom.setTextContent( condition );
        }
        return custom;
    }

    /**
     * Add files to delete if there any define in SetupBuilder.
     */
    private void addDeleteFiles() {
        for( String pattern : setup.getDeleteFiles() ) {
            addDeleteFiles( pattern );
        }
        for( String folder : setup.getDeleteFolders() ) {
            folder = folder.replace( '/', '\\' );
            String id = id( "rmdir_" + folder );

            if( folder.endsWith( "\\" ) ) {
                folder = folder.substring( 0, folder.length()-1 );
            }
            DesktopStarter run = new DesktopStarter( setup );
            run.setExecutable( "rmdir" );
            run.setStartArguments( "/S /Q \"[INSTALLDIR]" + folder + '\"' );
            addRun( run, id, "ignore", null );
            addCustomActionToSequence( id, true, "RemoveFolders", true, null );
        }
    }

    /**
     * Add files to delete.
     * 
     * @param pattern the pattern to delete
     * @return the component
     */
    private Element addDeleteFiles( String pattern ) {
        String[] segments = segments( pattern );
        Element dir = getDirectory( segments );
        String id = id( pattern );
        Element component = getComponent( dir, "deleteFiles" + id );
        Element remove = getOrCreateChildById( component, "RemoveFile", "removeFile" + id );
        addAttributeIfNotExists( remove, "On", "both" );
        addAttributeIfNotExists( remove, "Name", segments[segments.length - 1] );
        return component;
    }

    /**
     * Add a registry key.
     * 
     * @param component parent component
     * @param root The root of the key like HKLM, HKCU, HKMU
     * @param id the id for the key
     * @param key the key
     * @return the element to add values
     */
    private Element addRegistryKey( Element component, String root, String id, String key ) {
        Element regkey = getOrCreateChildById( component, "RegistryKey", id );
        addAttributeIfNotExists( regkey, "Root", root );
        addAttributeIfNotExists( regkey, "Key", key );
        addAttributeIfNotExists( regkey, "ForceDeleteOnUninstall", "yes" );
        return regkey;
    }

    /**
     * Add a registry value.
     * 
     * @param regkey the parent registry key
     * @param name the value name, null use the default value of a key
     * @param type the type
     * @param value the value
     * @return the value node
     */
    private Element addRegistryValue( Element regkey, String name, String type, String value ) {
        Element regValue = getOrCreateChildByKeyValue( regkey, "RegistryValue", "Name", name );
        addAttributeIfNotExists( regValue, "Type", type );
        addAttributeIfNotExists( regValue, "Value", value );
        return regValue;
    }

    /**
     * Split a path in segments.
     * 
     * @param path the path to split
     * @return the segments of the path
     */
    String[] segments( String path ) {
        return path.split( "[/\\\\]" );
    }

    /**
     * Create a valid id for a directory from path segments.
     * 
     * @param segments the segments of the path in the target. The last segment contains the file name.
     * @param length the length of the segments that should be used for the id
     * @return a valid id
     */
    private String id( String[] segments, int length ) {
        if( length <= 0 ) {
            return "";
        } else if( length == 1 ) {
            return id( segments[0] );
        } else {
            StringBuilder builder = new StringBuilder();
            for( int i = 0; i < length; i++ ) {
                if( i > 0 ) {
                    builder.append( '_' );
                }
                builder.append( segments[i] );
            }
            return id( builder.toString() );
        }
    }

    /**
     * Create a valid id from string for the wxs file.
     * 
     * @param str possible id but with possible invalid characters
     * @return a valid id
     */
    private String id( String str ) {
        StringBuilder builder = null;
        boolean needUnderscoreStart = false;
        for( int i = 0; i < str.length(); i++ ) {
            char ch = str.charAt( i );
            if( (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch == '_') ) {
                continue;
            }
            if( (ch >= '0' && ch <= '9') || (ch == '.') ) {
                if( i > 0 ) {
                    continue;
                }
                // id must begin with either a letter or an underscore.
                needUnderscoreStart = true;
                builder = new StringBuilder();
                continue;
            }
            if( builder == null ) {
                builder = new StringBuilder();
            }
            builder.append( str.substring( builder.length(), i ) );
            builder.append( '_' );
        }
        if( builder == null ) {
            if( str.length() <= 72 ) {
                if( !ids.containsKey( str ) ) {
                    ids.put( str, str );
                    return str;
                }
                if( str.equals( ids.get( str ) ) ) {
                    return str;
                }
            }
            builder = new StringBuilder();
        }
        builder.append( str.substring( builder.length() ) );
        if( builder.length() > 62 ) {
            // 72 is the max id length, we remove the starting part because this occurs only with files and there is the max important part at the end.
            builder.delete( 0, builder.length() - 62 );
            char ch = builder.charAt( 0 );
            needUnderscoreStart = (ch >= '0' && ch <= '9') || (ch == '.');
        }
        if( needUnderscoreStart ) {
            builder.insert( 0, '_' );
        }
        String id = builder.toString();
        if( !ids.containsKey( id ) ) {
            ids.put( id, str );
            return id; // new pair
        }
        if( str.equals( ids.get( id ) ) ) {
            return id; // identical pair already exists
        }
        // we have a collision, add a hashcode to prevent name collision
        builder.append( '_' );
        builder.append( Integer.toHexString( str.hashCode() ) );
        return builder.toString();
    }

    /**
     * Create a reproducible GUID
     * 
     * @param id a parameter as random input
     * @return the GUID
     */
    private String getGuid( String id ) {
        return UUID.nameUUIDFromBytes( (setup.getVendor() + setup.getApplication() + id).getBytes() ).toString();
    }

    /**
     * Add the settings for multiple instances
     * 
     * @throws IOException If an I/O error occur on loading resources
     */
    private void addMultiInstanceTransforms() throws IOException {
        int instanceCount = task.getMultiInstanceCount();
        if( instanceCount <= 1 ) {
            return;
        }
        // http://windows-installer-xml-wix-toolset.687559.n2.nabble.com/Multiple-Instance-Transforms-Walkthrough-Proposed-Simple-Addition-to-WiX-to-Make-Them-Easier-td708828.html

        // define the property "INSTANCE_ID"
        Element property = getOrCreateChildById( product, "Property", "INSTANCE_ID" );
        addAttributeIfNotExists( property, "Value", "NotSet" );

        // define the property "InstancesCount"
        property = getOrCreateChildById( product, "Property", "InstancesCount" );
        addAttributeIfNotExists( property, "Value", Integer.toString( instanceCount ) );

        // add instance count InstanceTransforms and UpgradeVersion
        Element transforms = getOrCreateChildByKeyValue( product, "InstanceTransforms", "Property", "INSTANCE_ID" );
        for( int i = 0; i < instanceCount; i++ ) {
            String id = "Instance_" + i;
            String guid = getGuid( id );
            Element instance = getOrCreateChildById( transforms, "Instance", id );
            addAttributeIfNotExists( instance, "ProductCode", "*" );
            addAttributeIfNotExists( instance, "UpgradeCode", guid );

            Element upgrade = getOrCreateChildById( product, "Upgrade", guid );
            Element upgradeVersion = getOrCreateChildByKeyValue( upgrade, "UpgradeVersion", "Property", "WIX_UPGRADE_DETECTED_" + i );
            addAttributeIfNotExists( upgradeVersion, "Minimum", "0.0.0.0" );
            addAttributeIfNotExists( upgradeVersion, "MigrateFeatures", "yes" );
        }
        Element executeSequence = getOrCreateChild( product, "InstallExecuteSequence" );
        Element removeExistingProducts = getOrCreateChild( executeSequence, "RemoveExistingProducts" );
        addAttributeIfNotExists( removeExistingProducts, "After", "InstallValidate" );

        Element action;

        // add a vbscript action to set the instance names
        action = getOrCreateChildById( product, "CustomAction", "SetInstanceID" );
        addAttributeIfNotExists( action, "Script", "vbscript" );
        try( Scanner scanner = new Scanner( task.getMultiInstanceScript().openStream(), "UTF8" ) ) {
            String vbscript = scanner.useDelimiter( "\\A" ).next(); // read the completely file into a string
            vbscript = vbscript.replace( "\r\n", "\n" ); // \n will be replaced with platform default characters. https://bugs.openjdk.java.net/browse/JDK-8133452
            action.setTextContent( vbscript );
        }
        addCustomActionToSequence( "SetInstanceID", false, "ExecuteAction", false, null );
        addCustomActionToSequence( "SetInstanceID", true, "ValidateProductID", false, null );

        // add a action to set the property TRANSFORM
        action = getOrCreateChildById( product, "CustomAction", "SetTransforms" );
        addAttributeIfNotExists( action, "Property", "TRANSFORMS" );
        addAttributeIfNotExists( action, "Value", ":[INSTANCE_ID]" );
        addCustomActionToSequence( "SetTransforms", false, "SetInstanceID", true, "ACTION = \"INSTALL\"" );

        // set the ProductName properties from the property PRODUCT_NAME of the vbscript
        action = getOrCreateChildById( product, "CustomAction", "SetProductName" );
        addAttributeIfNotExists( action, "Property", "ProductName" );
        addAttributeIfNotExists( action, "Value", "[PRODUCT_NAME]" );
        addCustomActionToSequence( "SetProductName", true, "SetInstanceID", true, "PRODUCT_NAME" );

        // add the registry key with instance path
        Element component = getComponent( installDir, "instance_path" );
        Element key = addRegistryKey( component, "HKLM", "instance_path_reg", "Software\\" + setup.getVendor() + "\\" + setup.getApplication() + "\\Instances\\[INSTANCE_NUMBER]" );
        addRegistryValue( key, null, "string", "[INSTALLDIR]" );
        addRegistryValue( key, "PackageCode", "string", "[PackageCode]" );
    }

    /**
     * Add pre and post scripts if any set.
     * 
     * @throws IOException can not occur
     */
    private void addPreAndPostScripts() throws IOException {
        addPreAndPostScripts( "PreGUI_Script", task.getPreGUI(), false, "PrepareDlg", false, "NOT Installed OR REINSTALL OR UPGRADINGPRODUCTCODE" );
        addPreAndPostScripts( "Preinst_Script", task.getPreinst(), true, "InstallInitialize", true, "NOT Installed OR REINSTALL OR UPGRADINGPRODUCTCODE" );
        addPreAndPostScripts( "Postinst_Script", task.getPostinst(), true, "InstallFinalize", false, "NOT Installed OR REINSTALL OR UPGRADINGPRODUCTCODE" );
        addPreAndPostScripts( "Prerm_Script", task.getPrerm(), true, "InstallInitialize", true, "REMOVE" );
        addPreAndPostScripts( "Postrm_Script", task.getPostrm(), true, "InstallFinalize", false, "REMOVE" );
    }

    /**
     * Add pre and post scripts if any set.
     * 
     * @param actionId the unique ID
     * @param scripts the scripts content
     * @param execute true, InstallExecuteSequence; false, InstallUISequence
     * @param sequenceAction the name of an existing action in sequence after which it should be added.
     * @param after true, After the action; false, Before
     * @param condition the condition under which it should run
     * @throws IOException can not occur
     */
    private void addPreAndPostScripts( String actionId, List<String> scripts, boolean execute, String sequenceAction, boolean after, String condition ) throws IOException {
        if( scripts == null || scripts.isEmpty() ) {
            return;
        }
        for( int i = 0; i < scripts.size(); i++ ) {
            String script = scripts.get( i );
            addPreAndPostScripts( actionId + i, script, execute, sequenceAction, after, condition );
        }
    }

    /**
     * Add pre and post scripts if any set.
     * 
     * @param actionId the unique ID
     * @param script the script content
     * @param execute true, InstallExecuteSequence; false, InstallUISequence
     * @param sequenceAction the name of an existing action in sequence after which it should be added.
     * @param after true, After the action; false, Before
     * @param condition the condition under which it should run
     * @throws IOException can not occur
     */
    private void addPreAndPostScripts( String actionId, String script, boolean execute, String sequenceAction, boolean after, String condition ) throws IOException {
        if( script == null || script.trim().isEmpty() ) {
            return;
        }

        Element action = getOrCreateChildById( product, "CustomAction", actionId );
        addAttributeIfNotExists( action, "Script", getScriptLanguage( script ) );
        script = script.replace( "\r\n", "\n" ); // \n will be replaced with platform default characters. https://bugs.openjdk.java.net/browse/JDK-8133452
        action.setTextContent( script );
        addCustomActionToSequence( actionId, execute, sequenceAction, after, condition );

        if( execute ) {
            addAttributeIfNotExists( action, "Execute", "deferred" );
            addAttributeIfNotExists( action, "Impersonate", "no" );

            //http://blogs.technet.com/b/alexshev/archive/2008/03/25/property-does-not-exist-or-empty-when-accessed-from-deferred-custom-action.aspx
            action = getOrCreateChildById( product, "CustomAction", "SetProperties" + actionId );
            addAttributeIfNotExists( action, "Property", actionId );
            addAttributeIfNotExists( action, "Value", "INSTALLDIR='[INSTALLDIR]';ProductCode='[ProductCode]';INSTANCE_ID='[INSTANCE_ID]'" );
            addCustomActionToSequence( "SetProperties" + actionId, execute, actionId, false, null );
        }
    }

    /**
     * Detect the script language.
     * 
     * @param script current script
     * @return "vbscript" or "jsscript"
     * @throws IOException can not occur
     */
    private static String getScriptLanguage( String script ) throws IOException {
        BufferedReader reader = new BufferedReader( new StringReader( script ) );
        do {
            String line = reader.readLine();
            if( line == null ) {
                return "vbscript";
            }
            if( line.trim().endsWith( ";" ) ) {
                return "jscript";
            }
        } while( true );
    }
}
