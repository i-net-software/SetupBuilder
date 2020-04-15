package com.inet.gradle.appbundler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;

import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.abstracts.AbstractBuilder;
import com.inet.gradle.setup.abstracts.AbstractSetupBuilder;
import com.inet.gradle.setup.abstracts.AbstractTask;

import groovy.util.XmlSlurper;

public class OSXNotarize<T extends AbstractTask, S extends AbstractSetupBuilder> extends AbstractBuilder<T,S> {

    private String username, passwordPlain, passwordKeychainItem, passwordEnvironmentVariable;
    private String ascProvider;

    public OSXNotarize( T task, FileResolver fileResolver ) {
        super( task, fileResolver );
    }

    /**
     * Execute the notarization ion the given file
     * @param notarizeFile the file to notarize
     * @param codesign the codesigning object for keychain interaction 
     */
    public void run( File notarizeFile, OSXCodeSign<T, S> codesign ) {
        System.out.println( "Notarizing the given file: " + notarizeFile.getAbsolutePath() );
    }

    /**
     * Returns the name of the environment variable that will be used to
     * retrieve the password used for notarization. PLEASE DO NOT USE.
     * @return the name of the environment variable  with the password
     */
    public String getPasswordEnvironmentVariable() {
        return passwordEnvironmentVariable;
    }

    /**
     * Set the name of a keychain item that is going to be used for retrieving the password.
     * Note: The username in the item has to match the one given in this API
     * @param passwordKeychainItem the name if the keychain item used for notarization
     */
    public void setPasswordKeychainItem( String passwordKeychainItem ) {
        this.passwordKeychainItem = passwordKeychainItem;
    }

    /**
     * Set the name of an environment variable that should be used to
     * retrieve the password used for notarization. Please refrain from using this, use {@link OSXNotarize#setPasswordKeychainItem} instead
     * @param passwordEnvironmentVariable the name of an environment variable with the password
     */
    public void setPasswordEnvironmentVariable( String passwordEnvironmentVariable ) {
        this.passwordEnvironmentVariable = passwordEnvironmentVariable;
    }

    /**
     * Returns the name of the keychain item that will be used to retrieve the password.
     * NOTE: the username has to match! 
     * @return the name of the keychain item that will be used for retrieving the password
     */
    public String getPasswordKeychainItem() {
        return passwordKeychainItem;
    }

    /**
     * Set a plain password to be used for notarization. THIS IS DISCOURAGED
     * @param passwordPlain the plain password to be used for notarization.
     */
    public void setPasswordPlain( String passwordPlain ) {
        this.passwordPlain = passwordPlain;
    }

    /**
     * Returns the plain password used for notarization. PLEASE DO NOT USE!
     * @return the plain password used for notarization.
     */
    public String getPasswordPlain() {
        return passwordPlain;
    }

    /**
     * Set the ASC provider which is equired with --notarize-app and --notarization-history
     * when a user account is associated with multiple providers.
     * @param ascProvider the ASC provider
     */
    public void setAscProvider( String ascProvider ) {
        this.ascProvider = ascProvider;
    }

    /**
     * Returns the ASC provider 
     * @return the ASC provider
     */
    public String getAscProvider() {
        return ascProvider;
    }
    
    /**
     * Returns the password item set for the current request.
     * It will throw an IllegalArgumentException if none of the fields
     * for the password items have been set.
     * 
     * @return the password item set for the current request.
     */
    private String getPasswordElement() {
        if ( passwordKeychainItem != null ) {
            return "@keychain:" + passwordKeychainItem;
        } else if ( passwordEnvironmentVariable != null ) {
            return "@env:" + passwordEnvironmentVariable;
        } else if ( passwordPlain != null ) {
            return passwordPlain;
        } else {
            throw new IllegalArgumentException( "At least on of the parameters have to be set: passwordKeychainItem, passwordEnvironmentVariable or passwordPlain" );
        }
    }

    /**
     * Adds default commands for the xcrun process
     * @param command the list of commands so far
     */
    private void addDefaultOptionsToXCRunCommand( ArrayList<String> command ) {
        command.add( "-u" );
        command.add( username );
        command.add( "-p" );
        command.add( getPasswordElement() );

        if ( ascProvider != null ) {
            command.add( "--asc-provider" );
            command.add( ascProvider );
        }

        // Receive an XML answer
        command.add( "--output-format" );
        command.add( "xml" );
    }

    /**
     * Start the notarization process for the given file
     * @param notarizeFile the file to notarize
     * @return the UUID for the process to keep working with
     */
    private String requestNotarization( File notarizeFile ) {

        ArrayList<String> command = new ArrayList<>();
        command.add( "xcrun" );
        command.add( "altool" );
        command.add( "--notarize-app" );
        command.add( "-f" );
        command.add( notarizeFile.getAbsolutePath() );
        command.add( "--primary-bundle-id" );
        command.add( notarizeFile.getName() );
        addDefaultOptionsToXCRunCommand( command );
        
        OutputStream output = new ByteArrayOutputStream();
        exec( command, null, output  );
        output.toString();

        return null;
    }

    /**
     * Wait until the notarization process is done.
     * @param UUID the ID of the task to check against
     */
    private void waitForNotarization( String UUID ) {

        ArrayList<String> command = new ArrayList<>();
        command.add( "xcrun" );
        command.add( "altool" );
        command.add( "--notarize-info" );
        command.add( UUID );
        addDefaultOptionsToXCRunCommand( command );

    }

    /**
     * Validate the original file against the Apple directory
     * @param notarizeFile the file to notarize
     * @return true if vaildation was OK
     */
    private boolean validateApplication( File notarizeFile ) {

        ArrayList<String> command = new ArrayList<>();
        command.add( "xcrun" );
        command.add( "altool" );
        command.add( "--validate-app" );
        command.add( "-f" );
        command.add( notarizeFile.getAbsolutePath() );
        addDefaultOptionsToXCRunCommand( command );

        return false;
    }

    /**
     * Staple the original file with the notarization result
     * @param notarizeFile the file to staple
     */
    private void stapleApplication( File notarizeFile ) {

        ArrayList<String> command = new ArrayList<>();
        command.add( "xcrun" );
        command.add( "stapler" );
        command.add( "staple" );
        command.add( "-v" );
        command.add( notarizeFile.getAbsolutePath() );
        exec( command );
    }

    /**
     * Staple the original file with the notarization result
     * @param notarizeFile the file to staple
     */
    private boolean validateStapledApplication( File notarizeFile ) {

        ArrayList<String> command = new ArrayList<>();
        command.add( "xcrun" );
        command.add( "stapler" );
        command.add( "validate" );
        command.add( "-v" );
        command.add( notarizeFile.getAbsolutePath() );
        exec( command );

        return false;
    }
}
