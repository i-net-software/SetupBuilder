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
package com.inet.gradle.setup.abstracts;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains content for registering file extensions.
 * 
 * @author Volker
 */
public class DocumentType {

    private final AbstractSetupBuilder setup;

    private List<String> extensions;

    private String name;

    private String mimetype;

    private String role = "Viewer";

    private Object icons;

    DocumentType( AbstractSetupBuilder setup ) {
        this.setup = setup;
    }

    /**
     * Add one file extensions.
     * 
     * @param extension the extension
     */
    public void fileExtension( String extension ) {
        if( extension.startsWith( "*." ) ) {
            extension = extension.substring( 2 );
        }
        if( extensions == null ) {
            extensions = new ArrayList<>();
        }
        extensions.add( extension );
    }

    /**
     * Add multiple file extensions.
     * 
     * @param extensions the extensions
     */
    public void fileExtension( List<String> extensions ) {
        for( String extension : extensions ) {
            fileExtension( extension );
        }
    }

    /**
     * Add one file extensions.
     * 
     * @param extension the extension
     */
    /**
     * Sets a single file extension.
     * @param extension the file extension
     */
    public void setFileExtension( String extension ) {
        extensions = null;
        fileExtension( extension );
    }

    /**
     * Gets the file extensions.
     * @return the list of file extensions
     */
    public List<String> getFileExtension() {
        return extensions;
    }

    /**
     * Set multiple file extensions.
     * 
     * @param extensions the extensions
     */
    public void setFileExtension( List<String> extensions ) {
        this.extensions = extensions;
    }

    /**
     * Gets the document type name.
     * @return the document type name, or a default name if not set
     */
    public String getName() {
        if( name != null && !name.isEmpty() ) {
            return name;
        }
        return setup.getProject().getName() + " file";
    }

    /**
     * Sets the document type name.
     * @param name the document type name
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * Returns the mime type for the document type. If none mime type was specified it will return 'application/&lt;the first extension&gt;'.
     * @return the mime type for the document type
     */
    public String getMimetype() {
        if( mimetype != null && mimetype.trim().length() > 0) {
            return mimetype;            
        } else {
            List<String> ext = getFileExtension();
            return "application/" + ext.get( 0 );
        }
    }

    /**
     * Sets the mime type of the document type
     * @param mimetype the mime type
     */
    public void setMimetype( String mimetype ) {
        this.mimetype = mimetype;
    }

    /**
     * Gets the document role.
     * @return the document role
     */
    public String getRole() {
        return role;
    }

    /**
     * Set the role for OSX.
     * 
     * @param role the role
     */
    public void setRole( String role ) {
        this.role = role;
    }

    /**
     * Gets the document icons.
     * @return the icons, or the setup builder icons if not set
     */
    public Object getIcons() {
        if( icons != null ) {
            return icons;
        }
        return setup.getIcons();
    }

    /**
     * Sets the document icons.
     * @param icons the icons
     */
    public void setIcons( Object icons ) {
        this.icons = icons;
    }

}
