package com.inet.gradle.setup.dmg;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;

/**
 * Automatically create and tear down a temporary directory
 * Also create additional directories when needed.
 * 
 * @author gamma
 *
 */
public class TempPath {

    private static Path tmp;

    /**
     * Create the temp directory at startup
     */
    static {
        try {
            tmp = Files.createTempDirectory( "SetupBuilder", new FileAttribute[0] );
        } catch( IOException e1 ) {
            System.err.println( "Could not create temporary directory." );
            e1.printStackTrace();
        }

        // Add a shutdown hook to kill the sub process faster
        Runtime.getRuntime().addShutdownHook( new Thread( new Runnable() {
            @Override
            public void run() {
                try {
                    // clearTemporaryFolder();
                } catch( Exception e ) {
                    System.err.println( "Could not remove temporary Resources" );
                    e.printStackTrace();
                }
            }
        } ) );
    }

    /**
     * Return the temporary root folder as string
     * 
     * @return the temporary root folder as string
     */
    public static String get() {
        return tmp.toString();
    }

    /**
     * Return the path to the temporary directory with the extension name
     * 
     * @param directory below the tmp root
     * @return the Path, create directory if not yet there
     * @throws IOException in case of fire.
     */
    public static Path get( String directory ) throws IOException {

        Path destination = new File( tmp.toFile(), directory ).toPath();
        if( Files.notExists( destination ) ) {
            Files.createDirectories( destination, new FileAttribute[0] );
            System.out.println( "Created temporary directory: " + tmp );
        }

        return destination;
    }

    /**
     * Return a new file from the directory
     * 
     * @param directory below the tmp root
     * @param file name of the file
     * @return a new File object for the directory and file
     * @throws IOException in case of fire.
     */
    public static File getTempFile( String directory, String file ) throws IOException {
        return new File( get( directory ).toFile(), file );
    }

    /**
     * Return a new file
     * 
     * @param file name of the file
     * @return a new File object for the directory and file
     */
    public static File getTempFile( String file ) {
        return new File( get(), file );
    }

    /**
     * Return a new temp file as string
     * 
     * @param file name of the file
     * @return a new File object for the directory and file
     */
    public static String getTempString( String file ) {
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
    public static String getTempString( String directory, String file ) throws IOException {
        return new File( get( directory ).toFile(), file ).toString();
    }

    /**
     * Clear up all the content from below the tmp directory.
     * 
     * @throws Exception in case of fire.
     */
    private static void clearTemporaryFolder() throws Exception {
        // Remove temporary folder and content.
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

        System.out.println( "Removed the temporary content at: " + tmp.toString() );
        tmp = null;
    }
}
