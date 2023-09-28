package com.inet.gradle.appbundler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.logging.Logger;

import com.inet.gradle.appbundler.utils.xmlwise.Plist;
import com.inet.gradle.appbundler.utils.xmlwise.XmlParseException;
import com.inet.gradle.setup.abstracts.AbstractBuilder;
import com.inet.gradle.setup.abstracts.AbstractSetupBuilder;
import com.inet.gradle.setup.abstracts.AbstractTask;

public class OSXNotarize<T extends AbstractTask, S extends AbstractSetupBuilder> extends AbstractBuilder<T, S> {

    private String            username, passwordPlain, passwordKeychainItem;

    private String            teamId;

    private boolean           debugOutput = false;

    private OSXCodeSign<T, S> codesign;

    public OSXNotarize( T task, FileResolver fileResolver, OSXCodeSign<T, S> codesign ) {
        super( task, fileResolver );
        this.codesign = codesign;
    }

    /**
     * Execute the notarization on the given file
     * @param notarizeFile the file to notarize
     */
    public void run( File notarizeFile ) {
        task.getProject().getLogger().info( "Notarizing the given file: " + notarizeFile.getAbsolutePath() );

        checkForRunningNotarizationProcess();

        codesign.unlockKeychain(); // Unlock the keychain before the action is run
        String UUID = requestNotarization( notarizeFile ); // This will hang and wait until the upload is done
        if( UUID == null ) {
            throw new IllegalStateException( "The notarization process has returned with an unexpected error." );
        }

        task.getProject().getLogger().info( "The RequestUUID for notarization is: " + UUID );

        // This will hang and wait until notarization is done
        if( !waitForNotarization( UUID ) ) {
            throw new IllegalStateException( "The application was not notarized." );
        }

        // Will throw if the exit value was not 0
        stapleApplication( notarizeFile );
    }

    /**
     * Set the name of a keychain item that is going to be used for retrieving the password. Note: The username in the item has to match the one given in this API
     * @param passwordKeychainItem the name if the keychain item used for notarization
     */
    public void setPasswordKeychainItem( String passwordKeychainItem ) {
        this.passwordKeychainItem = passwordKeychainItem;
    }

    /**
     * Returns the name of the keychain item that will be used to retrieve the password. NOTE: the username has to match!
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
     * Set the team Id which is required for the notarization.
     * @param teamId the team Id
     */
    public void setTeamId( String teamId ) {
        this.teamId = teamId;
    }

    /**
     * Returns the team ID
     * @return the team OD
     */
    public String getTeamId() {
        return teamId;
    }

    /**
     * Set the state of debugging. If true: will output the XMLContent from the tools
     * @param debugOutput the state of debugging
     */
    public void setDebugOutput( boolean debugOutput ) {
        this.debugOutput = debugOutput;
    }

    /**
     * Returns the state of debugging. If true: will output the XMLContent from the tools
     * @return the state of debugging.
     */
    public boolean isDebugOutput() {
        return debugOutput;
    }

    /**
     * Adds default commands for the xcrun process
     * @param command the list of commands so far
     */
    private void addDefaultOptionsToXCRunCommand( ArrayList<String> command ) {
        command.add( "--apple-id" );
        command.add( username );

        if( passwordKeychainItem != null ) {
            command.add( "-p" );
            command.add( passwordKeychainItem );
        } else if( passwordPlain != null ) {
            command.add( "-password" );
            command.add( passwordPlain );
        } else {
            throw new IllegalArgumentException( "At least on of the parameters has to be set: passwordKeychainItem, passwordEnvironmentVariable or passwordPlain" );
        }

        if( teamId != null ) {
            command.add( "--team-id" );
            command.add( teamId );
        }

        // Receive an XML answer
        command.add( "--output-format" );
        command.add( "plist" );
    }

    /**
     * Start the notarization process for the given file
     * @param notarizeFile the file to notarize
     * @return the UUID for the process to keep working with
     * @throws XmlParseException in case the received plist xml file was erroneous
     */
    private String requestNotarization( File notarizeFile ) {

        ArrayList<String> command = new ArrayList<>();
        command.add( "xcrun" );
        command.add( "notarytool" );
        command.add( "submit" );
        addDefaultOptionsToXCRunCommand( command );
        command.add( notarizeFile.getAbsolutePath() );

        ByteArrayOutputStream error = new ByteArrayOutputStream();
        String output = exec( true, error, command.toArray( new String[command.size()] ) );
        if( isDebugOutput() ) {
            task.getProject().getLogger().lifecycle( output );
        }

        try {
            Map<String, Object> plist = Plist.fromXml( output );
            return (String)plist.get( "id" );
        } catch( ClassCastException | XmlParseException e ) {
            Logger logger = task.getProject().getLogger();
            logger.error( "An error occured while checking the noraization response." );
            if( !isDebugOutput() ) {
                // Debug in addition
                logger.error( "Debug output START:" );
                logger.error( output );
                logger.error( "Debug output END" );
            }

            logger.error( "The Error stream produced:" );
            logger.error( error.toString() );

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PrintStream stream = new PrintStream( bos );
            e.printStackTrace( stream );
            logger.error( "This is the exception it produced:" );
            logger.error( bos.toString() );
            logger.error( "End of Output." );
        }

        return null;
    }

