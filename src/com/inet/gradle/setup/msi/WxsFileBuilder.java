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
import java.net.URL;
import java.util.HashSet;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.internal.file.UnionFileTree;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.inet.gradle.setup.SetupBuilder;

/**
 * Builder for a *.wsx file. A *.wsx file is a XML that described MSI setup and is needed for the Wix tool.
 * 
 * @author Volker Berlin
 */
class WxsFileBuilder {

    private final Msi          msi;

    private final SetupBuilder setup;

    private final File         wxsFile;

    WxsFileBuilder( Msi msi, SetupBuilder setup, File wxsFile ) {
        this.msi = msi;
        this.setup = setup;
        this.wxsFile = wxsFile;
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
        Element programFiles = getOrCreateChildById( directory, "Directory", "ProgramFilesFolder", true );
        Element appDirectory = getOrCreateChildById( programFiles, "Directory", "INSTALLDIR", true );
        addAttributeIfNotExists( appDirectory, "Name", setup.getApplication() );

        //Files
        HashSet<String> components = new HashSet<>();
        Element appDirRef = getOrCreateChildById( product, "DirectoryRef", "INSTALLDIR", true );
        FileTree fileTree = new UnionFileTree( setup.getSource(), msi.getSource() );
        fileTree.visit( new FileVisitor() {
            @Override
            public void visitFile( FileVisitDetails details ) {
                Element parent = appDirRef;
                String[] segemnts = details.getRelativePath().getSegments();
                StringBuilder pathID = new StringBuilder();
                for( int i = 0; i < segemnts.length - 1; i++ ) {
                    String seg = segemnts[i];
                    parent = getOrCreateChildById( parent, "Directory", seg, true );
                    addAttributeIfNotExists( parent, "Name", seg );
                    pathID.append( seg ).append( '_' );
                }

                String compID = pathID + "application";
                components.add( compID );
                Element component = getOrCreateChildById( parent, "Component", compID, true );
                addAttributeIfNotExists( component, "Guid", UUID.randomUUID().toString() );
                Element fileEl = doc.createElement( "File" );
                fileEl.setAttribute( "Id", pathID + details.getName() );
                fileEl.setAttribute( "Source", details.getFile().getAbsolutePath() );
                component.appendChild( fileEl );
            }

            @Override
            public void visitDir( FileVisitDetails arg0 ) {
                //ignore
            }
        } );

        //Feature
        Element feature = getOrCreateChildById( product, "Feature", "MainApplication", true );
        for( String compID : components ) {
            Element compRef = getOrCreateChildById( feature, "ComponentRef", compID, true );
        }

        addGUI( product );

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource( doc );
        StreamResult result = new StreamResult( wxsFile );
        transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
        transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "2" );
        transformer.transform( source, result );
    }

    /**
     * Add the GUI to the Setup
     * 
     * @param product the product node in the XML.
     */
    private void addGUI( Element product ) {
        Element installdir = getOrCreateChildById( product, "Property", "WIXUI_INSTALLDIR", true );
        addAttributeIfNotExists( installdir, "Value", "INSTALLDIR" );
        Element uiRef = getOrCreateChild( product, "UIRef", true );
        addAttributeIfNotExists( uiRef, "Id", "WixUI_InstallDir" );
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
        Node first = parent.getFirstChild();
        for( Node child = first; child != null; child = child.getNextSibling() ) {
            if( name.equals( child.getNodeName() ) && id.equals( ((Element)child).getAttribute( "Id" ) ) ) {
                return (Element)child;
            }
        }
        Document doc = parent.getOwnerDocument();
        Element child = doc.createElement( name );
        child.setAttribute( "Id", id );
        if( append || first == null ) {
            parent.appendChild( child );
        } else {
            parent.insertBefore( child, first );
        }
        return child;
    }
}
