/*
 * Copyright 2015 i-net software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import org.gradle.api.GradleException;
import org.gradle.util.ConfigureUtil;

import groovy.lang.Closure;

/**
 * Definition of an executable which can be started on the desktop (e.g. an entry in the start menu on Windows)
 */
public class DesktopStarter extends Application {

    private String             mimeTypes, categories;

    private Location           location;

    private List<DocumentType> documentTypes = new ArrayList<>();

    /**
     * Create a new DesktopStarter
     *
     * @param setup current SetupBuilder
     */
    public DesktopStarter( AbstractSetupBuilder setup ) {
        super( setup );
    }

    /**
     * Sets the mime type is application is associated with. Multiple mime types are separated
     * by semicolons.
     *
     * @param mimeTypes the mime type
     */
    public void setMimeTypes( String mimeTypes ) {
        this.mimeTypes = mimeTypes;
    }

    /**
     * Returns the mime types separated by semicolon.
     *
     * @return the mime types or <tt>null</tt> if not set
     */
    public String getMimeTypes() {
        return mimeTypes;
    }

    /**
     * Sets the categories as defined by Freedesktop. Multiple categories are separated
     * by semicolons.
     *
     * @see <a href="http://standards.freedesktop.org/menu-spec/latest/apa.html">http://standards.freedesktop.org/menu-spec/latest/apa.html</a>
     * @param categories the categories
     */
    public void setCategories( String categories ) {
        this.categories = categories;
    }

    /**
     * Returns the categories separated by semicolon.
     *
     * @return the categories or <tt>null</tt> if not set
     */
    public String getCategories() {
        return categories;
    }

    /**
     * Get the location of this desktop/shortcut entry.
     *
     * @return the location
     */
    public Location getLocation() {
        if( location != null ) {
            return location;
        }
        return Location.StartMenu;
    }

    /**
     * Set the location of this desktop/shortcut entry.
     *
     * @param location new location
     */
    public void setLocation( Location location ) {
        this.location = location;
    }

    /**
     * Possible locations for desktop/shortcut entries.
     */
    public static enum Location {
        StartMenu, ApplicationMenu, InstallDir;
    }

    /**
     * Register a file extensions.
     *
     * @param closue document type
     */
    public void documentType( Closure<?> closue ) {
        DocumentType doc = ConfigureUtil.configure( closue, new DocumentType( setup ) );
        if( doc.getFileExtension() == null || doc.getFileExtension().size() == 0 ) {
            throw new GradleException( "documentType must contains minimum one fileExtension." );
        }
        documentTypes.add( doc );
    }

    /**
     * Return the registered file extensions or the ones defined by the main setup
     *
     * @return list of document types.
     */
    public List<DocumentType> getDocumentType() {
        if( documentTypes.isEmpty() ) {
            return setup.getDocumentType();
        }
        return documentTypes;
    }
}
