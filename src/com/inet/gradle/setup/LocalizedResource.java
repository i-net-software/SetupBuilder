package com.inet.gradle.setup;

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.gradle.util.ConfigureUtil;

import groovy.lang.Closure;

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
	 * @return the language
	 */
	public String getLanguage() {
		return locale.getLanguage();
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
	
    /**
     * Return the localized file for a specific locale
     * @param list from which to receive the resource
     * @param locale for which to get the file
     * @return license file
     */
    public static File getLocalizedResourceFile( List<LocalizedResource> list, String locale ) {
    	
    	for (LocalizedResource res : list) {
			if ( locale.equalsIgnoreCase( res.getLanguage()) ) {
				return res.getResource();
			}
		}
    	
        return null;
    }


    /**
     * Set the license file
     * @param parent the setup builder
     * @param holder the list to add the entry to
     * @param resource file file or closure
     */
    public static void addLocalizedResource( SetupBuilder parent, List<LocalizedResource> holder, Object resource ) {
    	
    	LocalizedResource res = new LocalizedResource( parent );
    	if ( resource instanceof Closure<?> ) {
    		res = ConfigureUtil.configure((Closure<?>)resource, res);
    	} else {
        	res.setLocale( parent.getDefaultResourceLanguage() );
        	res.setResource( resource );
    	}
    	
    	holder.add( res );
    }
}
