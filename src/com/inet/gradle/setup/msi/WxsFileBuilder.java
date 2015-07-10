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
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.gradle.api.GradleException;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.internal.file.CopyActionProcessingStreamAction;
import org.gradle.api.internal.file.UnionFileTree;
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.inet.gradle.setup.Service;
import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.image.ImageFactory;

/**
 * Builder for a *.wsx file. A *.wsx file is a XML that described MSI setup and is needed for the Wix tool.
 * 
 * @author Volker Berlin
 */
class WxsFileBuilder {

    private final Msi          task;

    private final SetupBuilder setup;

    private final File         wxsFile;

    private File               buildDir;

    private HashSet<String>    components = new HashSet<>();

    private String             jvmDll;

    WxsFileBuilder( Msi msi, SetupBuilder setup, File wxsFile, File buildDir ) {
        this.task = msi;
        this.setup = setup;
        this.wxsFile = wxsFile;
        this.buildDir = buildDir;
    }

    /**
     * Create *.wxs file based on the settings in the task.
     * 
     * @throws ParserConfigurationException
     * @throws Exception
     */
    void build() throws Exception {
        wxsFile.getParentFile().mkdirs();

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        URL url = getClass().getResource( "template.wxs" );
        Document doc = docBuilder.parse( url.toString() );

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

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource( doc );
        StreamResult result = new StreamResult( wxsFile );
        transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
        transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "2" );
        transformer.transform( source, result );
    }

    /**
     * Add a file to the setup
     * 
     * @param installDir The XML element of the install directory.
     * @param file the file to add.
     * @param segments the segments of the path in the target. The last segment contains the file name.
     * @return id of the file
     */
    private String addFile( Element installDir, File file, String[] segments ) {
        Document doc = installDir.getOwnerDocument();
        Element parent = installDir;
        StringBuilder pathID = new StringBuilder();
        for( int i = 0; i < segments.length - 1; i++ ) {
            String seg = segments[i];
            parent = getOrCreateChildById( parent, "Directory", seg, true );
            addAttributeIfNotExists( parent, "Name", seg );
            pathID.append( seg ).append( '_' );
        }

        String compID = pathID + "application";
        components.add( compID );
        Element component = getOrCreateChildById( parent, "Component", compID, true );
        addAttributeIfNotExists( component, "Guid", UUID.randomUUID().toString() );
        Element fileEl = doc.createElement( "File" );
        String name = segments[segments.length - 1];
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
        String id = pathID + id( name );
        fileEl.setAttribute( "Id", id );
        fileEl.setAttribute( "Source", file.getAbsolutePath() );
        fileEl.setAttribute( "Name", name );
        component.appendChild( fileEl );
        return id;
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

        File prunsrv = new File( buildDir, "prunsrv.exe" );
        try( InputStream input = getClass().getResourceAsStream( task.getArch() + "/prunsrv.exe" ) ) {
            Files.copy( input, prunsrv.toPath(), StandardCopyOption.REPLACE_EXISTING );
        }

        Element executeSequence = getOrCreateChild( product, "InstallExecuteSequence", true );

        for( Service service : services ) {
            String name = service.getName();
            String exe = name + "Service.exe";
            String id = id( name.replace( '-', '_' ) ) + "_service";
            String fileKey = addFile( installDir, prunsrv, new String[] { exe } );

            // Build the service install command line
            StringBuilder cmd = new StringBuilder();
            cmd.append( " install " ).append( name );
            if( service.getDescription() != null ) {
                cmd.append( " \"--Description=" ).append( service.getDescription() ).append( '\"' );
            }
            cmd.append( " \"--DisplayName=" ).append( service.getDisplayName() ).append( '\"' );
            if( setup.getBundleJre() != null ) {
                cmd.append( " \"--Jvm=" ).append( setup.getBundleJreTarget() ).append( "[INSTALLDIR]" ).append( jvmDll ).append( '\"' );
            }
            cmd.append( " \"--Classpath=" ).append( "[INSTALLDIR]" ).append( service.getMainJar() ).append( '\"' );
            cmd.append( " \"--StartClass=" ).append( service.getMainClass() ).append( '\"' );
            task.getProject().getLogger().lifecycle( "\t" + cmd );

            // save the command line as property
            String propID = "prop." + id;
            addLargeProperty( product, propID, cmd.toString() );

            Element customAction = getOrCreateChildById( product, "CustomAction", id, true );
            addAttributeIfNotExists( customAction, "FileKey", fileKey );
            addAttributeIfNotExists( customAction, "ExeCommand", '[' + propID + ']' );
            addAttributeIfNotExists( customAction, "Execute", "deferred" );
            addAttributeIfNotExists( customAction, "Impersonate", "no" );

            Element custom = getOrCreateChildByKeyValue( executeSequence, "Custom", "Action", id, true );
            addAttributeIfNotExists( custom, "Before", "InstallFinalize" );
        }
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
     * Add a attribute if not exists.
     * 
     * @param el the element/node in the XML
     * @param name the name of the attribute
     * @param value the value
     */
    private void addAttributeIfNotExists( Element el, String name, String value ) {
        if( !el.hasAttribute( name ) ) {
            el.setAttribute( name, value );
        }
    }

    private Element getOrCreateChild( Element parent, String name, boolean append ) {
        Node first = parent.getFirstChild();
        for( Node child = first; child != null; child = child.getNextSibling() ) {
            if( name.equals( child.getNodeName() ) ) {
                return (Element)child;
            }
        }
        Document doc = parent.getOwnerDocument();
        Element child = doc.createElement( name );
        if( append || first == null ) {
            parent.appendChild( child );
        } else {
            parent.insertBefore( child, first );
        }
        return child;
    }

    private Element getOrCreateChildById( Element parent, String name, String id, boolean append ) {
        return getOrCreateChildByKeyValue( parent, name, "Id", id, append );
    }

    private Element getOrCreateChildByKeyValue( Element parent, String name, String key, String value, boolean append ) {
        Node first = parent.getFirstChild();
        for( Node child = first; child != null; child = child.getNextSibling() ) {
            if( name.equals( child.getNodeName() ) && value.equals( ((Element)child).getAttribute( key ) ) ) {
                return (Element)child;
            }
        }
        Document doc = parent.getOwnerDocument();
        Element child = doc.createElement( name );
        child.setAttribute( key, value );
        if( append || first == null ) {
            parent.appendChild( child );
        } else {
            parent.insertBefore( child, first );
        }
        return child;
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
