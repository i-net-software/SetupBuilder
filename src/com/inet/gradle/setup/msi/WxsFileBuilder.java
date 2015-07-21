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
import java.util.ArrayList;
import java.util.Collections;
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

    private HashSet<String>    components = new HashSet<>();

    private String             jvmDll;

    WxsFileBuilder( Msi msi, SetupBuilder setup, File wxsFile, File buildDir ) throws Exception {
        super( msi, setup, wxsFile, buildDir, WxsFileBuilder.class.getResource( "template.wxs" ) );
    }

    /**
     * Create *.wxs file based on the settings in the task.
     * 
     * @throws ParserConfigurationException
     * @throws Exception
     */
    void build() throws Exception {
        // Wix node
        Element wix = (Element)doc.getFirstChild();
        if( !"Wix".equals( wix.getTagName() ) ) {
            throw new IllegalArgumentException( "Template does not contains a Wix root: " + wix.getTagName() );
        }

        // Product node
        Element product = getOrCreateChildById( wix, "Product", "*", false );
        addAttributeIfNotExists( product, "Language", "1033" );
        addAttributeIfNotExists( product, "Manufacturer", setup.getVendor() );
        addAttributeIfNotExists( product, "Name", setup.getApplication() );
        addAttributeIfNotExists( product, "Version", setup.getVersion() );
        addAttributeIfNotExists( product, "UpgradeCode", UUID.randomUUID().toString() );

        // Package node
        Element media = getOrCreateChildById( product, "Media", "1", false );
        addAttributeIfNotExists( media, "Cabinet", "media1.cab" );
        addAttributeIfNotExists( media, "EmbedCab", "yes" );
        Element packge = getOrCreateChild( product, "Package", false );
        addAttributeIfNotExists( packge, "Compressed", "yes" );

        // Directory
        Element directory = getOrCreateChildById( product, "Directory", "TARGETDIR", true );
        addAttributeIfNotExists( directory, "Name", "SourceDir" );
        Element programFiles = getOrCreateChildById( directory, "Directory", task.is64Bit() ? "ProgramFiles64Folder" : "ProgramFilesFolder", true );
        Element appDirectory = getOrCreateChildById( programFiles, "Directory", "INSTALLDIR", true );
        addAttributeIfNotExists( appDirectory, "Name", setup.getApplication() );

        //Files
        Element installDir = getOrCreateChildById( product, "DirectoryRef", "INSTALLDIR", true );
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
        addServices( product, installDir );

        //Feature
        Element feature = getOrCreateChildById( product, "Feature", "MainApplication", true );
        for( String compID : components ) {
            Element compRef = getOrCreateChildById( feature, "ComponentRef", compID, true );
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
        Element parent = installDir;
        for( int i = 0; i < segments.length - 1; i++ ) {
            String seg = segments[i];
            parent = getOrCreateChildById( parent, "Directory", seg, true );
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
        Element component = getOrCreateChildById( dir, "Component", compID, true );
        addAttributeIfNotExists( component, "Guid", UUID.randomUUID().toString() );
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

        String pathID = segments.length > 1 ? segments[segments.length - 2] : "";
        String compID = "application_" + pathID;
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
        Document doc = component.getOwnerDocument();
        Element fileEl = doc.createElement( "File" );
        String id = pathID + '_' + id( name );
        fileEl.setAttribute( "Id", id );
        fileEl.setAttribute( "Source", file.getAbsolutePath() );
        fileEl.setAttribute( "Name", name );
        component.appendChild( fileEl );
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
                addFile( installDir, file, (target + name).split( "\\\\" ) );
            }
        }
    }

    /**
     * Bundle a JRE if setup.
     * 
     * @param appDirRef the
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
            ArrayList<File> versions = new ArrayList<File>();
            String javaVersion = "jre" + jre;
            for( File file : java.listFiles() ) {
                if( file.isDirectory() && file.getName().startsWith( javaVersion ) ) {
                    versions.add( file );
                }
            }
            if( versions.size() == 0 ) {
                throw new GradleException( "bundleJre version " + jre + " can not be found in: " + java );
            }
            Collections.sort( versions );
            jreDir = versions.get( versions.size() - 1 );
        }

        task.getProject().getLogger().lifecycle( "\tbundle jre: " + jreDir );

        int baseLength = jreDir.getAbsolutePath().length();
        String target = setup.getBundleJreTarget().replace( '/', '\\' );
        if( target.endsWith( "\\" ) ) {
            baseLength++;
        }

        addDirectory( installDir, jreDir, baseLength, target );
    }

    /**
     * Add the GUI to the Setup.
     * 
     * @param product the product node in the XML.
     * @throws Exception if any exception occur
     */
    private void addGUI( Element product ) throws Exception {
        Element installdir = getOrCreateChildById( product, "Property", "WIXUI_INSTALLDIR", true );
        addAttributeIfNotExists( installdir, "Value", "INSTALLDIR" );
        Element uiRef = getOrCreateChild( product, "UIRef", true );
        addAttributeIfNotExists( uiRef, "Id", "WixUI_InstallDir" );

        boolean isLicense = addLicense( product );
        if( !isLicense ) {
            // skip license dialog because we does no hat a license text
            Document doc = product.getOwnerDocument();
            Element ui = getOrCreateChild( product, "UI", true );
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
            Element licenseNode = getOrCreateChildById( product, "WixVariable", "WixUIBannerBmp", true );
            addAttributeIfNotExists( licenseNode, "Value", file.getAbsolutePath() );
        }
        file = task.getDialogBmp();
        if( file != null ) {
            Element licenseNode = getOrCreateChildById( product, "WixVariable", "WixUIDialogBmp", true );
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
        Element icon = getOrCreateChildById( product, "Icon", "icon.ico", true );
        addAttributeIfNotExists( icon, "SourceFile", iconFile.getAbsolutePath() );
        Element appProduction = getOrCreateChildById( product, "Property", "ARPPRODUCTICON", true );
        addAttributeIfNotExists( appProduction, "Value", "icon.ico" );
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

        Element licenseNode = getOrCreateChildById( product, "WixVariable", "WixUILicenseRtf", true );
        addAttributeIfNotExists( licenseNode, "Value", license.getAbsolutePath() );
        return true;
    }

    /**
     * Add the windows services.
     * 
     * @param product
     * @throws IOException if an I/O error occurs when reading or writing
     */
    private void addServices( Element product, Element installDir ) throws IOException {
        List<Service> services = setup.getServices();
        if( services == null || services.isEmpty() ) {
            return;
        }

        File prunsrv = ResourceUtils.extract( getClass(), task.getArch() + "/prunsrv.exe", buildDir );

        for( Service service : services ) {
            String name = service.getName();
            String exe = name + "Service.exe";
            String id = id( name.replace( '-', '_' ) ) + "_service";

            // add the service file
            Element directory = getDirectory( installDir, new String[] { exe } );
            Element component = getComponent( directory, id );
            addFile( component, prunsrv, id, exe );

            // install the windows service
            Element install = getOrCreateChildById( component, "ServiceInstall", id + "_install", true );
            addAttributeIfNotExists( install, "Name", name );
            addAttributeIfNotExists( install, "DisplayName", service.getDisplayName() );
            addAttributeIfNotExists( install, "Description", service.getDescription() );
            addAttributeIfNotExists( install, "Start", service.isStartOnBoot() ? "auto" : "demand" );
            addAttributeIfNotExists( install, "Type", "ownProcess" );
            addAttributeIfNotExists( install, "ErrorControl", "normal" );
            addAttributeIfNotExists( install, "Arguments", " //RS//" + name );

            // Java parameter of the service
            String baseKey =
                            task.is64Bit() ? "SOFTWARE\\Wow6432Node\\Apache Software Foundation\\ProcRun 2.0\\"
                                            : "SOFTWARE\\Apache Software Foundation\\ProcRun 2.0\\";
            Element regkey = getOrCreateChildById( component, "RegistryKey", id + "_RegJava", true );
            addAttributeIfNotExists( regkey, "Root", "HKLM" );
            addAttributeIfNotExists( regkey, "Key", baseKey + name + "\\Parameters\\Java" );
            addAttributeIfNotExists( regkey, "ForceDeleteOnUninstall", "yes" );
            addRegistryValue( regkey, "Classpath", "string", service.getMainJar() );
            addRegistryValue( regkey, "JavaHome", "string", "[INSTALLDIR]" + setup.getBundleJreTarget() );
            addRegistryValue( regkey, "Jvm", "string", "[INSTALLDIR]" + jvmDll );
            regkey = getOrCreateChildById( component, "RegistryKey", id + "_RegStart", true );
            addAttributeIfNotExists( regkey, "Root", "HKLM" );
            addAttributeIfNotExists( regkey, "Key", baseKey + name + "\\Parameters\\Start" );
            addAttributeIfNotExists( regkey, "ForceDeleteOnUninstall", "yes" );
            addRegistryValue( regkey, "Class", "string", service.getMainClass() );
            addRegistryValue( regkey, "Mode", "string", "Java" );
            addRegistryValue( regkey, "WorkingPath", "string", "[INSTALLDIR]" );

            // start the service
            if( service.isStartOnBoot() ) {
                Element start = getOrCreateChildById( component, "ServiceControl", id + "_start", true );
                addAttributeIfNotExists( start, "Name", name );
                addAttributeIfNotExists( start, "Start", "install" );
                addAttributeIfNotExists( start, "Stop", "both" );
                addAttributeIfNotExists( start, "Remove", "uninstall" );
            }
        }
    }

    /**
     * Add a registry value.
     * 
     * @param regkey the parent registry key
     * @param name the value name
     * @param type the type
     * @param value the value
     */
    private void addRegistryValue( Element regkey, String name, String type, String value ) {
        Element regValue = getOrCreateChildByKeyValue( regkey, "RegistryValue", "Name", name, true );
        addAttributeIfNotExists( regValue, "Type", type );
        addAttributeIfNotExists( regValue, "Value", value );
    }

    /**
     * Split a large property into multiple sub properties to handle the length limit.
     * 
     * @param product the product element
     * @param id the if of the property
     * @param value the value of the property
     */
    private void addLargeProperty( Element product, String id, final String value ) {
        int count = 0;
        String rest = value;
        String partValue;
        String after = "InstallFiles";
        do {
            String propID;
            if( rest.length() < 200 ) {
                propID = id;
                partValue = rest;
            } else {
                propID = id + count;
                partValue = rest.substring( 0, 200 );
                int brackets = 0;
                int lastSeroPosition = 0;
                // search for place holder
                for( int i = 0; i < partValue.length(); i++ ) {
                    char ch = partValue.charAt( i );
                    switch( ch ) {
                        case '[':
                            if( brackets == 0 ) {
                                lastSeroPosition = i;
                            }
                            brackets++;
                            break;
                        case ']':
                            brackets--;
                            if( brackets == 0 ) {
                                lastSeroPosition = i + 1;
                            }
                            break;
                    }
                }
                if( brackets != 0 ) {
                    partValue = partValue.substring( 0, lastSeroPosition );
                }
                if( partValue.length() == 0 ) {
                    throw new GradleException( "Invalid property value: " + value );
                }
                rest = '[' + propID + ']' + rest.substring( partValue.length() );
            }
            String actionID = "action." + propID;
            Element action = getOrCreateChildById( product, "CustomAction", actionID, true );
            addAttributeIfNotExists( action, "Property", propID );
            addAttributeIfNotExists( action, "Value", partValue );
            Element executeSequence = getOrCreateChild( product, "InstallExecuteSequence", true );
            Element custom = getOrCreateChildByKeyValue( executeSequence, "Custom", "Action", actionID, true );
            addAttributeIfNotExists( custom, "After", after );
            after = actionID;
            count++;
        } while( partValue != rest );
    }

    /**
     * Create a valid id from string for the wxs file.
     * 
     * @param str possible id but with possible invalid characters
     * @return a valid id
     */
    private String id( String str ) {
        StringBuilder builder = null;
        for( int i = 0; i < str.length(); i++ ) {
            char ch = str.charAt( i );
            if( (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || (ch == '_') || (ch == '.') ) {
                continue;
            }
            if( builder == null ) {
                builder = new StringBuilder();
            }
            builder.append( str.substring( builder.length(), i ) );
            builder.append( '_' );
        }
        if( builder == null ) {
            return str;
        }
        builder.append( str.substring( builder.length() ) );
        builder.append( '_' );
        builder.append( Math.abs( str.hashCode() ) );//add a hashcode to prevent name collision
        return builder.toString();
    }
}
