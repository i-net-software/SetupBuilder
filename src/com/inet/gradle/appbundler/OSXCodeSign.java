package com.inet.gradle.appbundler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.abstracts.AbstractBuilder;
import com.inet.gradle.setup.abstracts.AbstractSetupBuilder;
import com.inet.gradle.setup.abstracts.AbstractTask;

/**
 * Create code signature for packages. Deep Signing.
 * @author gamma
 * @param <T> concrete Task
 * @param <S> concrete SetupBuilder
 *
 */
public class OSXCodeSign<T extends AbstractTask, S extends AbstractSetupBuilder> extends AbstractBuilder<T,S> {

    static private String INTERNAL_ENTITLEMENT =    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                                                    "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" + 
                                                    "<plist version=\"1.0\">\n" + 
                                                    "<dict>\n" + 
                                                    "    <key>com.apple.security.cs.allow-jit</key>\n" + 
                                                    "    <true/>\n" + 
                                                    "    <key>com.apple.security.cs.allow-unsigned-executable-memory</key>\n" + 
                                                    "    <true/>\n" + 
                                                    "    <key>com.apple.security.cs.disable-executable-page-protection</key>\n" + 
                                                    "    <true/>\n" + 
                                                    "    <key>com.apple.security.cs.allow-dyld-environment-variables</key>\n" + 
                                                    "    <true/>\n" + 
                                                    "</dict>\n" + 
                                                    "</plist>";

    private String identity, productIdentity, identifier, keychain, keychainPassword, entitlements = INTERNAL_ENTITLEMENT;
    private boolean ignoreError, deepsign = true, hardened = true;

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
     * This is the "Common Name" part from the certificate
     * @return identity
     */
    public String getIdentity() {
        if ( identity == null ) {
            throw new IllegalArgumentException( "You have to define the signing identity" );
        }
        return identity;
    }

    /**
     * Set the Identity to sign with.
     * This is the "Common Name" part from the certificate
     * @param identity to sign with
     */
    public void setIdentity(String identity) {
        this.identity = identity.replaceFirst( "Installer", "Application" );
    }

    /**
     * Return the Identity to sign with
     * This is the "Common Name" part from the certificate
     * @return identity
     */
    public String getProductIdentity() {
        if ( productIdentity == null && identity != null ) {
            System.out.println( "No product identity given, trying to use the application identity by replacing 'Application' with 'Installer'" );
            return identity.replaceFirst( "Application", "Installer" );
        } else if ( productIdentity == null ) {
            throw new IllegalArgumentException( "You have to define the signing identity" );
        }
        return productIdentity;
    }

    /**
     * Set the Identity to sign with.
     * This is the "Common Name" part from the certificate
     * @param identity to sign with
     */
    public void setProductIdentity(String identity) {
        this.productIdentity = identity;
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
     * The password to unlock the keychain
     * @return the keychainPassword
     */
    public String getKeychainPassword() {
        return keychainPassword;
    }

    /**
     * Set the keychain password to unlock the keychain
     * @param keychainPassword the keychainPassword to set
     */
    public void setKeychainPassword(String keychainPassword) {
        this.keychainPassword = keychainPassword;
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
     * Unlocks the keychain if the password is not null.
     * Will unlock the default login.keychain if no other is set.
     */
    private void unlockKeychain() {
        if ( getKeychainPassword() == null ) {
            return;
        }

        // unlock keychain
        String keychain = getKeychain() != null ? getKeychain() : System.getenv("HOME") + "/Library/Keychains/login.keychain";
        exec( isIgnoreError(), "security", "-v", "unlock-keychain", "-p", getKeychainPassword(), keychain);
    }

    /**
     * Signed an application package
     * @param path of the application
     */
    public void signApplication( File path ) {

        unlockKeychain();

        // Codesign
        ArrayList<String> command = new ArrayList<>();
        command.add( "codesign" );
        command.add( "-f" );
        command.add( "--timestamp" );

        if ( isDeepsign() ) {
            command.add( "--deep" );
        }

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
        
        if ( isHardened() ) {
            command.add( "--options" );
            command.add( "runtime" );
        }

        File entitlementsTempFile = null;
        String entitlements = getEntitlements();
        if ( entitlements != null && entitlements.length() > 0 ) {
            try {
                entitlementsTempFile = Files.createTempFile( "signing", "entitlement" ).toFile();
                try ( FileOutputStream stream = new FileOutputStream( entitlementsTempFile ) ) {
                    stream.write( entitlements.getBytes() );
                }

                command.add( "--entitlements" );
                command.add( entitlementsTempFile.getAbsolutePath() );
            } catch( IOException e ) {
                System.err.println( "Could not use the entitlements file" );
            }
        }

        command.add( path.getAbsolutePath() );
        exec( command, null, null, isIgnoreError() );

        if ( entitlementsTempFile != null && entitlementsTempFile.exists() ) {
            entitlementsTempFile.delete();
        }
    }

    /**
     * Signed a product package
     * @param path of the application
     */
    public void signProduct( File path ) {

        unlockKeychain();

        // Productsign
        ArrayList<String> command = new ArrayList<>();
        command.add( "productsign" );
        command.add( "--sign" );
        command.add( getProductIdentity() );

        if ( getKeychain() != null ) {
            command.add( "--keychain" );
            command.add( getKeychain() );
        }

        command.add( path.getAbsolutePath() );

        File output = new File( path.getParentFile(), "signed." + path.getName() );
        command.add( output.getAbsolutePath() );
        exec( command, null, null, isIgnoreError() );

        // Move to old directory
        if ( output.exists() && path.delete() ) {
            output.renameTo( path );
        }
    }

    /**
     * Should be deep signed?
     * @return if deep signing is enabled
     */
    public boolean isDeepsign() {
        return deepsign;
    }

    /**
     * Set deep signing
     * @param deepsign deep sign?
     */
    public void setDeepsign(boolean deepsign) {
        this.deepsign = deepsign;
    }

    /**
     * Set true (default=true) to enabled hardened signing for applications. This is required prior to notarization
     * @param hardened true if hardening is required
     */
    public void setHardened( boolean hardened ) {
        this.hardened = hardened;
    }
    
    /**
     * Returns true if the signing process should add the option to sign hardened
     * @return true if the signing process should add the option to sign hardened
     */
    public boolean isHardened() {
        return hardened;
    }

    /**
     * Returns entitlements set to use while processing the application
     * Will only be used for the ".app" files, not the ".pkg" files
     * @return entitlements set to use while processing the application
     */
    public String getEntitlements() {
        return entitlements;
    }

    /**
     * Set an XML content string used while signing the main application bundle
     * @param entitlements XML content string used while signing the main application bundle
     */
    public void setEntitlements( String entitlements ) {
        this.entitlements = entitlements;
    }
}
