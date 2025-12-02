/*
 * Copyright 2015, Quality First Software GmbH and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package com.oracle.appbundler;

import static com.oracle.appbundler.BundleDocument.getListFromCommaSeparatedString;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Class representing an UTExportedTypeDeclaration or UTImportedTypeDeclaration in Info.plist
 */
public class TypeDeclaration implements IconContainer {

    private boolean imported = false;
    private String identifier = null;
    private String referenceUrl = null;
    private String description = null;
    private String icon = null;
    private List<String> conformsTo = null;
    private List<String> osTypes = null;
    private List<String> mimeTypes = null;
    private List<String> extensions = null;

    /** Creates a new TypeDeclaration with default conformsTo value. */
    public TypeDeclaration() {
        this.conformsTo = Arrays.asList(new String[]{"public.data"});
    }
    
    /**
     * Checks if this type declaration is imported.
     * @return true if imported
     */
    public boolean isImported() {
        return imported;
    }

    /**
     * Sets whether this type declaration is imported.
     * @param imported true if imported
     */
    public void setImported(boolean imported) {
        this.imported = imported;
    }

    /**
     * Gets the type identifier.
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets the type identifier.
     * @param identifier the identifier
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Gets the reference URL.
     * @return the reference URL
     */
    public String getReferenceUrl() {
        return referenceUrl;
    }

    /**
     * Sets the reference URL.
     * @param referenceUrl the reference URL
     */
    public void setReferenceUrl(String referenceUrl) {
        this.referenceUrl = referenceUrl;
    }

    /**
     * Gets the description.
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the icon path.
     * @return the icon path
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Sets the icon path.
     * @param icon the icon path
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * Gets the icon file.
     * @return the icon file, or null if not available
     */
    public File getIconFile() {
        if (icon == null) { return null; }

        File ifile = new File (icon);
        
        if (! ifile.exists ( ) || ifile.isDirectory ( )) { return null; }

        return ifile;
    }
    
    /**
     * Checks if an icon is set.
     * @return true if an icon is set
     */
    public boolean hasIcon() {
        return icon != null;
    }
    
    /**
     * Gets the conformsTo list.
     * @return the conformsTo list
     */
    public List<String> getConformsTo() {
        return conformsTo;
    }

    /**
     * Sets the conformsTo list from a comma-separated string.
     * @param conformsToAsString the comma-separated string
     */
    public void setConformsTo(String conformsToAsString) {
        this.conformsTo = getListFromCommaSeparatedString(conformsToAsString, "Conforms To");
    }

    /**
     * Gets the OS types list.
     * @return the OS types list
     */
    public List<String> getOsTypes() {
        return osTypes;
    }

    /**
     * Sets the OS types list from a comma-separated string.
     * @param osTypesAsString the comma-separated string
     */
    public void setOsTypes(String osTypesAsString) {
        this.osTypes = getListFromCommaSeparatedString(osTypesAsString, "OS Types");
    }

    /**
     * Gets the MIME types list.
     * @return the MIME types list
     */
    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    /**
     * Sets the MIME types list from a comma-separated string.
     * @param mimeTypesAsString the comma-separated string
     */
    public void setMimeTypes(String mimeTypesAsString) {
        this.mimeTypes = getListFromCommaSeparatedString(mimeTypesAsString, "Mime Types", true);
    }

    /**
     * Gets the extensions list.
     * @return the extensions list
     */
    public List<String> getExtensions() {
        return extensions;
    }

    /**
     * Sets the extensions list from a comma-separated string.
     * @param extensionsAsString the comma-separated string
     */
    public void setExtensions(String extensionsAsString) {
        this.extensions = getListFromCommaSeparatedString(extensionsAsString, "Extensions", true);
    }

    @Override
    public String toString() {
        return "" + imported;
    }
}
