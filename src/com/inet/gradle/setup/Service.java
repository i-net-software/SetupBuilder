package com.inet.gradle.setup;

/**
 * Definition of a service to be installed on the target system.
 */
public class Service extends Application {
    private boolean startOnBoot = true;
    private String  startArguments, id, wrapper;

    /**
     * Create a new Service
     * @param setup current SetupBuilder
     */
    public Service( SetupBuilder setup ) {
        super( setup );
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
     * Returns the serviceID which should be a short version of the application name 
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
	 * @param serviceID
	 */
	public void setId(String serviceID) {
		this.id = serviceID;
	}

	/**
	 * Name of the daemon wrapper, or the program that should be run as daemon 
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
	 * @param wrapper daemon name
	 */
	public void setWrapper(String wrapper) {
		this.wrapper = wrapper;
	}
}