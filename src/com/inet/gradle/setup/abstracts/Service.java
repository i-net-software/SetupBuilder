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

import com.inet.gradle.setup.SetupBuilder;

/**
 * Definition of a service to be installed on the target system.
 */
public class Service extends Application {
    private boolean startOnBoot = true, keepAlive = false;

    private String id, wrapper, logPath, logPrefix, logLevel, pidFile, stdError, stdOutput, libraryPath, javaHome, jvm;

    /**
     * Create a new Service
     *
     * @param setup current SetupBuilder
     */
    public Service( SetupBuilder setup ) {
        super( setup );
    }

    /**
     * Returns a boolean flag indicating whether the service is started when the system is booted.
     *
     * @return boolean flag indicating whether the service is started when the system is booted
     */
    public boolean isStartOnBoot() {
        return startOnBoot;
    }

    /**
     * Sets a boolean flag indicating whether the service is started when the system is booted.
     *
     * @param startOnBoot boolean flag indicating whether the service is started when the system is booted
     */
    public void setStartOnBoot( boolean startOnBoot ) {
        this.startOnBoot = startOnBoot;
    }

    /**
     * Returns the serviceID which should be a short version of the application name
     *
     * @return the serviceID
     */
    public String getId() {
        if( id == null ) {
            return setup.getAppIdentifier();
        }

        return id;
    }

    /**
     * Set the serviceID which should be a short version of the application name
     * It must not contain spaces.
     *
     * @param serviceID the id
     */
    public void setId( String serviceID ) {
        this.id = serviceID;
    }

    /**
     * Name of the daemon wrapper, or the program that should be run as daemon
     *
     * @return daemon wrapper
     */
    public String getWrapper() {
        String newWrapper = wrapper;
        if( newWrapper == null ) {
            newWrapper = setup.getAppIdentifier() + "-service";
        }

        return newWrapper.toLowerCase().replace( ' ', '-' );
    }

    /**
     * Name of the daemon wrapper, or the program that should be run as daemon
     *
     * @param wrapper daemon name
     */
    public void setWrapper( String wrapper ) {
        this.wrapper = wrapper;
    }

    /**
     * Return true the job should always be running
     *
     * @return true the job should always be running
     */
    public boolean isKeepAlive() {
        return keepAlive;
    }

    /**
     * Set if the job should always be running, even if it crashes
     *
     * @param keepAlive if the job should always be running, even if it crashes
     */
    public void setKeepAlive( boolean keepAlive ) {
        this.keepAlive = keepAlive;
    }

    /**
     * Returns the path to write the daemon logs to.
     */
    public String getLogPath() {
        return logPath;
    }

    /**
     * Sets the path to write the daemon logs to.
     */
    public void setLogPath( String logPath ) {
        this.logPath = logPath;
    }

    /**
     * Returns the daemon log file name prefix.
     */
    public String getLogPrefix() {
        return logPrefix;
    }

    /**
     * Sets the daemon log file name prefix.
     */
    public void setLogPrefix( String logPrefix ) {
        this.logPrefix = logPrefix;
    }

    /**
     * Returns the daemon log level.
     */
    public String getLogLevel() {
        return logLevel;
    }

    /**
     * Sets the daemon log level.
     */
    public void setLogLevel( String logLevel ) {
        this.logLevel = logLevel;
    }

    /**
     * Returns the file name for storing the running process id. Actual file is created in the LogPath directory.
     */
    public String getPidFile() {
        return pidFile;
    }

    /**
     * Sets the file name for storing the running process id. Actual file is created in the LogPath directory.
     */
    public void setPidFile( String pidFile ) {
        this.pidFile = pidFile;
    }

    /**
     * Returns the redirected stderr filename. If named auto file is created in the LogPath directory with the name
     * {@code service-stderr.YEAR-MONTH-DAY.log}.
     */
    public String getStdError() {
        return stdError;
    }

    /**
     * Sets the redirected stderr filename. If named auto file is created in the LogPath directory with the name
     * {@code service-stderr.YEAR-MONTH-DAY.log}.
     */
    public void setStdError( String stdError ) {
        this.stdError = stdError;
    }

    /**
     * Returns the redirected stdout filename. If named auto file is created inside LogPath with the name
     * {@code service-stdout.YEAR-MONTH-DAY.log}.
     */
    public String getStdOutput() {
        return stdOutput;
    }

    /**
     * Sets the redirected stdout filename. If named auto file is created inside LogPath with the name
     * {@code service-stdout.YEAR-MONTH-DAY.log}.
     */
    public void setStdOutput( String stdOutput ) {
        this.stdOutput = stdOutput;
    }

    /**
     * Returns the directory added to the search path used to locate the DLLs for the JVM. This directory is added both
     * in front of the PATH environment variable and as a parameter to the SetDLLDirectory function.
     */
    public String getLibraryPath() {
        return libraryPath;
    }

    /**
     * Sets the directory added to the search path used to locate the DLLs for the JVM. This directory is added both
     * in front of the PATH environment variable and as a parameter to the SetDLLDirectory function.
     */
    public void setLibraryPath( String libraryPath ) {
        this.libraryPath = libraryPath;
    }

    /**
     * Returns the JAVA_HOME used by the service.
     */
    public String getJavaHome() {
        return javaHome;
    }

    /**
     * Sets a different JAVA_HOME than defined by JAVA_HOME environment variable.
     */
    public void setJavaHome( String javaHome ) {
        this.javaHome = javaHome;
    }

    /**
     * Returns the configured jvm.
     *
     * @see #setJvm(String)
     */
    public String getJvm() {
        return jvm;
    }

    /**
     * Set either {@code auto} (i.e. find the JVM from the Windows registry) or the full path to the jvm.dll.
     * Environment variable expansion can be used.
     */
    public void setJvm( String jvm ) {
        this.jvm = jvm;
    }
}
