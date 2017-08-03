package com.inet.gradle.setup.util;

/**
 * Wrapper Class for logging using the environment variable _SETUPBUILDER_DEBUG=true
 * @author gamma
 *
 */
public class Logging {

    public static final boolean        DEBUG_LOG               = System.getenv( "_SETUPBUILDER_DEBUG" ) != null;

    /**
     * Convenience Method to log output only if environment has _SETUPBUILDER_DEBUG=true
     * @param message to log
     */
    public static void sysout( String message ) {
        if ( ! DEBUG_LOG ) {
            return;
        }

        System.out.println( message );
    }

    /**
     * Convenience Method for error logging. Will always log.
     * @param message to log
     */
    public static void syserr( String message ) {
        System.err.println( message );
    }
}
