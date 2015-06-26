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
package com.inet.gradle.setup;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains content for registering file extensions.
 * 
 * @author Volker
 */
public class DocumentType {

    private final SetupBuilder setup;

    private List<String>       extensions;

    private String             name;

    private String             role = "Viewer";

    private Object             icons;

    DocumentType( SetupBuilder setup ) {
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
    public void setFileExtension( String extension ) {
        extensions = null;
        fileExtension( extension );
    }

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

    public String getName() {
        if( name != null && !name.isEmpty() ) {
            return name;
        }
        return setup.getProject().getName() + " file";
    }

    public void setName( String name ) {
        this.name = name;
    }

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

    public Object getIcons() {
        if( icons != null ) {
            return icons;
        }
        return setup.getIcons();
    }

    public void setIcons( Object icons ) {
        this.icons = icons;
    }

}
