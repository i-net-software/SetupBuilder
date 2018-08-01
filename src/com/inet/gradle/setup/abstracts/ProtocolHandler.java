package com.inet.gradle.setup.abstracts;

import java.util.ArrayList;
import java.util.Arrays;
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

    private List<String> scheme = new ArrayList<>();

    /**
     * Construct the protocol handler
     * @param setup the setup
     */
    public ProtocolHandler( AbstractSetupBuilder setup ) {
        super( setup );
    }

    /**
     * Return the list of scheme
     * @return the list of scheme
     */
    public List<String> getSchemes() {
        return scheme;
    }

    /**
     * Set the scheme. Can be a string or list
     * @param scheme the scheme to set
     */
    @SuppressWarnings( "unchecked" )
    public void setScheme( Object scheme ) {
        if ( ! ( scheme instanceof List<?> ) ) {
            this.scheme = Arrays.asList( (String)scheme );
        } else {
            this.scheme = (List<String>)scheme;
        }

        checkSchemesAreValid();
    }

    /**
     * Add one or a list of schemes
     * @param scheme the scheme(s) to add
     */
    @SuppressWarnings( "unchecked" )
    public void scheme( Object scheme ) {
        if ( ! ( scheme instanceof List<?> ) ) {
            if ( !((String)scheme).matches( "^[a-zA-Z]+$" ) ) {
                throw new IllegalArgumentException( "The scheme only allows the letters 'a-z'. Was: " + scheme );
            }
            this.scheme.add( (String)scheme );
        } else {
            this.scheme.addAll( (List<String>)scheme );
        }

        checkSchemesAreValid();
    }

    /**
     * Check validity of schemes
     */
    private void checkSchemesAreValid() {
        if ( this.scheme.stream().filter( e -> !e.matches( "^[a-zA-Z]+$" ) ).findFirst().isPresent() ) {
            this.scheme = new ArrayList<>(); // reset
            throw new IllegalArgumentException( "The scheme only allows the letters 'a-z'. Was: " + this.scheme );
        }
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
            res.setScheme( scheme );
        }

        holder.add( res );
    }
}
