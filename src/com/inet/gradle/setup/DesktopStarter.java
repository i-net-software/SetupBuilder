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

/**
 * Definition of an executable which can be started on the desktop (e.g. an entry in the start menu on Windows)
 */
public class DesktopStarter extends Application {
    private String             startArguments;
    private String             mimeTypes;
    private String             categories;
    private Location           location;

    /**
     * Create a new DesktopStarter
     * @param setup current SetupBuilder
     */
    public DesktopStarter( SetupBuilder setup ) {
        super( setup );
    }
 
    /**
     * Returns the command-line arguments for starting.
     * @return the command-line arguments for starting
     */
    public String getStartArguments() {
        if (startArguments == null) {
            return "";
        }
        return startArguments;
    }

    /**
     * Sets the command-line arguments for starting .
     * @param startArguments the command-line arguments for starting
     */
    public void setStartArguments( String startArguments ) {
        this.startArguments = startArguments;
    }

    /**
     * Sets the mime type is application is associated with. Multiple mime types are separated 
     * by semicolons.
     * @param mimeTypes the mime type
     */
    public void setMimeTypes( String mimeTypes ) {
        this.mimeTypes = mimeTypes;
    }

    /**
     * Returns the mime types separated by semicolon.
     * @return the mime types or <tt>null</tt> if not set
     */
    public String getMimeTypes() {
        return mimeTypes;
    }

    /**
     * Sets the categories as defined by Freedesktop. Multiple categories are separated
     * by semicolons.
     * @see <a href="http://standards.freedesktop.org/menu-spec/latest/apa.html">http://standards.freedesktop.org/menu-spec/latest/apa.html</a>
     * @param categories the categories
     */
    public void setCategories( String categories ) {
        this.categories = categories;
    }
    
    /**
     * Returns the categories separated by semicolon.
     * @return the categories or <tt>null</tt> if not set
     */
    public String getCategories() {
        return categories;
    }

    /**
     * Get the location of this desktop/shortcut entry.
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
}
