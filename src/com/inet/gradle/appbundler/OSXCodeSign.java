package com.inet.gradle.appbundler;

import java.io.File;
import java.util.ArrayList;

import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.AbstractBuilder;
import com.inet.gradle.setup.AbstractSetupBuilder;
import com.inet.gradle.setup.AbstractTask;

/**
 * Create code signature for packages. Deep Signing. 
 * @author gamma
 * @param <T> concrete Task
 * @param <S> concrete SetupBuilder
 *
 */
public class OSXCodeSign<T extends AbstractTask, S extends AbstractSetupBuilder> extends AbstractBuilder<T,S> {

	private String identity, identifier, keychain;
	private boolean ignoreError;
	
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
		if ( identity == null ) {
			throw new IllegalArgumentException( "Ypu have to define the signing identity" );
		}
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
	 * Specific Identifier to embed in code (option -i) 
	 * @return identifier 
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Specific Identifier to embed in code (option -i) 
	 * @param identifier to set
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * Key chain to use for signing. It has to be unlocked.
	 * @return key chain
	 */
	public String getKeychain() {
		return keychain;
	}

	/**
	 * Set Key chain to use for signing. It has to be unlocked. 
	 * @param keychain key chain
	 */
	public void setKeychain(String keychain) {
		this.keychain = keychain;
	}

	/**
	 * True if errors during signing should be ignored
	 * @return ignore errors
	 */
	public boolean isIgnoreError() {
		return ignoreError;
	}

	/**
	 * Should errors be ignored during signing
	 * @param ignoreError ignore
	 */
	public void setIgnoreError(boolean ignoreError) {
		this.ignoreError = ignoreError;
	}
	
	/**
	 * Signed an application package
	 * @param path of the application
	 */
	public void signApplication( File path ) {
		
		// Set Read on all files and folders
		ArrayList<String> command = new ArrayList<>();
        command.add( "codesign" );
        command.add( "-f" );
        command.add( "--deep" );
        command.add( "-s" );
        command.add( getIdentity() );
        
        if ( getIdentifier() != null ) {
            command.add( "-i" );
            command.add( getIdentifier() );
        }
        
        if ( getKeychain() != null ) {
            command.add( "--keychain" );
            command.add( getKeychain() );
        }

        command.add( path.getAbsolutePath() );
        exec( command, null, null, isIgnoreError() );
		
	}
}
