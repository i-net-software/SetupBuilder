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

public class OSXNotarize<T extends AbstractTask, S extends AbstractSetupBuilder> extends AbstractBuilder<T, S> {

    private String            username, passwordPlain, passwordKeychainItem, passwordEnvironmentVariable;

    private String            ascProvider;

    private boolean           debugOutput = false;

    private OSXCodeSign<T, S> codesign;

    public OSXNotarize( T task, FileResolver fileResolver, OSXCodeSign<T, S> codesign ) {
        super( task, fileResolver );
        this.codesign = codesign;
    }

    /**
     * Execute the notarization on the given file
     *
     * @param notarizeFile the file to notarize
     */
    public void run( File notarizeFile ) {
        System.out.println( "Notarizing the given file: " + notarizeFile.getAbsolutePath() );

        checkForRunningNotarizationProcess();
        
        codesign.unlockKeychain(); // Unlock the keychain before the action is run
        String UUID = requestNotarization( notarizeFile ); // This will hang and wait until the upload is done
        if( UUID == null ) {
            throw new IllegalStateException( "The notarization process has returned with an unexpected error." );
        }

        System.out.println( "The RequestUUID for notarization is: " + UUID );

        // This will hang and wait until notarization is done
        if( !waitForNotarization( UUID ) ) {
            throw new IllegalStateException( "The application was not notarized." );
        }

        // Will throw if the exit value was not 0
        stapleApplication( notarizeFile );
    }

    /**
     * Returns the name of the environment variable that will be used to
     * retrieve the password used for notarization. PLEASE DO NOT USE.
     *
     * @return the name of the environment variable with the password
     */
    public String getPasswordEnvironmentVariable() {
        return passwordEnvironmentVariable;
    }

    /**
     * Set the name of a keychain item that is going to be used for retrieving the password.
     * Note: The username in the item has to match the one given in this API
     *
     * @param passwordKeychainItem the name if the keychain item used for notarization
     */
    public void setPasswordKeychainItem( String passwordKeychainItem ) {
        this.passwordKeychainItem = passwordKeychainItem;
    }

    /**
     * Set the name of an environment variable that should be used to
     * retrieve the password used for notarization. Please refrain from using this, use {@link OSXNotarize#setPasswordKeychainItem} instead
     *
     * @param passwordEnvironmentVariable the name of an environment variable with the password
     */
    public void setPasswordEnvironmentVariable( String passwordEnvironmentVariable ) {
        this.passwordEnvironmentVariable = passwordEnvironmentVariable;
    }

    /**
     * Returns the name of the keychain item that will be used to retrieve the password.
     * NOTE: the username has to match!
     *
     * @return the name of the keychain item that will be used for retrieving the password
     */
    public String getPasswordKeychainItem() {
        return passwordKeychainItem;
    }

    /**
     * Set a plain password to be used for notarization. THIS IS DISCOURAGED
     *
     * @param passwordPlain the plain password to be used for notarization.
     */
    public void setPasswordPlain( String passwordPlain ) {
        this.passwordPlain = passwordPlain;
    }

    /**
     * Returns the plain password used for notarization. PLEASE DO NOT USE!
     *
     * @return the plain password used for notarization.
     */
    public String getPasswordPlain() {
        return passwordPlain;
    }

    /**
     * Set the ASC provider which is equired with --notarize-app and --notarization-history
     * when a user account is associated with multiple providers.
     *
     * @param ascProvider the ASC provider
     */
    public void setAscProvider( String ascProvider ) {
        this.ascProvider = ascProvider;
    }

    /**
     * Returns the ASC provider
     *
     * @return the ASC provider
     */
    public String getAscProvider() {
        return ascProvider;
    }

    /**
     * Set the state of debugging. If true: will output the XMLContent from the tools
     *
     * @param debugOutput the state of debugging
     */
    public void setDebugOutput( boolean debugOutput ) {
        this.debugOutput = debugOutput;
    }

    /**
     * Returns the state of debugging. If true: will output the XMLContent from the tools
     *
     * @return the state of debugging.
     */
    public boolean isDebugOutput() {
        return debugOutput;
    }

    /**
     * Returns the password item set for the current request.
     * It will throw an IllegalArgumentException if none of the fields
     * for the password items have been set.
     *
     * @return the password item set for the current request.
     */
    private String getPasswordElement() {
        if( passwordKeychainItem != null ) {
            return "@keychain:" + passwordKeychainItem;
        } else if( passwordEnvironmentVariable != null ) {
            return "@env:" + passwordEnvironmentVariable;
        } else if( passwordPlain != null ) {
            return passwordPlain;
        } else {
            throw new IllegalArgumentException( "At least on of the parameters has to be set: passwordKeychainItem, passwordEnvironmentVariable or passwordPlain" );
        }
    }

