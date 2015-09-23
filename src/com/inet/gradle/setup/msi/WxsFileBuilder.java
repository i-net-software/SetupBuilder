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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.inet.gradle.setup.DesktopStarter;
import com.inet.gradle.setup.Service;
import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.image.ImageFactory;
import com.inet.gradle.setup.util.ResourceUtils;
import com.inet.gradle.setup.util.XmlFileBuilder;

/**
 * Builder for a *.wsx file. A *.wsx file is a XML that described MSI setup and is needed for the Wix tool.
 * 
 * @author Volker Berlin
 */
class WxsFileBuilder extends XmlFileBuilder<Msi> {

    private static final String     ICON_ID = "icon.ico";

    private HashSet<String>         components = new HashSet<>();

    private HashMap<String, String> ids        = new HashMap<>();

    private String                  jvmDll;

    private String                  javaDir;

    private boolean                 isAddFiles;

    /**
     * Create a new instance.
     * @param msi the MSI task
     * @param setup the SetupBuilder extension
     * @param wxsFile the file name
     * @param buildDir the temporary directory of the task
     * @param template a template file
     * @param addFiles if files shouls be added in this phase
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

        // Product node
        Element product = getOrCreateChildById( wix, "Product", "*" );
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
        addAttributeIfNotExists( packge, "Compressed", "yes" );

        // MajorUpgrade
        Element update = getOrCreateChild( product, "MajorUpgrade" );
        addAttributeIfNotExists( update, "AllowDowngrades", "yes" );

        // Directory
        Element directory = getOrCreateChildById( product, "Directory", "TARGETDIR" );
        addAttributeIfNotExists( directory, "Name", "SourceDir" );
        Element programFiles = getOrCreateChildById( directory, "Directory", task.is64Bit() ? "ProgramFiles64Folder" : "ProgramFilesFolder" );
        Element appDirectory = getOrCreateChildById( programFiles, "Directory", "INSTALLDIR" );
        addAttributeIfNotExists( appDirectory, "Name", setup.getApplication() );

        //Files
        Element installDir = getOrCreateChildById( product, "DirectoryRef", "INSTALLDIR" );
        task.processFiles( new CopyActionProcessingStreamAction() {
            @Override
            public void processFile( FileCopyDetailsInternal details ) {
                if( !details.isDirectory() ) {
                    String[] segments = details.getRelativePath().getSegments();
                    addFile( installDir, details.getFile(), segments );
                }
            }
        } );

        addBundleJre( installDir );
        addGUI( product );
        addIcon( product );
        addServices( installDir );
        addShortcuts( product, installDir );
        addRunAfter( product );
        addDeleteFiles( installDir );

        //Feature
        Element feature = getOrCreateChildById( product, "Feature", "MainApplication" );
        for( String compID : components ) {
            getOrCreateChildById( feature, "ComponentRef", compID );
        }

        save();
    }

    /**
     * Get or create a directory node.
     * 
     * @param installDir The XML element of the install directory.
     * @param segments the segments of the path in the target. The last segment contains the file name.
     * @return the directory node
     */
    private Element getDirectory( Element installDir, String[] segments ) {
        return getDirectory( installDir, segments, segments.length - 1 );
    }

    /**
     * Get or create a directory node.
     * 
     * @param installDir The XML element of the install directory.
     * @param segments the segments of the path in the target. The last segment contains the file name.
     * @param length the used length from the segments
     * @return the directory node
     */
    private Element getDirectory( Element installDir, String[] segments, int length ) {
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
        return component;
    }

    /**
     * Add a file to the setup
     * 
     * @param installDir The XML element of the install directory.
     * @param file the file to add.
     * @param segments the segments of the path in the target. The last segment contains the file name.
     */
    private void addFile( Element installDir, File file, String[] segments ) {
        Element parent = getDirectory( installDir, segments );

        String pathID = id( segments, segments.length - 1 );
        String compID = id( pathID + "_Comp");
        Element component = getComponent( parent, compID );

        String name = segments[segments.length - 1];
        addFile( component, file, pathID, name );

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
    }

