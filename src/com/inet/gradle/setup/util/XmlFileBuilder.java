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
package com.inet.gradle.setup.util;

import java.io.File;
import java.net.URL;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.abstracts.AbstractSetupTask;

/**
 * Build an XML.
 * @author gamma
 *
 * @param <T> The SetupBuilder Task.
 */
public class XmlFileBuilder<T extends AbstractSetupTask> {

    public final T            task;

    public final SetupBuilder setup;

    public final File         xmlFile;

    public final File         buildDir;

    public final Document     doc;

    public XmlFileBuilder( T task, SetupBuilder setup, File xmlFile, File buildDir, URL template ) throws Exception {
        this.task = task;
        this.setup = setup;
        this.xmlFile = xmlFile;
        this.buildDir = buildDir;
        xmlFile.getParentFile().mkdirs();

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setIgnoringComments( true );
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        doc = template != null ? docBuilder.parse( template.toString() ) : docBuilder.newDocument();

    }

    /**
     * Save the XML file
     * @throws Exception if any error occur
     */
    public void save() throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource( doc );
        StreamResult result = new StreamResult( xmlFile );
        transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
        transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "2" );
        transformer.transform( source, result );

    }

    /**
     * Add a attribute if not exists.
     *
     * @param el the element/node in the XML
     * @param name the name of the attribute
     * @param value the value
     */
    public void addAttributeIfNotExists( Element el, String name, String value ) {
        if( !el.hasAttribute( name ) ) {
            el.setAttribute( name, value );
        }
    }

    /**
     * Adds Text content to a node
     * @param el Node
     * @param text content
     */
    public void addNodeText( Element el, String text ) {
        Document doc = el.getOwnerDocument();
        el.appendChild( doc.createTextNode( text ) );
    }

    public Element getOrCreateChild( Node parent, String name ) {
        return getOrCreateChild( parent, name, true );
    }

    public Element getOrCreateChild( Node parent, String name, boolean append ) {
        Node first = parent.getFirstChild();
        for( Node child = first; child != null; child = child.getNextSibling() ) {
            if( name.equals( child.getNodeName() ) ) {
                return (Element)child;
            }
        }
        return createChild( parent, name, append );
    }

    /**
     * Returns or creates a child node with a text node entry only
     * @param parent the parent node to attach to
     * @param name the name of the node
     * @param textNodeValue the textnode value of the node
     * @param append if it should be appended or set in first position
     * @return the new element
     */
    public Element getOrCreateChild( Node parent, String name, String textNodeValue, boolean append ) {
        Node first = parent.getFirstChild();
        for( Node child = first; child != null; child = child.getNextSibling() ) {
            if( name.equals( child.getNodeName() ) &&
                child.getFirstChild().getNodeType() == Element.TEXT_NODE &&
                textNodeValue.equals( child.getFirstChild().getTextContent() ) ) {
                return (Element)child;
            }
        }
        Element child = createChild( parent, name, append );
        addNodeText( child, textNodeValue );
        return child;
    }

    public Element createChild( Node parent, String name, boolean append ) {
        Document doc = parent instanceof Document ? (Document)parent : parent.getOwnerDocument();
        Element child = doc.createElement( name );
        Node first = parent.getFirstChild();
        if( append || first == null ) {
            parent.appendChild( child );
        } else {
            parent.insertBefore( child, first );
        }
        return child;
    }

    public Element getOrCreateChildById( Node parent, String name, String id ) {
        return getOrCreateChildByKeyValue( parent, name, "Id", id, true );
    }

    public Element getOrCreateChildById( Node parent, String name, String id, boolean append ) {
        return getOrCreateChildByKeyValue( parent, name, "Id", id, append );
    }

    public Element getOrCreateChildByKeyValue( Node parent, String name, String key, String value ) {
        return getOrCreateChildByKeyValue( parent, name, key, value, true );
    }

    /**
     * Get or create a child element.
     *
     * @param parent the parent node in which we search and create
     * @param name The tag name of the element
     * @param key the name of an attribute, can't be null
     * @param value the value, can be null for not existing
     * @param append true, append at end of the children; false, add at top of the children
     * @return the create of find Element
     */
    public Element getOrCreateChildByKeyValue( Node parent, String name, String key, String value, boolean append ) {
        Node first = parent.getFirstChild();
        for( Node child = first; child != null; child = child.getNextSibling() ) {
            if( name.equals( child.getNodeName() ) ) {
                if( Objects.equals( value, ((Element)child).getAttribute( key ) ) || (value == null && !((Element)child).hasAttribute( key )) ) {
                    return (Element)child;
                }
            }
        }
        Document doc = parent.getOwnerDocument();
        Element child = doc.createElement( name );
        if( value != null ) {
            child.setAttribute( key, value );
        }
        if( append || first == null ) {
            parent.appendChild( child );
        } else {
            parent.insertBefore( child, first );
        }
        return child;
    }

}
