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

package com.inet.gradle.setup;

/**
 * Definition of a service to be installed on the target system.
 */
public class Service extends Application {
    private boolean startOnBoot = true, keepAlive = false;

    private String  id, wrapper;

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
}
