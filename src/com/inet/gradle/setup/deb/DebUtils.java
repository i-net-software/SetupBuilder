package com.inet.gradle.setup.deb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains methods, that are used in different classes.
 * 
 * @author Stefan Heidrich
 *
 */
public class DebUtils {

	
	/**
     * Sets the permissions of the specified file, either to 644 (non-executable) or 755 (executable).
     * @param file the file 
     * @param executable if set to <tt>true</tt> the executable bit will be set
     * @throws IOException on errors when setting the permissions
     */
    static void setPermissions( File file, boolean executable ) throws IOException {
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
        Files.setPosixFilePermissions( file.toPath(), perms );
    }
	
}
