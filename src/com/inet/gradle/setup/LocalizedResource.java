package com.inet.gradle.setup;

import java.io.File;
import java.util.Locale;

/**
 * Stub Object for localized resources
 * @author Gerry Weißbach
 *
 */
public class LocalizedResource {

	private Locale locale;
	private Object resource;
	private SetupBuilder setup;
	
	/**
	 * Stub Object for localized resources
	 * @param setup the setup
	 */
	public LocalizedResource( SetupBuilder setup ) {
		this.setup = setup;
		
	}
	
	/**
	 * @return the resource as file
	 */
	public File getResource() {
		
		if ( resource != null ) {
			return setup.getProject().file( resource );
		}
		
		return null;
	}
	
	/**
	 * @return the locale
	 */
	public Locale getLocale() {
		return locale;
	}

	/**
	 * @param locale the locale to set
	 */
	public void setLocale(String locale) {
		this.locale = new Locale( locale );
	}

	/**
	 * @param resource the resource to set
	 */
	public void setResource(Object resource) {
		this.resource = resource;
	}
}