    /**
     * Adds default commands for the xcrun process
     *
     * @param command the list of commands so far
     */
    private void addDefaultOptionsToXCRunCommand( ArrayList<String> command ) {
        command.add( "-u" );
        command.add( username );
        command.add( "-p" );
        command.add( getPasswordElement() );

        if( ascProvider != null ) {
            command.add( "--asc-provider" );
            command.add( ascProvider );
        }

        // Receive an XML answer
        command.add( "--output-format" );
        command.add( "xml" );
    }

    /**
     * Start the notarization process for the given file
     *
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
        if( isDebugOutput() ) {
            System.out.println( output );
        }

        try {
            Map<String, Object> plist = Plist.fromXml( output );

            // Check for product errors during upload
            List<String> productErrors = (List<String>)plist.computeIfPresent( "product-errors", ( String key, Object value ) -> ((List<Map<String, Object>>)value).stream().map( entry -> entry.get( "message" ) ).collect( Collectors.toList() ) );
            if( productErrors != null && productErrors.size() > 0 ) {
                throw new IllegalStateException( String.join( "\n", productErrors ) );
            }

            // Return the request UUID for later use
            return (String)plist.computeIfPresent( "notarization-upload", ( String key, Object value ) -> ((Map<String, String>)value).get( "RequestUUID" ) );

        } catch( ClassCastException | XmlParseException e ) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Wait for a minute. Will print the status if given
     * @param status the status to print
     * @throws InterruptedException in case the thread was interrupted
     */
    private void waitWithStatus( String status ) throws InterruptedException {
        if ( status != null ) {
            System.out.println( "Status was: '" + status + "'." );
        }

        System.out.println( "Will wait a minute now." );
        Thread.sleep( 1000 * 60 );
    }

    /**
     * Wait until the notarization process is done.
     *
     * @param UUID the ID of the task to check against
     * @return true if the process was successful
     */
    @SuppressWarnings( "unchecked" )
    private boolean waitForNotarization( String UUID ) {

        int acceptedFailureCount = 5;
        List<String> lastErrors = new ArrayList<>();

        try {
            while( true && acceptedFailureCount > 0 ) {

                ArrayList<String> command = new ArrayList<>();
                command.add( "xcrun" );
                command.add( "altool" );
                command.add( "--notarization-info" );
                command.add( UUID );
                addDefaultOptionsToXCRunCommand( command );

                String output = exec( command.toArray( new String[command.size()] ) );
                if( isDebugOutput() ) {
                    System.out.println( output );
                }

                Map<String, Object> plist = Plist.fromXml( output );
                Map<String, Object> info = (Map<String, Object>)plist.get( "notarization-info" );
                if( info == null ) {
                    acceptedFailureCount--;
                    lastErrors.add( "There was no notarization information present. Was I too fast?" );
                    waitWithStatus( null );
                    continue;
                }

                String status = (String)info.get( "Status" );
                if( status == null ) {
                    acceptedFailureCount--;
                    lastErrors.add( "There was no Status present in the notarization information.\n\n" + output );
                    waitWithStatus( null );
                    continue;
                }

                if( status.equalsIgnoreCase( "success" ) ) {
                    // This is what we have been waiting for!
                    return true;
                } else if( status.equalsIgnoreCase( "invalid" ) ) {
                    System.out.println( "The response status was 'invalid'. Please check the online logfile for problems:" );
                    System.out.println( info.get( "LogFileURL" ) );
                    return false;
                }

                waitWithStatus( status );
            }
        } catch( ClassCastException | XmlParseException | InterruptedException e ) {
            throw new IllegalArgumentException( e );
        }

        throw new IllegalArgumentException( String.join( "\n", lastErrors ) );
    }

    /**
     * This method checks for other processes running the notarization, since Apple
     * does not allow multiple uploads simultaneously 
     */
    private void checkForRunningNotarizationProcess() {

        while( true ) {

            try {
                ArrayList<String> command = new ArrayList<>();
                command.add( "bash" );
                command.add( "-c" );
                command.add( "ps aux | grep notarize-app | grep -v grep | wc -l" );
    
                String output = exec( command.toArray( new String[command.size()] ) );
                if ( debugOutput ) {
                    System.out.println( "Response: `" + output + "`" );
                }

                Integer lineCount = new Integer( output );
                if ( lineCount == 0 ) {
                    return; // Done
                }
    
                // Else continue;
                System.out.println( "There was another process notarizing. Will wait a minute now." );
                Thread.sleep( 1000 * 60 );

            } catch( NumberFormatException | InterruptedException e ) {
                return; // Done
            }
        }
    }

    /**
     * Staple the original file with the notarization result
     *
     * @param notarizeFile the file to staple
     */
    private void stapleApplication( File notarizeFile ) {
        ArrayList<String> command = new ArrayList<>();
        command.add( "xcrun" );
        command.add( "stapler" );
        command.add( "staple" );
        command.add( "-v" );
        command.add( notarizeFile.getAbsolutePath() );

        String output = exec( false, command.toArray( new String[command.size()] ) );
        if( isDebugOutput() ) {
            System.out.println( output );
        }
    }
}
