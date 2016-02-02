package com.inet.gradle.setup.dmg;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

	/**
	 * Create an OSX script using the specified template
	 * @param template to create script from
	 * @throws IOException in case of errors 
	 */
	public OSXScriptBuilder(String template) throws IOException {
		super( template );
	}

    /**
     * Creates an instance and concatenates the script from the input lines
     * @param script input lines
     */
	public OSXScriptBuilder(ArrayList<String> script) {
		super( script );
	}
	
	/**
	 * Create an OSX script using the specified template
	 * @param application to use for initial setup
	 * @param template to create script from
	 * @throws IOException in case of errors 
	 */
	public OSXScriptBuilder(Application application, String template) throws IOException {
		super( template );

		setPlaceholder("executable", 	application.getExecutable());
		setPlaceholder("displayName", 	application.getDisplayName());
		setPlaceholder("serviceName", 	application.getMainClass());

		setPlaceholder("mainClass",		application.getMainClass());
		setPlaceholder("mainJar",		application.getMainJar());
		setPlaceholder("workingDir",	application.getWorkDir());
	}
	
	/**
	 * Add another subscript. These will be inserted at the {{script}} tokens
	 * @param script to add
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
			if ( osxScriptBuilder == null) {
				continue;
			}
			sb.append( osxScriptBuilder.toString() );
		}
		
		setPlaceholder("script", sb.toString());
		return super.toString();
	}
	
	/**
	 * Write file and set permissions
	 * @param file to write to
	 * @throws IOException that can occur
	 */
	public void writeTo( File file ) throws IOException {
        
        try(FileWriter writer = new FileWriter( file )) {
            writer.write( toString() );
        }
		
        super.writeTo( file );
        Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
        perms.add( PosixFilePermission.OWNER_READ );
        perms.add( PosixFilePermission.OWNER_WRITE );
        perms.add( PosixFilePermission.GROUP_READ );
        perms.add( PosixFilePermission.OTHERS_READ );
        perms.add( PosixFilePermission.OWNER_EXECUTE );
        perms.add( PosixFilePermission.GROUP_EXECUTE );
        perms.add( PosixFilePermission.OTHERS_EXECUTE );
        Files.setPosixFilePermissions( file.toPath(), perms );
	}
	
	@Override
	public OSXScriptBuilder setPlaceholder(String placeholder, String content) {
		super.setPlaceholder(placeholder, content);
		return this;
	}
}
