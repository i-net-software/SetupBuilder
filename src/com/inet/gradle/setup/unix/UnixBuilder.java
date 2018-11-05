package com.inet.gradle.setup.unix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.abstracts.AbstractBuilder;
import com.inet.gradle.setup.abstracts.AbstractSetupBuilder;
import com.inet.gradle.setup.util.ResourceUtils;

public abstract class UnixBuilder<T extends Unix, S extends AbstractSetupBuilder> extends AbstractBuilder<T, S> {

    protected SetupBuilder setup;

    private String         javaCommandSuffix  = "/bin/java";

    protected String       javaMainExecutable = "/usr" + javaCommandSuffix; // Default Java Location

    /**
     * Create a new instance
     *
     * @param task the calling task
     * @param setup the shared settings
     * @param fileResolver the file Resolver
     */
    protected UnixBuilder( T task, SetupBuilder setup, FileResolver fileResolver ) {
        super( task, fileResolver );
        this.setup = setup;
    }

    /**
     * Check for the bundled JRE and add it
     * @param filesPath the destination to put the jre at
     * @throws IOException an exception
     */
    protected void addBundleJre( File filesPath ) throws IOException {
        Object jre = setup.getBundleJre();
        if( jre == null ) {
            task.getProject().getLogger().lifecycle( "\tNo JRE for bundling set." );
            return;
        }
        File jreDir;
        try {
            jreDir = task.getProject().file( jre );
        } catch( Exception e ) {
            jreDir = null;
        }

        if ( jreDir == null || !jreDir.isDirectory() )
        {
            // This is a version number, need to look on our own.
            // check version of current java
            String javaCommand = exec( "sh", "-c", "readlink -e $(which java)" ); // readlink requires an "sh"
            if ( javaCommand.length() == 0 || !javaCommand.endsWith( javaCommandSuffix ) ) { // Forward slashes should be good. This is Linux!
                throw new GradleException( "Java was not found: '" + javaCommand +"'" );
            }

            String javaVersion = exec( "sh", "-c", javaCommand + " -version 2>&1 | awk -F '\"' '/version/ {print $2}'" );
            if( javaVersion.length() == 0 || !javaVersion.startsWith( jre.toString() ) ) {
                throw new GradleException( "bundleJre version '" + jre + "' does not match the found JavaVersion: '" + javaVersion +"'" );
            }

            jreDir = new File( javaCommand.substring( 0, javaCommand.length() - javaCommandSuffix.length() ) );
            if( !jreDir.isDirectory() ) {
                throw new GradleException( "bundleJre version '" + jre + "' can not be found in: " + jreDir + "'" );
            }
        } else {
            // Check if the folder contains a java - command
            File javaCommand = new File( jreDir, javaCommandSuffix);
            String javaVersion = exec( "sh", "-c", javaCommand.getAbsolutePath() + " -version 2>&1 | awk -F '\"' '/version/ {print $2}'" );
            if( javaVersion.length() == 0 ) {
                throw new GradleException( "Java - Command '" + javaCommandSuffix + "' not found in '" + jreDir + "'" );
            }
        }

        // Check if this is the JRE or JDK - usually the jdk has a jre folder
        File jdkCheck = new File( jreDir, "jre" );
        if ( jdkCheck.isDirectory() ) {
            jreDir = jdkCheck;
        }

        File jreTarget = new File( filesPath, setup.getBundleJreTarget() ); // jre or something. This is the final destination

        task.getProject().getLogger().lifecycle( "\tJRE is set and will be copied from: '" + jreDir.getAbsolutePath() + "' to' " + jreTarget.getAbsolutePath() + "'" );
        ResourceUtils.copy( jreDir, jreTarget );
        javaMainExecutable = String.join( "/", task.getInstallationRoot(), setup.getBundleJreTarget(), javaCommandSuffix ).replaceAll( "\\/+", "\\/" );
        task.getProject().getLogger().lifecycle( "\tUpdated the Java Executable Path to: '" + javaMainExecutable + "'" );
    }


    /**
     * Changes the permissions of all directories recursively inside the specified path to 755.
     *
     * @param path
     *            the path
     * @throws IOException
     *             on I/O failures
     */
    protected void changeDirectoryPermissionsTo755( File path ) throws IOException {
        if ( path == null ) { return; }
        setPermissions( path, true );
        for( File file : path.listFiles() ) {
            if( file.isDirectory() ) {
                changeDirectoryPermissionsTo755( file );
            }
        }
    }

    /**
     * Changes the permissions of all files recursively inside the specified path to 644.
     *
     * @param path
     *            the path
     * @throws IOException
     *             on I/O failures
     */
    protected void changeFilePermissionsTo644( File path ) throws IOException {
        if ( path == null ) { return; }
        for( File file : path.listFiles() ) {
            if( file.isDirectory() ) {
                changeFilePermissionsTo644( file );
            } else {
                if( file.getName().endsWith( ".sh" ) ) {
                    setPermissions( file, true );
                } else {
                    setPermissions( file, false );
                }
            }
        }
    }


    /**
     * Sets the permissions of the specified file, either to 644 (non-executable) or 755 (executable).
     *
     * @param file the file
     * @param executable if set to <tt>true</tt> the executable bit will be set
     * @throws IOException on errors when setting the permissions
     */
    // share
    public static void setPermissions( File file, boolean executable ) throws IOException {
        Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
        perms.add( PosixFilePermission.OWNER_READ );
        perms.add( PosixFilePermission.OWNER_WRITE );
        perms.add( PosixFilePermission.GROUP_READ );
        perms.add( PosixFilePermission.OTHERS_READ );
        if( executable ) {
            perms.add( PosixFilePermission.OWNER_EXECUTE );
            perms.add( PosixFilePermission.GROUP_EXECUTE );
            perms.add( PosixFilePermission.OTHERS_EXECUTE );
        }
        try {
            Files.setPosixFilePermissions( file.toPath(), perms );
        } catch ( NoSuchFileException e ) {
            // This might happen when a link was asked ... do not bother.
        }
    }
}
