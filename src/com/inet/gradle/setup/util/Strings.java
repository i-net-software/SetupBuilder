package com.inet.gradle.setup.util;

public final class Strings {
    private Strings() {
        throw new UnsupportedOperationException( "Not instantiatable" );
    }

    public static String defaultString( String string, String fallback ) {
        return string != null ? string : fallback;
    }
}
