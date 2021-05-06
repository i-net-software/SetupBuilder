/*
 * Copyright 2015 - 2021 i-net software
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

package com.inet.gradle.setup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.util.ConfigureUtil;

import com.inet.gradle.setup.abstracts.AbstractSetupBuilder;
import com.inet.gradle.setup.abstracts.DesktopStarter;
import com.inet.gradle.setup.abstracts.LocalizedResource;
import com.inet.gradle.setup.abstracts.Service;
import com.inet.gradle.setup.abstracts.SetupSources;

import groovy.lang.Closure;

/**
 * The Gradle extension for all setup tasks.
 *
 * @author Volker Berlin
 */
public class SetupBuilder extends AbstractSetupBuilder implements SetupSources {

    private List<LocalizedResource>    licenseFiles            = new ArrayList<>();

    private List<LocalizedResource>    longDescription         = new ArrayList<>();

    private String                     defaultResourceLanguage = "en";

    private DesktopStarter             runAfter, runBeforeUninstall;

    private List<Service>              services                = new ArrayList<>();

    private final List<DesktopStarter> desktopStarters         = new ArrayList<>();

    private List<String>               deleteFiles             = new ArrayList<>();

    private List<String>               deleteFolders           = new ArrayList<>();

    private String                     minUpdateVersion;

    private String                     minUpdateMessage;

    /**
     * Create a new instance.
     *
     * @param project current project
     */
    public SetupBuilder( Project project ) {
        super( project );
    }

    /**
     * Return the license files
     *
     * @return licenseFiles list of license files
     */
    public List<LocalizedResource> getLicenseFiles() {
        return licenseFiles;
    }

    /**
     * Return the license file for a specific locale
     *
     * @param locale for which to get the file
     * @return license file
     */
    public File getLicenseFile( String locale ) {
        return LocalizedResource.getLocalizedResourceFile( licenseFiles, locale );
    }

    /**
     * Set the license file
     *
     * @param license license file or closure
     */
    public void setLicenseFile( Object license ) {
        licenseFiles.clear();
        licenseFile( license );
    }

    /**
     * Add a license file
     *
     * @param license license file or closure
     */
    public void licenseFile( Object license ) {
        LocalizedResource.addLocalizedResource( this, licenseFiles, license );
    }

    /**
     * A command that run after the installer.
     *
     * @return the command or null
     */
    public DesktopStarter getRunAfter() {
        return runAfter;
    }

    /**
     * Set a command that run after the installer.
     *
     * @param runAfter the command
     */
    public void setRunAfter( String runAfter ) {
        this.runAfter = new DesktopStarter( this );
        this.runAfter.setExecutable( runAfter );
    }

    /**
     * Set a command that run after the installer.
     *
     * @param closure the command
     */
    public void runAfter( Closure<?> closure ) {
        runAfter = ConfigureUtil.configure( closure, new DesktopStarter( this ) );
    }

    /**
     * A command that run before the uninstaller.
     *
     * @return the command or null
     */
    public DesktopStarter getRunBeforeUninstall() {
        return runBeforeUninstall;
    }

    /**
     * Set a command that run before the uninstaller.
     *
     * @param runAfter the command
     */
    public void setRunBeforeUninstall( String runAfter ) {
        this.runBeforeUninstall = new DesktopStarter( this );
        this.runBeforeUninstall.setExecutable( runAfter );
    }

    /**
     * Set a command that run run before the uninstaller.
     *
     * @param closue the command
     */
    public void runBeforeUninstall( Closure<DesktopStarter> closue ) {
        runBeforeUninstall = ConfigureUtil.configure( closue, new DesktopStarter( this ) );
    }

    /**
     * Register a service.
     *
     * @param closue the closure of the service definition
     */
    public void service( Closure<Service> closue ) {
        Service service = ConfigureUtil.configure( closue, new Service( this ) );
        services.add( service );
    }

    /**
     * Returns the registered services.
     *
     * @return the registered services
     */
    public List<Service> getServices() {
        return services;
    }

    /**
     * Register a desktop starter.
     *
     * @param closue the closure of the desktop starter's definition
     */
    public void desktopStarter( Closure<?> closue ) {
        DesktopStarter service = ConfigureUtil.configure( closue, new DesktopStarter( this ) );
        desktopStarters.add( service );
    }

    /**
     * Returns the registered desktop starters.
     *
     * @return the registered desktop starters, never null, can be empty.
     */
    public List<DesktopStarter> getDesktopStarters() {
        return desktopStarters;
    }

    /**
     * Get the list pattern for files that should be deleted.
     *
     * @return the list
     */
    public List<String> getDeleteFiles() {
        return deleteFiles;
    }

    /**
     * Add a file pattern to delete files before install and after uninstall.
     *
     * @param pattern the patter. Can contains * and ? characters
     */
    public void deleteFiles( String pattern ) {
        if( pattern.isEmpty() ) {
            return;
        }
        deleteFiles.add( pattern );
    }

    /**
     * Get the list of folders to delete.
     *
     * @return the list
     */
    public List<String> getDeleteFolders() {
        return deleteFolders;
    }

    /**
     * Add a folder to delete before install and after uninstall. It delete the folder with all sub directories.
     *
     * @param folder the folder
     */
    public void deleteFolder( String folder ) {
        this.deleteFolders.add( folder );
    }

    /**
     * @return the defaultResourceLanguage
     */
    public String getDefaultResourceLanguage() {
        return defaultResourceLanguage;
    }

    /**
     * @param defaultResourceLanguage the defaultResourceLanguage to set
     */
    public void setDefaultResourceLanguage( String defaultResourceLanguage ) {
        this.defaultResourceLanguage = defaultResourceLanguage;
    }

    /**
     * Return the description files
     *
     * @return licenseFiles list of license files
     */
    public List<LocalizedResource> getLongDescriptions() {
        return longDescription;
    }

    /**
     * Return the description file for a specific locale
     *
     * @param locale for which to get the file
     * @return license file
     */
    public File getLongDescription( String locale ) {
        return LocalizedResource.getLocalizedResourceFile( longDescription, locale );
    }

    /**
     * Set the description file
     *
     * @param description file or closure
     */
    public void longDescription( Object description ) {
        LocalizedResource.addLocalizedResource( this, longDescription, description );
    }

    /**
     * Return the minimum supported version for an update.
     *
     * @return the version or null
     */
    public String getMinimumUpdateVersion() {
        return minUpdateVersion;
    }

    /**
     * Return the minimum supported version error message.
     *
     * @return the message or null
     */
    public String getMinimumUpdateMessage() {
        return minUpdateMessage;
    }

    /**
     * Set the oldest version which will be updated. If an already installed version is older then the update will be cancel.
     * A default error message is used.
     * @param version the version
     */
    public void setMinimumUpdateMessage( String version ) {
        minimumUpdateVersion( version, null );
    }

    /**
     * Set the oldest version which will be updated. If an already installed version is older then the update will be cancel.
     * A default error message is used.
     * @param version the version
     */
    public void minimumUpdateVersion( String version ) {
        minimumUpdateVersion( version, null );
    }

    /**
     * Set the oldest version which will be updated. If an already installed version is older then the update will be cancel.
     * If no message is set a default error message is used.
     * @param version the version
     * @param message the error message for the user
     */
    public void minimumUpdateVersion( String version, String message ) {
        if( version != null && message == null ) {
            message = "Updates are only supported from version " + version + " or later.";
        }
        minUpdateVersion = version;
        minUpdateMessage = message;
    }
}
