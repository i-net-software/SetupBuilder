package com.inet.gradle.setup.msi;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.gradle.util.ConfigureUtil;

import com.inet.gradle.setup.SetupBuilder;

import groovy.lang.Closure;

/**
 * Stub Object for localized resources
 *
 * @author Gerry Weißbach
 *
 */
public class MsiLocalizedResource {

    private MsiLanguages locale;

    private Object       resource;

    private SetupBuilder setup;

    private boolean      overridable;

    private File         temporaryDirectory;

    /**
     * Stub Object for localized resources
     *
     * @param setup the setup
     * @param temporaryDirectory the temporary directory of the task to store resources
     */
    public MsiLocalizedResource( SetupBuilder setup, File temporaryDirectory ) {
        this.setup = setup;
        this.temporaryDirectory = temporaryDirectory;
    }

    /**
     * @return the resource as WXL File
     */
    public File getResource() {

        if( resource == null ) {
            return null;
        }

        File input;
        if( !(resource instanceof File) ) {
            input = setup.getProject().file( resource );
        } else {
            input = (File)resource;
        }

        try {
            File wxlFile = new File( temporaryDirectory, "i18n-" + input.getName() + "." + UUID.randomUUID().toString() + ".wxl" );
            if( wxlFile.exists() ) {
                return wxlFile;
            }

            wxlFile.getParentFile().mkdirs();
            Properties props = new Properties();
            props.load( Files.newInputStream( input.toPath() ) );

            StringBuilder builder = new StringBuilder();
            builder.append( "<?xml version='1.0'?>\n" );
            builder.append( "<WixLocalization xmlns='http://schemas.microsoft.com/wix/2006/localization' Codepage='utf-8' " );

            boolean isDefaultLanguage = locale.getLangID().equalsIgnoreCase( MsiLanguages.getMsiLanguage( setup.getDefaultResourceLanguage() ).getLangID() );
            boolean overridable = isDefaultLanguage || this.overridable;
            if( !isDefaultLanguage ) {
                builder.append( "Culture='" + locale.getCulture() + "'" );
            }

            builder.append( ">\n" );

            for( Object key : props.keySet() ) {

                String value = props.getProperty( (String)key );
                builder.append( "\t<String Id='" );
                builder.append( key );

                if ( overridable ) {
                    builder.append( "' Overridable='yes" );
                }

                builder.append( "'>" );
                builder.append( value );
                builder.append( "</String>\n" );
            }

            builder.append( "</WixLocalization>" );
            Files.write( wxlFile.toPath(), builder.toString().getBytes( StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE );
            return wxlFile;
        } catch( IOException e ) {
            return null;
        }
    }

    /**
     * @return the locale
     */
    public MsiLanguages getLocale() {
        return locale;
    }

    /**
     * @param locale the locale to set
     */
    public void setLocale( String locale ) {
        this.locale = MsiLanguages.getMsiLanguage( locale );
    }

    /**
     * @param resource the resource to set
     */
    public void setResource( Object resource ) {
        this.resource = resource;
    }

    /**
     * @return the overrideable
     */
    public boolean isOverridable() {
        return overridable;
    }

    /**
     * @param overrideable the overrideable to set
     */
    public void setOverridable( boolean overrideable ) {
        this.overridable = overrideable;
    }

    /**
     * Return the localized file for a specific locale
     *
     * @param list from which to receive the resource
     * @param locale for which to get the file
     * @return localized file
     */
    public static File getLocalizedResourceFile( List<MsiLocalizedResource> list, String locale ) {

        for( MsiLocalizedResource res : list ) {
            if( res.locale.getLangID().equalsIgnoreCase( locale ) ) {
                return res.getResource();
            }
        }

        return null;
    }

    /**
     * Set a localized file
     *
     * @param parent the setup builder
     * @param temporaryDirectory the temporary directory to put the resource is
     * @param holder the list to add the entry to
     * @param resource file file or closure
     */
    public static void addLocalizedResource( SetupBuilder parent, File temporaryDirectory, List<MsiLocalizedResource> holder, Object resource ) {

        MsiLocalizedResource res = new MsiLocalizedResource( parent, temporaryDirectory );
        if( resource instanceof Closure<?> ) {
            res = ConfigureUtil.configure( (Closure<?>)resource, res );
        } else {
            res.setLocale( parent.getDefaultResourceLanguage() );
            res.setResource( resource );
        }

        holder.add( res );
    }


    /**
     * Returns the file for a localized license
     * @param temporaryDirectory the directory
     * @param lang the language
     * @return the localized license file
     */
    public static File localizedRtfFile( File temporaryDirectory, MsiLanguages lang ) {
        return new File( temporaryDirectory, "i18n/" + "license-" + lang.getCulture().replaceAll( "[^a-zA-Z0-9]", "_" ) + ".rtf" );
    }
}
