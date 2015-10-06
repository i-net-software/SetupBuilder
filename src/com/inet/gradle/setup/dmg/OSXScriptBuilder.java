package com.inet.gradle.setup.dmg;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.inet.gradle.setup.Application;
import com.inet.gradle.setup.Template;

/**
 * Create scripts from templates and replace placeholders
 * according to configuration/application
 * @author gamma
 *
 */
public class OSXScriptBuilder extends Template {

	private ArrayList<OSXScriptBuilder> scripts = new ArrayList<OSXScriptBuilder>();

	public OSXScriptBuilder(String template) throws IOException {
		super( template );
	}
	
	public OSXScriptBuilder(Application application, String template) throws IOException {
		super( template );

		setPlaceholder("displayName", 	application.getDisplayName());
		setPlaceholder("serviceName", 	application.getMainClass());

		setPlaceholder("mainClass",		application.getMainClass());
		setPlaceholder("mainJar",		application.getMainJar());
		setPlaceholder("workingDir",	application.getWorkDir());
	}
	
	/**
	 * Add another subscript. These will be inserted at the {{script}} tokens
	 * @param script
	 */
	public void addScript( OSXScriptBuilder script ) {
		scripts.add(script);
	}
	
	/**
	 * Create a string containing all subscripts
	 * @return string of all the scripts.
	 */
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		for (OSXScriptBuilder osxScriptBuilder : scripts) {
			sb.append( osxScriptBuilder.toString() );
		}
		
		setPlaceholder("script", sb.toString());
		return super.toString();
	}
	
	/**
	 * Write file and set permissions
	 */
	public void writeTo( File destination ) throws IOException {
        
        super.writeTo( destination );
        Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
        perms.add( PosixFilePermission.OWNER_READ );
        perms.add( PosixFilePermission.OWNER_WRITE );
        perms.add( PosixFilePermission.GROUP_READ );
        perms.add( PosixFilePermission.OTHERS_READ );
        perms.add( PosixFilePermission.OWNER_EXECUTE );
        perms.add( PosixFilePermission.GROUP_EXECUTE );
        perms.add( PosixFilePermission.OTHERS_EXECUTE );
        Files.setPosixFilePermissions( destination.toPath(), perms );
	}
}
