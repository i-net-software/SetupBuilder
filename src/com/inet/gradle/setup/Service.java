package com.inet.gradle.setup;

/**
 * Definition of a service to be installed on the target system.
 */
public class Service {
    private String  name, description;
    private boolean startOnBoot;
    private String  startArguments;

    /**
     * Returns the name of this service.
     * @return the name of this service
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the service.
     * @param name the name to set
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * Returns the description
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     * @param description the description to set
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * Returns a boolean flag indicating whether the service is started when the system is booted.
     * @return boolean flag indicating whether the service is started when the system is booted
     */
    public boolean isStartOnBoot() {
        return startOnBoot;
    }

    /**
     * Sets a boolean flag indicating whether the service is started when the system is booted.
     * @param startOnBoot boolean flag indicating whether the service is started when the system is booted
     */
    public void setStartOnBoot( boolean startOnBoot ) {
        this.startOnBoot = startOnBoot;
    }

    /**
     * Returns the command-line arguments for starting the service.
     * @return the command-line arguments for starting the service
     */
    public String getStartArguments() {
        return startArguments;
    }

    /**
     * Sets the command-line arguments for starting the service.
     * @param startArguments the command-line arguments for starting the service
     */
    public void setStartArguments( String startArguments ) {
        this.startArguments = startArguments;
    }
}