    /**
     * Add a file.
     * 
     * @param component the parent component node
     * @param file the source file
     * @param pathID an ID of the parent path
     * @param name the target file name
     */
    private void addFile( Element component, File file, String pathID, String name ) {
        addFile( component, file, pathID, name, isAddFiles );
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
    private void addFile( Element component, File file, String pathID, String name, boolean isAddFiles ) {
        if( isAddFiles ) {
            String id = id( pathID + '_' + name );
            Element fileEl = getOrCreateChildById( component, "File", id );
            addAttributeIfNotExists( fileEl, "Source", file.getAbsolutePath() );
            addAttributeIfNotExists( fileEl, "Name", name );
        } else {
            getOrCreateChild( component, "CreateFolder" );
        }
    }

    /**
     * Add all files in a directory.
     * 
     * @param installDir The XML element of the install directory.
     * @param dir the source directory
     * @param baseLength the base length of the directory. This length will be cut from the absolute path.
     * @param target the target directory
     */
    private void addDirectory( Element installDir, File dir, int baseLength, String target ) {
        for( File file : dir.listFiles() ) {
            if( file.isDirectory() ) {
                addDirectory( installDir, file, baseLength, target );
            } else {
                String name = file.getAbsolutePath().substring( baseLength );
                addFile( installDir, file, segments( (target + name) ) );
            }
        }
    }

    /**
     * Bundle a JRE if setup.
     * 
     * @param installDir referent to INSTALLDIR
     */
    private void addBundleJre( Element installDir ) {
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
            List<File> versions = getDirectories( java, "jre" + jre );
            if( versions.size() == 0 ) {
                // if the java version is like "1.8" then search also for "jre8"
                String jreStr = jre.toString();
                if( jreStr.length() > 2 && jreStr.startsWith( "1." ) ) {
                    versions = getDirectories( java, "jre" + jreStr.substring( 2 ) );
                }
                if( versions.size() == 0 ) {
                    throw new GradleException( "bundleJre version " + jre + " can not be found in: '" + java + "' Its search for an folder that starts with: jre" + jre );
                }
            }
            Collections.sort( versions );
            jreDir = versions.get( versions.size() - 1 );
        }

        task.getProject().getLogger().lifecycle( "\tbundle jre: " + jreDir );

        int baseLength = jreDir.getAbsolutePath().length();
        javaDir = setup.getBundleJreTarget().replace( '/', '\\' );
        if( javaDir.endsWith( "\\" ) ) {
            baseLength++;
        }

        addDirectory( installDir, jreDir, baseLength, javaDir );
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
     * @param product the product node in the XML.
     * @throws Exception if any exception occur
     */
    private void addGUI( Element product ) throws Exception {
        Element installdir = getOrCreateChildById( product, "Property", "WIXUI_INSTALLDIR" );
        addAttributeIfNotExists( installdir, "Value", "INSTALLDIR" );
        Element uiRef = getOrCreateChild( product, "UIRef" );
        addAttributeIfNotExists( uiRef, "Id", "WixUI_InstallDir" );

        boolean isLicense = addLicense( product );
        if( !isLicense ) {
            // skip license dialog because we does no hat a license text
            Document doc = product.getOwnerDocument();
            Element ui = getOrCreateChild( product, "UI" );
            Element child = doc.createElement( "Publish" );
            child.setAttribute( "Dialog", "WelcomeDlg" );
            child.setAttribute( "Control", "Next" );
            child.setAttribute( "Event", "NewDialog" );
            child.setAttribute( "Value", "InstallDirDlg" );
            child.setAttribute( "Order", "2" );
            child.setTextContent( "1" );
            ui.appendChild( child );

            child = doc.createElement( "Publish" );
            child.setAttribute( "Dialog", "InstallDirDlg" );
            child.setAttribute( "Control", "Back" );
            child.setAttribute( "Event", "NewDialog" );
            child.setAttribute( "Value", "WelcomeDlg" );
            child.setAttribute( "Order", "2" );
            child.setTextContent( "1" );
            ui.appendChild( child );
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
     * @param product the product node in the XML.
     * @param appDirRef
     * @throws IOException if an error occur on reading the image files
     */
    private void addIcon( Element product ) throws IOException {
        File iconFile = ImageFactory.getImageFile( task.getProject(), setup.getIcons(), buildDir, "ico" );
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
        File license = setup.getLicenseFile();
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
     * @param installDir referent to INSTALLDIR
     * @throws IOException if an I/O error occurs when reading or writing
     */
    private void addServices( Element installDir ) throws IOException {
        List<Service> services = setup.getServices();
        if( services == null || services.isEmpty() ) {
            return;
        }

        File prunsrv = ResourceUtils.extract( getClass(), task.getArch() + "/prunsrv.exe", buildDir );
        File prunmgr = ResourceUtils.extract( getClass(), "x86/prunmgr.exe", buildDir );

        for( Service service : services ) {
            String name = service.getName();
            String exe = service.getBaseName() + ".exe";
            String id = id( name.replace( '-', '_' ) ) + "_service";

            // add the service file
            String[] segments = segments( exe );
            Element directory = getDirectory( installDir, segments );
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
            addAttributeIfNotExists( install, "Arguments", " //RS//" + name );

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
            addRegistryValue( regkey, "JavaHome", "string", "[INSTALLDIR]" + setup.getBundleJreTarget() );
            addRegistryValue( regkey, "Jvm", "string", "[INSTALLDIR]" + jvmDll );
            regkey = addRegistryKey( component, "HKLM", id + "_RegStart", baseKey + name + "\\Parameters\\Start" );
            addRegistryValue( regkey, "Class", "string", service.getMainClass() );
            addRegistryValue( regkey, "Mode", "string", "Java" );
            addRegistryValue( regkey, "WorkingPath", "string", "[INSTALLDIR]" );

            // start the service
            if( service.isStartOnBoot() ) {
                Element start = getOrCreateChildById( component, "ServiceControl", id + "_start" );
                addAttributeIfNotExists( start, "Name", name );
                addAttributeIfNotExists( start, "Start", "install" );
                addAttributeIfNotExists( start, "Stop", "both" );
                addAttributeIfNotExists( start, "Remove", "uninstall" );
            }

            addFile( component, prunmgr, id + "GUI", name + ".exe", true );

            // delete log files on uninstall
            int idx = exe.replace( '\\', '/' ).lastIndexOf( '/' );
            addDeleteFiles( exe.substring( 0, idx + 1 ) + "commons-daemon.*.log", installDir );
        }
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
     * @param product the node of the product
     * @param installDir referent to INSTALLDIR
     * @throws IOException If any I/O exception occur on icon loading
     */
    private void addShortcuts( Element product, Element installDir ) throws IOException {
        List<DesktopStarter> starters = setup.getDesktopStarters();
        if( starters.isEmpty() ) {
            return;
        }

        for( DesktopStarter starter : starters ) {
            Element component = getShortcutComponent( starter, product );
            String id = id( starter.getLocation() + "_" + starter.getName() );
            Element shortcut = getOrCreateChildById( component, "Shortcut", id );
            addAttributeIfNotExists( shortcut, "Name", starter.getName() );
            addAttributeIfNotExists( shortcut, "Description", starter.getDescription() );

            // find the ID of the working directory
            String workDir = starter.getWorkDir();
            if( workDir != null && !workDir.isEmpty() ) {
                String[] segments = segments( workDir );
                Element dir = getDirectory( installDir, segments, segments.length );
                workDir = dir.getAttribute( "Id" );
            } else {
                workDir = "INSTALLDIR";
            }

            addAttributeIfNotExists( shortcut, "WorkingDirectory", workDir );

            // find the optional id for an icon
            String target = starter.getExecutable();
            String iconID;
            if( starter.getIcons() != null ) {
                if( starter.getIcons().equals( setup.getIcons() ) ) {
                    iconID = ICON_ID;
                } else {
                    File iconFile = ImageFactory.getImageFile( task.getProject(), starter.getIcons(), buildDir, "ico" );
                    iconID = id( starter.getName() + ".ico" );
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

            String[] cmd = getCommandLine( starter );
            target = cmd[0];
            String arguments = cmd[1];
            addAttributeIfNotExists( shortcut, "Target", target );
            if( !arguments.isEmpty() ) {
                addAttributeIfNotExists( shortcut, "Arguments", arguments );
            }
            if( iconID != null ) {
                addAttributeIfNotExists( shortcut, "Icon", iconID );
            }
        }
    }

    /**
     * Get the command of the starter without arguments
     * 
     * @param starter the DesktopStarter
     * @return [0] - target; [1] - arguments; [2] - both
     */
    private String[] getCommandLine( DesktopStarter starter ) {
        String target = starter.getExecutable();
        String arguments = starter.getStartArguments();
        if( target == null || target.isEmpty() ) {
            if( javaDir != null ) {
                target = "[INSTALLDIR]" + javaDir + "\\bin\\javaw.exe";
                arguments = "-cp " + starter.getMainJar() + " " + starter.getMainClass() + " " + arguments;
            } else {
                target = "cmd.exe";
                arguments = "/C javaw.exe -cp \"[INSTALLDIR]" + starter.getMainJar() + "\" " + starter.getMainClass() + " " + arguments;
            }
        } else {
            target = "[INSTALLDIR]" + target;
        }
        return new String[] { target, arguments, '\"' +target + "\" " + arguments };
    }

    /**
     * Add an action that is executed after the setup.
     * 
     * @param product the product node in the XML.
     */
    private void addRunAfter( Element product ) {
        if( setup.getRunAfter() == null ) {
            return;
        }
        String[] cmd = getCommandLine( setup.getRunAfter() );
        Element action = getOrCreateChildById( product, "CustomAction", "runAfter" );
        if( cmd[1].isEmpty() ) {
            Element target = getOrCreateChildById( product, "Property", "WixShellExecTarget" );
            addAttributeIfNotExists( target, "Value", cmd[0] );
            addAttributeIfNotExists( action, "BinaryKey", "WixCA" );
            addAttributeIfNotExists( action, "DllEntry", "WixShellExec" );
        } else {
            addAttributeIfNotExists( action, "Directory", "INSTALLDIR" );
            addAttributeIfNotExists( action, "ExeCommand", cmd[2] );
            addAttributeIfNotExists( action, "Return", "asyncNoWait" );
        }

        Element ui = getOrCreateChild( product, "UI" );
        Element exitDialog = getOrCreateChildByKeyValue( ui, "Publish", "Dialog", "ExitDialog" );
        addAttributeIfNotExists( exitDialog, "Control", "Finish" );
        addAttributeIfNotExists( exitDialog, "Event", "DoAction" );
        addAttributeIfNotExists( exitDialog, "Value", "runAfter" );
        exitDialog.setTextContent( "NOT Installed OR REINSTALL OR UPGRADINGPRODUCTCODE" );
    }

    /**
     * Add files to delete if there any define in SetupBuilder.
     * 
     * @param installDir referent to INSTALLDIR
     */
    private void addDeleteFiles( Element installDir ) {
        if( setup.getDeleteFiles().isEmpty() ) {
            return;
        }
        for( String pattern : setup.getDeleteFiles() ) {
            addDeleteFiles( pattern, installDir );
        }
    }

    /**
     * Add files to delete.
     * 
     * @param pattern the pattern to delete
     * @param installDir referent to INSTALLDIR
     */
    private void addDeleteFiles( String pattern, Element installDir ) {
        String[] segments = segments( pattern );
        Element dir = getDirectory( installDir, segments );
        String id = id( pattern );
        Element component = getComponent( dir, "deleteFiles" + id );
        Element remove = getOrCreateChildById( component, "RemoveFile", "removeFile" + id );
        addAttributeIfNotExists( remove, "On", "both" );
        addAttributeIfNotExists( remove, "Name", segments[segments.length - 1] );
    }

    /**
     * Add a registry key.
     * @param component parent component
     * @param root The root of the key like HKLM, HKCU, HKMU
     * @param id the id for the key
     * @param key the key
     * @return the element to add values
     */
    private Element addRegistryKey( Element component, String root, String id, String key ){
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
     * @param name the value name
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
     * Split a large property into multiple sub properties to handle the length limit.
     * 
     * @param product the product element
     * @param id the if of the property
     * @param value the value of the property
     */
//    private void addLargeProperty( Element product, String id, final String value ) {
//        int count = 0;
//        String rest = value;
//        String partValue;
//        String after = "InstallFiles";
//        do {
//            String propID;
//            if( rest.length() < 200 ) {
//                propID = id;
//                partValue = rest;
//            } else {
//                propID = id + count;
//                partValue = rest.substring( 0, 200 );
//                int brackets = 0;
//                int lastSeroPosition = 0;
//                // search for place holder
//                for( int i = 0; i < partValue.length(); i++ ) {
//                    char ch = partValue.charAt( i );
//                    switch( ch ) {
//                        case '[':
//                            if( brackets == 0 ) {
//                                lastSeroPosition = i;
//                            }
//                            brackets++;
//                            break;
//                        case ']':
//                            brackets--;
//                            if( brackets == 0 ) {
//                                lastSeroPosition = i + 1;
//                            }
//                            break;
//                    }
//                }
//                if( brackets != 0 ) {
//                    partValue = partValue.substring( 0, lastSeroPosition );
//                }
//                if( partValue.length() == 0 ) {
//                    throw new GradleException( "Invalid property value: " + value );
//                }
//                rest = '[' + propID + ']' + rest.substring( partValue.length() );
//            }
//            String actionID = "action." + propID;
//            Element action = getOrCreateChildById( product, "CustomAction", actionID );
//            addAttributeIfNotExists( action, "Property", propID );
//            addAttributeIfNotExists( action, "Value", partValue );
//            Element executeSequence = getOrCreateChild( product, "InstallExecuteSequence" );
//            Element custom = getOrCreateChildByKeyValue( executeSequence, "Custom", "Action", actionID );
//            addAttributeIfNotExists( custom, "After", after );
//            after = actionID;
//            count++;
//        } while( partValue != rest );
//    }

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
            if( str.length() > 72 ) {
                builder = new StringBuilder();
            } else {
                return str;
            }
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
    String getGuid( String id ) {
        return UUID.nameUUIDFromBytes( (setup.getVendor() + setup.getApplication() + id).getBytes() ).toString();
    }
}
