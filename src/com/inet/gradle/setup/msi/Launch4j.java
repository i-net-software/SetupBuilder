package com.inet.gradle.setup.msi;

import com.inet.gradle.setup.abstracts.AbstractSetupBuilder;
import com.inet.gradle.setup.abstracts.DesktopStarter;

public class Launch4j extends DesktopStarter {

    private String requestedExecutionLevel = "requireAdministrator";

    /**
     * Create a new DesktopStarter
     * 
     * @param setup current SetupBuilder
     */
    public Launch4j( AbstractSetupBuilder setup ) {
        super( setup );
    }

    /**
     * Sets the requested execution level for launch4j
     * 
     * @param requestedExecutionLevel the requested execution level 
     */
    public void setRequestedExecutionLevel( String requestedExecutionLevel ) {
        this.requestedExecutionLevel = requestedExecutionLevel;
    }

    /**
     * Returns the requested execution level
     * 
     * @return the requested execution level, <tt>"requireAdministrator"</tt> by default or <tt>null</tt> if unset
     */
    public String getRequestedExecutionLevel() {
        return requestedExecutionLevel;
    }
}
