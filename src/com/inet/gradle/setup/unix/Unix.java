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
package com.inet.gradle.setup.unix;

import java.io.File;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

import com.inet.gradle.setup.abstracts.AbstractUnixSetupTask;

/**
 * The unix base implementation for SetupBuilder.
 *
 * @author Gerry Weissbach
 */
public abstract class Unix extends AbstractUnixSetupTask {

    private String section;

    private String architecture;

    private String recommends;

    private String depends;

    private String homepage;

    private String installationRoot;

    private Object defaultServiceFile;

    private boolean startDefaultService = true;

    private String additionalServiceScript;

    private Object bundleJre;

    public Unix( String format ) {
        super( format );
    }

    /**
     * Default variables for every unix daemon
     * @return the prefilled variables
     */
    @Internal
    public String getVariablesTemplate() {
        String variables = "";
        variables += "APPLICATION_DISPLAY_NAME=\"" + getSetupBuilder().getApplication() + "\"\n";
        variables += "DAEMON_USER=\"" + getDaemonUser() + "\"\n";
        variables += "INSTALLATION_ROOT=\"" + getInstallationRoot() + "\"\n";
        return variables;
    }

    /**
     * Returns the section that specifies the 'Group' entry in the SPEC file. Default is Applications/Productivity
     *
     * @return the section
     */
    @Input
    public String getSection() {
        if( section != null ) {
            return section;
        }
        return "Applications/Productivity";
    }

    /**
     * Set a section (like category) for the application
     * @param section the section
     */
    public void setSection( String section ) {
        this.section = section;
    }

    /**
     * The current architecture
     * @return current architecture
     */
    @Input
    @Optional
    public String getArchitecture() {
        return architecture;
    }

    /**
     * Set the current architecture
     * @param architecture current architecture to set
     */
    public void setArchitecture( String architecture ) {
        this.architecture = architecture;
    }

    /**
     * The recommended package dependencies
     * @return recommended package dependencies
     */
    @Input
    @Optional
    public String getRecommends() {
        return recommends;
    }

    /**
     * Sets the recommended package dependencies
     * @param recommends package dependencies
     */
    public void setRecommends( String recommends ) {
        this.recommends = recommends;
    }

    /**
     * The package dependencies
     * @return package dependencies
     */
    @Input
    @Optional
    public String getDepends() {
        return depends;
    }

    /**
     * Sets the package dependencies
     * @param depends package dependencies
     */
    public void setDepends( String depends ) {
        this.depends = depends;
    }

    /**
     * Homepage of the author
     * @return homepage of the author
     */
    @Input
    @Optional
    public String getHomepage() {
        return homepage;
    }

    /**
     * Sets the homepage of the author
     * @param homepage of the author
     */
    public void setHomepage( String homepage ) {
        this.homepage = homepage;
    }

    /**
     * Returns the installation root where the program directory should be located. Default is /usr/share + basename
     * @return the installation root directory
     */
    @Input
    public String getInstallationRoot() {
        if( installationRoot == null ) {
            return "/usr/share/" + getSetupBuilder().getApplication().toLowerCase().replaceAll("[^a-z0-9-_]", "-");
        } else {
            return installationRoot;
        }
    }

    /**
     * Sets the installation root directory where the main files should be located. If the directory ends with a / it
     * will be removed.
     * @param installationRoot the installations root directory
     */
    public void setInstallationRoot( String installationRoot ) {
        this.installationRoot = installationRoot.endsWith( "/" )
                        ? installationRoot.substring( 0, installationRoot.length() - 1 ) : installationRoot;
    }

    /**
     * Returns the a default service configuration file This will be included in the service starter
     * @return the default service file
     */
    @InputFile
    @Optional
    public File getDefaultServiceFile() {
        if( defaultServiceFile != null ) {
            return getProject().file( defaultServiceFile );
        }
        return null;
    }

    /**
     * Set the default service configuration file The file will be put at the "/etc/default/$service$" location
     * @param defaultServiceFile the default service file
     */
    public void setDefaultServiceFile( Object defaultServiceFile ) {
        this.defaultServiceFile = defaultServiceFile;
    }

    /**
     * Returns the additional service script that will be included into the original one.
     * This allows to modify and enhance the service.
     * @return the additional service file
     */
    @Input
    @Optional
    public String getAdditionalServiceScript() {
        return additionalServiceScript != null ? additionalServiceScript : "" ;
    }

    /**
     * Set an additional service script that will be included into the original one.
     * @param additionalServiceScript a script the will additionally be included in the service
     */
    public void setAdditionalServiceScript( String additionalServiceScript ) {
        this.additionalServiceScript = additionalServiceScript;
    }

    /**
     * Check if the default service should be started during the setup
     * @return true if there is a default service an if it should be started
     */
    public boolean shouldStartDefaultService() {
        return startDefaultService;
    }

    /**
     * Determine if the default service should be started.
     * @param startDefaultService true by default, so only setting false makes sense here.
     */
    public void setStartDefaultService( boolean startDefaultService ) {
        this.startDefaultService = startDefaultService;
    }

    /**
     * Get the bundle JRE directory.
     *
     * @return the value
     */
    @InputDirectory
    @Optional
    public File getBundleJre() {

        Object jre = bundleJre;
        if ( jre == null ) {
            jre = getSetupBuilder().getBundleJre();
        }
        if ( jre == null ) {
            return null; // Expected if nothing was set, nothing will be included.
        }

        File jreDir = null;
        try {
            jreDir = getProject().file( jre );
        } catch( Exception e ) {
            // Will keep going though!
            getProject().getLogger().error( "bundleJre version '" + jre + "' can not be resolved to a Java Runtime Directory which is required for embedding!" );
        }
        return jreDir;
    }

    /**
     * Add a Java VM into your setup. This has to be a
     * directory to an installed Java VM.
     *
     * @param bundleJre path
     */
    public void setBundleJre( Object bundleJre ) {
        this.bundleJre = bundleJre;
    }
}
