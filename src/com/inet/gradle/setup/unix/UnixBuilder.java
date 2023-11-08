package com.inet.gradle.setup.unix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.file.FileTree;
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
        File jreDir = task.getBundleJre();
        if( jreDir == null ) {
            task.getProject().getLogger().lifecycle( "\tNo JRE for bundling set." );
            return;
        }

        // Check if this is the JRE or JDK - usually the jdk has a jre folder
        File jreTarget = new File( filesPath, setup.getBundleJreTarget() ); // jre or something. This is the final destination

        if ( jreDir.isDirectory() ) {

            File jdkCheck = new File( jreDir, "jre" );
            if ( jdkCheck.isDirectory() ) {
                jreDir = jdkCheck;
            }

            checkForBinJava( jreDir);
            task.getProject().getLogger().lifecycle( "\tJRE is set and will be copied from: '" + jreDir.getAbsolutePath() + "' to' " + jreTarget.getAbsolutePath() + "'" );
            ResourceUtils.copy( jreDir, jreTarget );
        } else if ( jreDir.isFile() ) {
            // Check for Archive ... usually a tgz/tar.gz
            FileTree tree = null;
            if ( jreDir.getName().endsWith( ".zip" ) ) {
                tree = setup.getProject().zipTree( jreDir );
            } else if ( jreDir.getName().endsWith( ".tar.gz" ) || jreDir.getName().endsWith( ".tgz" ) ) {
                tree = setup.getProject().tarTree( jreDir );
            } else {
                throw new GradleException( "Unsupported content set as Java Runtime, please use .zip, .tar.gz or .tgz - or a Directory '" + jreDir + "'" );
            }

            
            FileTree sourceTree = tree;
            File javaCopy = new File(setup.getProject().getBuildDir() + "/java_copy"); 
            setup.getProject().copy( spec -> {
                spec.from( sourceTree );
                spec.into( javaCopy );
            } );  
            
            File[] files = javaCopy.listFiles();
            if (files != null && files.length == 1 ) { // only one directory! Move it up!
                Files.move( files[0].toPath(), jreTarget.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING );
            } else {
                Files.move( javaCopy.toPath(), jreTarget.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING );
            }
            checkForBinJava( jreTarget );

        } else {
            throw new GradleException( "Unsupported content set as Java Runtime '" + jreDir + "'" );
        }

        javaMainExecutable = String.join( "/", task.getInstallationRoot(), setup.getBundleJreTarget(), javaCommandSuffix ).replaceAll( "\\/+", "\\/" );
        task.getProject().getLogger().lifecycle( "\tUpdated the Java Executable Path to: '" + javaMainExecutable + "'" );
    }

    /**
     *
     * @param jreDir
     */
    private void checkForBinJava( File jreDir ) {
        File javaCommand = new File( jreDir, javaCommandSuffix);
        String javaVersion = exec( "sh", "-c", javaCommand.getAbsolutePath() + " -version 2>&1 | awk -F '\"' '/version/ {print $2}'" );
        if( javaVersion.length() == 0 ) {
            throw new GradleException( "Java - Command '" + javaCommandSuffix + "' not found in '" + jreDir + "'" );
        }

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
     * @param executable if set to <code>true</code> the executable bit will be set
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
        } else {
        	Set<PosixFilePermission> posixFilePermissions = Files.getPosixFilePermissions(file.toPath());
        	if(posixFilePermissions.contains(PosixFilePermission.OWNER_EXECUTE)) {
        		perms.add( PosixFilePermission.OWNER_EXECUTE );        		
        	}
        	if(posixFilePermissions.contains(PosixFilePermission.GROUP_EXECUTE)) {
        		perms.add( PosixFilePermission.GROUP_EXECUTE );        		
        	}
        	if(posixFilePermissions.contains(PosixFilePermission.OTHERS_EXECUTE)) {
        		perms.add( PosixFilePermission.OTHERS_EXECUTE );        		
        	}
        }
        try {
            Files.setPosixFilePermissions( file.toPath(), perms );
        } catch ( NoSuchFileException e ) {
            // This might happen when a link was asked ... do not bother.
        }
    }
}
