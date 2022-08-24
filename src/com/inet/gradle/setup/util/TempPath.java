package com.inet.gradle.setup.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Automatically create and tear down a temporary directory
 * Also create additional directories when needed.
 *
 * @author gamma
 *
 */
public class TempPath {

    private Path tmp;

    /**
     * Create a TempPath in a defined directory
     * @param root the root path of the temporary directory. Should be used with the tasks temp dir.
     */
    public TempPath( Path root ) {
        tmp = root;
        if ( !tmp.toFile().exists() ) {
            tmp.toFile().mkdirs();
        }

        // Add a shutdown hook to kill the sub process faster
        Runtime.getRuntime().addShutdownHook( new Thread( new Runnable() {
            @Override
            public void run() {
                try {
                    clearTemporaryFolder();
                } catch( Exception e ) {
                    System.err.println( "Could not remove temporary Resources" );
                    e.printStackTrace();
                }
            }
        } ) );
    }

    /**
     * Create a TempPath in a temporary Directory
     * @throws IOException in case of errors while creating the temporary directory
     */
    public TempPath() throws IOException {
        this( Files.createTempDirectory( "SetupBuilder" ) );
    }

    /**
     * Return the temporary root folder as string
     *
     * @return the temporary root folder as string
     */
    public String get() {
        return tmp.toString();
    }

    /**
     * Return the path to the temporary directory with the extension name
     *
     * @param directory below the tmp root
     * @return the Path, create directory if not yet there
     * @throws IOException in case of fire.
     */
    public Path get( String directory ) throws IOException {

        File destination = new File( tmp.toFile(), directory );
        if( !destination.exists() ) {
            if ( destination.mkdirs() ) {
                Logging.sysout( "Created temporary directory: " + destination );
            } else {
                Logging.sysout( "Could not create temporary directory: " + destination );
            }
        } else {
            Logging.sysout( "Directory did exist: " + destination );
        }

        return destination.toPath();
    }

    /**
     * Return a new file from the directory
     *
     * @param directory below the tmp root
     * @param file name of the file
     * @return a new File object for the directory and file
     * @throws IOException in case of fire.
     */
    public File getTempFile( String directory, String file ) throws IOException {
        return new File( get( directory ).toFile(), file );
    }

    /**
     * Return a new file
     *
     * @param file name of the file
     * @return a new File object for the directory and file
     */
    public File getTempFile( String file ) {
        return new File( get(), file );
    }

    /**
     * Return a new temp file as string
     *
     * @param file name of the file
     * @return a new File object for the directory and file
     */
    public String getTempString( String file ) {
        return getTempFile( file ).toString();
    }

    /**
     * Return a new file from the directory
     *
     * @param directory below the tmp root
     * @param file name of the file
     * @return a new File object for the directory and file
     * @throws IOException in case of fire.
     */
    public String getTempString( String directory, String file ) throws IOException {
        return new File( get( directory ).toFile(), file ).toString();
    }

    /**
     * Clear up all the content from below the tmp directory.
     *
     * @throws Exception in case of fire.
     */
    private void clearTemporaryFolder() throws Exception {
        clearTemporaryFolder( tmp );
    }

    /**
     * Clear up all the content from below the given directory.
     * @param tmp the temporary file
     *
     * @throws Exception in case of fire.
     */
    public static void clearTemporaryFolder( Path tmp ) throws Exception {
        // Remove temporary folder and content.
        if ( Logging.DEBUG_LOG ) {
            Logging.sysout( "Will not remove path due to debugging: " + tmp.toString() );
            return;
        }
        
        Files.walkFileTree( tmp, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
                Files.delete( file );
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory( Path dir, IOException exc ) throws IOException {
                Files.delete( dir );
                return FileVisitResult.CONTINUE;
            }
        } );

        Logging.sysout( "Removed the temporary content at: " + tmp.toString() );
    }
}
