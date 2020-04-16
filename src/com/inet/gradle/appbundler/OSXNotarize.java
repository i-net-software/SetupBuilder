package com.inet.gradle.appbundler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.appbundler.utils.xmlwise.Plist;
import com.inet.gradle.appbundler.utils.xmlwise.XmlParseException;
import com.inet.gradle.setup.abstracts.AbstractBuilder;
import com.inet.gradle.setup.abstracts.AbstractSetupBuilder;
import com.inet.gradle.setup.abstracts.AbstractTask;

public class OSXNotarize<T extends AbstractTask, S extends AbstractSetupBuilder> extends AbstractBuilder<T,S> {

    private String username, passwordPlain, passwordKeychainItem, passwordEnvironmentVariable;

    private String ascProvider;

    private OSXCodeSign<T, S> codesign;

    public OSXNotarize( T task, FileResolver fileResolver, OSXCodeSign<T, S> codesign ) {
        super( task, fileResolver );
        this.codesign = codesign;
    }

    /**
     * Execute the notarization ion the given file
     * @param notarizeFile the file to notarize
     */
    public void run( File notarizeFile ) {
        System.out.println( "Notarizing the given file: " + notarizeFile.getAbsolutePath() );

        codesign.unlockKeychain(); // Unlock the keychain before the action is run
        String UUID = requestNotarization( notarizeFile ); // This will hang and wait until the upload is done
        if ( UUID == null ) {
            throw new IllegalStateException( "The notarization process has returned with an unexpected error." );
        }

        System.out.println( "The RequestUUID for notarization is: " + UUID );

        // This will hang and wait until notarization is done
        if ( !waitForNotarization( UUID ) ) {
            throw new IllegalStateException( "The application was not notarized" );
        }

        // Will throw if the exit value was not 0
        stapleApplication( notarizeFile );
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
     * @throws XmlParseException in case the received plist xml file was erroneous
     */
    @SuppressWarnings( "unchecked" )
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

        String output = exec( command.toArray( new String[command.size()] ) );

        try {
            Map<String, Object> plist = Plist.fromXml( output );

            // Check for product errors during upload
            List<String> productErrors = (List<String>)plist.computeIfPresent( "product-errors", (String key, Object value) ->
                ((List<Map<String,Object>>)value).stream().map( entry -> entry.get( "message" ) )
                .collect( Collectors.toList() )
            );
            if ( productErrors != null && productErrors.size() > 0 ) {
                throw new IllegalStateException( String.join( "\n", productErrors ) );
            }

            // Return the request UUID for later use
            return (String)plist.computeIfPresent( "notarization-upload", (String key, Object value) ->
                ((Map<String, String>)value).get( "RequestUUID" )
            );

        } catch( ClassCastException | XmlParseException e ) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Wait until the notarization process is done.
     * @param UUID the ID of the task to check against
     * @return 
     */
    private boolean waitForNotarization( String UUID ) {

        Boolean returnStatus[] = { false };
        Object lock = new Object();
        Thread thread = new Thread( new Runnable() {

            @Override
            @SuppressWarnings( "unchecked" )
            public void run() {
                synchronized( lock ) {

                    try {
                        do {
                            ArrayList<String> command = new ArrayList<>();
                            command.add( "xcrun" );
                            command.add( "altool" );
                            command.add( "--notarize-info" );
                            command.add( UUID );
                            addDefaultOptionsToXCRunCommand( command );

                            String output = exec( command.toArray( new String[command.size()] ) );

                            try {
                                Map<String, Object> plist = Plist.fromXml( output );
                                Map<String, Object> info = (Map<String, Object>)plist.get( "notarization-info" );
                                if ( info == null ) {
                                    throw new IllegalStateException( "There was no notarization information present." );
                                }

                                String status = (String)info.get( "Status" );
                                if ( status == null ) {
                                    throw new IllegalStateException( "There was no Status present in the notarization information." );
                                }

                                if ( status.equalsIgnoreCase( "success" ) ) {
                                    // This is what we have been waiting for!
                                    returnStatus[0] = true;
                                    break;
                                } else if ( status.equalsIgnoreCase( "invalid" ) ) {
                                    System.out.println( "The response status was 'invalid'. Please check the online logfile for problems:" );
                                    System.out.println( info.get( "LogFileURL" ) );
                                    break;
                                }

                                // Else continue;
                                System.out.println( "Status was: '" + status + "'. Will wait a minute now." );
                                Thread.sleep( 1000 * 60 );

                            } catch( ClassCastException | XmlParseException | InterruptedException e ) {
                                throw new IllegalArgumentException( e );
                            }

                        } while (true);
                    } finally {
                        lock.notifyAll();
                    }
                }
            }

        } );

        thread.start();
        synchronized( lock ) {
            try {
                lock.wait();
            } catch ( InterruptedException e ) {
                e.printStackTrace();
            }
        }

        // Done waiting for notarization
        return returnStatus[0];
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
        exec( false, command.toArray( new String[command.size()] ) );
    }
}
