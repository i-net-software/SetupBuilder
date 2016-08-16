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

import com.inet.gradle.setup.AbstractSetupTask;

/**
 * The unix Gradle base.
 * 
 * @author Gerry Weissbach
 */
public abstract class Unix extends AbstractSetupTask {

    private String section;

    private String architecture;

    private String recommends;

    private String depends;

    private String homepage;

    private String installationRoot;

    private String daemonUser = "root";

    private Object defaultServiceFile;

    private String additionalServiceScript;

    public Unix( String format ) {
        super( format );
    }

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
    public String getSection() {
        if( section != null ) {
            return section;
        }
        return "Applications/Productivity";
    }

    public void setSection( String section ) {
        this.section = section;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture( String architecture ) {
        this.architecture = architecture;
    }

    public String getRecommends() {
        return recommends;
    }

    public void setRecommends( String recommends ) {
        this.recommends = recommends;
    }

    public String getDepends() {
        return depends;
    }

    public void setDepends( String depends ) {
        this.depends = depends;
    }

    public String getHomepage() {
        return homepage;
    }

    public void setHomepage( String homepage ) {
        this.homepage = homepage;
    }

    /**
     * Returns the installation root where the program directory should be located. Default is /usr/share + basename
     * 
     * @return the installation root directory
     */
    public String getInstallationRoot() {
        if( installationRoot == null ) {
            return "/usr/share/" + getSetupBuilder().getApplication().toLowerCase().replaceAll("[^a-z0-9-_]", "");
        } else {
            return installationRoot;
        }
    }

    /**
     * Sets the installation root directory where the main files should be located. If the directory ends with a / it
     * will be removed.
     * 
     * @param installationRoot
     *            the installation root directory
     */
    public void setInstallationRoot( String installationRoot ) {
        this.installationRoot = installationRoot.endsWith( "/" )
                        ? installationRoot.substring( 0, installationRoot.length() - 1 ) : installationRoot;
    }

    /**
     * Returns the user with which the daemon should be running. If no user was specified the default root user will be
     * used.
     * 
     * @return the user for the daemon
     */
    public String getDaemonUser() {
        if( daemonUser.trim().isEmpty() ) {
            return "root";
        } else {
            return daemonUser;
        }
    }

    /**
     * Sets the user with which the daemon should be running.
     */
    public void setDaemonUser( String daemonUser ) {
        this.daemonUser = daemonUser;
    }

    /**
     * Returns the a default service configuration file This will be included in the service starter
     * 
     * @return the default service file
     */
    public File getDefaultServiceFile() {
        if( defaultServiceFile != null ) {
            return getProject().file( defaultServiceFile );
        }
        return null;
    }

    /**
     * Set the default service configuration file The file will be put at the "/etc/default/$service$" location
     * 
     * @param defaultServiceFile
     */
    public void setDefaultServiceFile( Object defaultServiceFile ) {
        this.defaultServiceFile = defaultServiceFile;
    }

    /**
     * Returns the additional service script that will be included into the original one.
     * This allows to modify and enhance the service.
     * @return the additional service file
     */
    public String getAdditionalServiceScript() {
        return additionalServiceScript != null ? additionalServiceScript : "" ;
    }

    /**
     * Set an additional service script that will be included into the original one.
     * @param additionalServiceScript
     */
    public void setAdditionalServiceScript( String additionalServiceScript ) {
        this.additionalServiceScript = additionalServiceScript;
    }
}
