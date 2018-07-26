package com.inet.gradle.setup.abstracts;

import java.util.List;

import org.gradle.util.ConfigureUtil;

import com.inet.gradle.setup.SetupBuilder;

import groovy.lang.Closure;

/**
 * Custom Protocol handler. Will result in the registration of `scheme`
 * as a handler with the application as a starter.
 *
 * @author gamma
 */
public class ProtocolHandler extends Application {

    private String scheme;

    /**
     * Construct the protocol handler
     * @param setup the setup
     */
    public ProtocolHandler( AbstractSetupBuilder setup ) {
        super( setup );
    }

    /**
     * Return the scheme
     * @return the scheme
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * Set the scheme
     * @param scheme the scheme to set
     */
    public void setScheme( String scheme ) {
        if ( !scheme.matches( "^[a-zA-Z]+$" ) ) {
            throw new IllegalArgumentException( "The scheme only allows the letters 'a-z'. Was: " + scheme );
        }
        this.scheme = scheme;
    }


    /**
     * Add a protocol handler
     *
     * @param parent the setup builder
     * @param holder the list to add the entry to
     * @param scheme file file or closure
     */
    public static void addProtocolHandler( SetupBuilder parent, List<ProtocolHandler> holder, Object scheme ) {

        ProtocolHandler res = new ProtocolHandler( parent );
        if( scheme instanceof Closure<?> ) {
            res = ConfigureUtil.configure( (Closure<?>)scheme, res );
        } else {
            res.setScheme( (String)scheme );
        }

        holder.add( res );
    }
}