    /**
     * Wait for a minute. Will print the status if given
     * @param status the status to print
     * @throws InterruptedException in case the thread was interrupted
     */
    private void waitWithStatus( String status ) throws InterruptedException {
        if( status != null ) {
            task.getProject().getLogger().info( "Status was: '" + status + "'." );
        }

        task.getProject().getLogger().info( "Will wait a minute now." );
        Thread.sleep( 1000 * 60 );
    }

    /**
     * Wait until the notarization process is done.
     * @param UUID the ID of the task to check against
     * @return true if the process was successful
     */
    private boolean waitForNotarization( String UUID ) {

        String output = "";
        int acceptedFailureCount = 5;
        List<String> lastErrors = new ArrayList<>();

        while( true && acceptedFailureCount > 0 ) {

            ByteArrayOutputStream error = new ByteArrayOutputStream();
            try {

                ArrayList<String> command = new ArrayList<>();
                command.add( "xcrun" );
                command.add( "notarytool" );
                command.add( "info" );
                addDefaultOptionsToXCRunCommand( command );
                command.add( UUID );

                output = exec( true, error, command.toArray( new String[command.size()] ) );
                task.getProject().getLogger().debug( output );

                Map<String, Object> info = Plist.fromXml( output );
                if( info == null ) {
                    acceptedFailureCount--;
                    lastErrors.add( "There was no notarization information present. Was I too fast?" );
                    waitWithStatus( null );
                    continue;
                }

                String status = (String)info.get( "status" );
                if( status == null ) {
                    acceptedFailureCount--;
                    lastErrors.add( "There was no Status present in the notarization information.\n\n" + output );
                    waitWithStatus( null );
                    continue;
                }

                if( status.equalsIgnoreCase( "Accepted" ) ) {
                    // This is what we have been waiting for!
                    return true;
                } else if( status.equalsIgnoreCase( "Invalid" ) ) {
                    task.getProject().getLogger().error( "The response status was 'invalid'. Please check the online logfile for problems:" );
                    requestLogfile( UUID );
                    return false;
                } else if( status.equalsIgnoreCase( "Rejected" ) ) {
                    task.getProject().getLogger().error( "The response status was 'rejected'. Please check the online logfile for problems:" );
                    Object logFileURL = info.get( "LogFileURL" );
                    if ( logFileURL != null ) {
                        task.getProject().getLogger().error( logFileURL.toString() );
                    }
                    requestLogfile( UUID );
                    return false;
                }

                waitWithStatus( status );
            } catch( ClassCastException | XmlParseException | InterruptedException e ) {
                lastErrors.add( e.getMessage() );

                lastErrors.add( "The Error stream produced:\n" );
                lastErrors.add( error.toString() + "\n" );

                lastErrors.add( "Output:\n" );
                lastErrors.add( output + "\n\n" );
                try {
                    waitWithStatus( null );
                } catch( InterruptedException ie ) {
                    // ignore
                }
            }
        }

        throw new IllegalArgumentException( String.join( "\n", lastErrors ) );
    }

    /**
     * Request the logfile of the notarization
     * @param UUID the uuid of the process
     */
    private void requestLogfile( String UUID ) {

        ArrayList<String> command = new ArrayList<>();
        command.add( "xcrun" );
        command.add( "notarytool" );
        command.add( "log" );
        addDefaultOptionsToXCRunCommand( command );
        command.add( UUID );

        ByteArrayOutputStream error = new ByteArrayOutputStream();
        String output = exec( true, error, command.toArray( new String[command.size()] ) );
        if( isDebugOutput() ) {
            task.getProject().getLogger().debug( output );
        }

        task.getProject().getLogger().info( "Here comes the logfile of the notarization request" );
        task.getProject().getLogger().info( output );
    }
    /**
     * This method checks for other processes running the notarization, since Apple does not allow multiple uploads simultaneously
     */
    private void checkForRunningNotarizationProcess() {

        while( true ) {

            try {
                ArrayList<String> command = new ArrayList<>();
                command.add( "bash" );
                command.add( "-c" );
                command.add( "ps aux | grep notarize-app | grep -v grep | wc -l" );

                String output = exec( command.toArray( new String[command.size()] ) );
                if( debugOutput ) {
                    task.getProject().getLogger().info( "Response: `" + output + "`" );
                }

                Integer lineCount = Integer.valueOf( output );
                if( lineCount.intValue() == 0 ) {
                    return; // Done
                }

                // Else continue;
                task.getProject().getLogger().info( "There was another process notarizing. Will wait a minute now." );
                Thread.sleep( 1000 * 60 );

            } catch( NumberFormatException | InterruptedException e ) {
                return; // Done
            }
        }
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

        String output = exec( false, command.toArray( new String[command.size()] ) );
        if( isDebugOutput() ) {
            task.getProject().getLogger().debug( output );
        }
    }
}
