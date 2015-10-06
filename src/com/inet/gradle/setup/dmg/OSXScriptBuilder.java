package com.inet.gradle.setup.dmg;

import java.io.IOException;
import java.util.ArrayList;

import com.inet.gradle.setup.Service;
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
	
	public OSXScriptBuilder(Service service, String template) throws IOException {
		super( template );

		setPlaceholder("displayName", 	service.getDisplayName());
		setPlaceholder("serviceName", 	service.getExecutable());

		setPlaceholder("mainClass",		service.getMainClass());
		setPlaceholder("mainJar",		service.getMainJar());
		setPlaceholder("workingDir",	service.getWorkDir());
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
}
