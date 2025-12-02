package com.inet.gradle.setup.util;

/**
 * Utility class for string operations.
 */
public final class Strings {
    private Strings() {
        throw new UnsupportedOperationException( "Not instantiatable" );
    }

    /**
     * Returns the string if not null, otherwise returns the fallback.
     * @param string the string to check
     * @param fallback the fallback string
     * @return the string or fallback
     */
    public static String defaultString( String string, String fallback ) {
        return string != null ? string : fallback;
    }
}
