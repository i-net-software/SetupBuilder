package com.inet.gradle.setup.abstracts;

import org.gradle.api.tasks.Input;

/**
 * An abstract base for the Unix SetupTask
 * @author gamma
 */
public abstract class AbstractUnixSetupTask extends AbstractSetupTask {

    public AbstractUnixSetupTask( String extension ) {
        super( extension );
    }

    private String daemonUser = "root";
	private boolean useInitD = false;

    /**
     * Returns the user with which the daemon should be running. If no user was specified the default root user will be
     * used.
     *
     * @return the user for the daemon
     */
    @Input
    public String getDaemonUser() {
        if( daemonUser.trim().isEmpty() ) {
            return "root";
        } else {
            return daemonUser;
        }
    }

    /**
     * Sets the user with which the daemon should be running.
     * @param daemonUser the user to be used for the daemon
     */
    public void setDaemonUser( String daemonUser ) {
        this.daemonUser = daemonUser;
    }
    
    /**
     * Checks if the installer should use the old outdated InitD format
     * @return true if the installer should use the old outdated InitD format
     */
    @Input
	public boolean isUseInitD() {
		return useInitD ;
	}

	/**
	 * Specifies if the installer should use the old outdated InitD format. Per default the Unix installers
	 * use the SystemD daemon format.
	 * @param useInitD true if the old init.d format should be used.
	 */
	public void setUseInitD(boolean useInitD) {
		this.useInitD = useInitD;
	}
    
}
