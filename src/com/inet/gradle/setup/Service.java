package com.inet.gradle.setup;

/**
 * Definition of a service to be installed on the target system.
 */
public class Service {
    private final SetupBuilder setup;
    private String  name, displayName, description, mainJar, mainClass;
    private boolean startOnBoot = true;
    private String  startArguments;

    /**
     * Create a new Service
     * @param setup current SetupBuilder
     */
    public Service( SetupBuilder setup ) {
        this.setup = setup;
    }

    /**
     * Returns the name of this service.
     * @return the name of this service
     */
    public String getName() {
        if( name != null ) {
            return name;
        }
        return setup.getBaseName();
    }

    /**
     * Set the name of the service.
     * @param name the name to set
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * Get the display name
     * @return the display name
     */
    public String getDisplayName() {
        if( displayName != null ) {
            return displayName;
        }
        return setup.getApplication();
    }

    /**
     * Set the display name
     * @param displayName the display name
     */
    public void setDisplayName( String displayName ) {
        this.displayName = displayName;
    }

    /**
     * Returns the description
     * @return the description
     */
    public String getDescription() {
        if( description == null ) {
            return "";
        }
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
        if (startArguments == null) {
            return "";
        }
        return startArguments;
    }

    /**
     * Sets the command-line arguments for starting the service.
     * @param startArguments the command-line arguments for starting the service
     */
    public void setStartArguments( String startArguments ) {
        this.startArguments = startArguments;
    }

    /**
     * Get the main jar file.
     * 
     * @return the main jar
     */
    public String getMainJar() {
        if( mainJar != null ) {
            return mainJar;
        }
        return setup.getMainJar();
    }

    /**
     * Set the jar which contains the main class. This jar must contains all references to all other needed jar files in the manifest.
     * 
     * @param mainJar the main jar file
     */
    public void setMainJar( String mainJar ) {
        this.mainJar = mainJar;
    }

    /**
     * Get the main class.
     * 
     * @return the class name
     */
    public String getMainClass() {
        if( mainClass != null ) {
            return mainClass;
        }
        return setup.getMainClass();
    }

    /**
     * Set the main class of the Java application.
     * 
     * @param mainClass the class name
     */
    public void setMainClass( String mainClass ) {
        this.mainClass = mainClass;
    }
}
