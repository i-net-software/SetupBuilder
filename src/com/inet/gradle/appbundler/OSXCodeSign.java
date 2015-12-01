package com.inet.gradle.appbundler;

import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.AbstractBuilder;
import com.inet.gradle.setup.AbstractSetupBuilder;
import com.inet.gradle.setup.AbstractSetupTask;

/**
 * Create code signature for packages. Deep Signing. 
 * @author gamma
 * @param <T> concrete Task
 * @param <S> concrete SetupBuilder
 *
 */
public class OSXCodeSign<T extends AbstractSetupTask<S>, S extends AbstractSetupBuilder> extends AbstractBuilder<T,S> {

	private String identity;
	
	/**
	 * Setup up the Sign Tool
	 * @param task task
	 * @param fileResolver resolver
	 */
	public OSXCodeSign(T task, FileResolver fileResolver) {
		super(task, fileResolver);
	}

	/**
	 * Return the Identity to sign with
	 * @return identity
	 */
	public String getIdentity() {
		return identity;
	}

	/**
	 * Set the Identity to sign with.
	 * @param identity to sign with
	 */
	public void setIdentity(String identity) {
		this.identity = identity;
	}

	/**
	 * Signed an application package
	 * @param path of the application
	 */
	public void signApplication( String path ) {
		
		
		
	}
}
