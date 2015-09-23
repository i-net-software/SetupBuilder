/*
 * Copyright 2015 i-net software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.inet.gradle.setup.msi;

import groovy.lang.Closure;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.gradle.api.internal.file.CopyActionProcessingStreamAction;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.util.ConfigureUtil;

import com.inet.gradle.setup.AbstractSetupTask;

/**
 * The msi Gradle task. It build a msi setup for Windows.
 * 
 * @author Volker Berlin
 */
public class Msi extends AbstractSetupTask {

    private String   arch;

    private Object   bannerBmp, dialogBmp, wxsTemplate;

    private List<String> languages;

    private SignTool signTool;

    /**
     * Create a new instance.
     */
    public Msi() {
        super( "msi" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void build() {
        ProjectInternal project = (ProjectInternal)getProject();
        new MsiBuilder( this, getSetupBuilder(), project.getFileResolver() ).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void processFiles( CopyActionProcessingStreamAction action ) {
        super.processFiles( action );
    }

    /**
     * Get the architecture of the installer.
     * @return the architecture
     */
    public String getArch() {
        if( arch != null ) {
            return arch;
        }
        return "x64";
    }

    /**
     * Set the architecture of the setup. The default is x64. Possible values are:
     * <ul>
     * <li>x86
     * <li>x64
     * <li>ia64
     * </ul>
     * 
     * @param arch the architecture
     */
    public void setArch( String arch ) {
        this.arch = arch;
    }

    /**
     * if the setup is a 64 bit setup.
     * 
     * @return true, if 64 bit.
     */
    boolean is64Bit() {
        switch( getArch() ) {
            case "x64":
            case "ia64":
                return true;
            default:
                return false;
        }
    }

    /**
     * Get the file of the banner or null
     * @return the file
     */
    public File getBannerBmp() {
        if( bannerBmp != null ) {
            return getProject().file( bannerBmp );
        }
        return null;
    }

    /**
     * Set a file with a banner BMP. The typical size is 493 x 58
     * 
     * @param bannerBmp the file
     */
    public void setBannerBmp( Object bannerBmp ) {
        this.bannerBmp = bannerBmp;
    }

    /**
     * Get the banner BMP.
     * 
     * @return the BMP
     */
    public File getDialogBmp() {
        if( dialogBmp != null ) {
            return getProject().file( dialogBmp );
        }
        return null;
    }

    /**
     * Set a file with the dialog BMP. The typical size is 493 x 312
     * 
     * @param dialogBmp the file
     */
    public void setDialogBmp( Object dialogBmp ) {
        this.dialogBmp = dialogBmp;
    }

    /**
     * Set the needed information for signing the setup.
     * 
     * @param closue the data for signing
     */
    public void signTool( Closure<SignTool> closue ) {
        signTool = ConfigureUtil.configure( closue, new SignTool() );
    }

    /**
     * Get the SignTool configuration if set
     * 
     * @return the settings or null
     */
    public SignTool getSignTool() {
        return signTool;
    }

    /**
     * Get a URL to a *.wxs file for the WIX Toolset
     * 
     * @return the template
     * @throws MalformedURLException if any error occur
     */
    public URL getWxsTemplate() throws MalformedURLException {
        if( wxsTemplate != null ) {
            return getProject().file( wxsTemplate ).toURI().toURL();
        }
        return getClass().getResource( "template.wxs" );
    }

    /**
     * Set an optional *.wxs file as template for building the final file with the settings of the gradle task. This
     * template give you the option to set things that are not available via the gradle task.
     * 
     * @param wxsTemplate the file location
     */
    public void setWxsTemplate( Object wxsTemplate ) {
        this.wxsTemplate = wxsTemplate;
    }

    /**
     * Get the languages that the setup should be support. The list must at minimum one element.
     * @return a list of languages
     */
    public List<MsiLanguages> getLanguages() {
        ArrayList<MsiLanguages> result = new ArrayList<>();
        if( languages == null ) {
            Collections.addAll( result, MsiLanguages.values() );
            return result;
        }

        for( String lang : languages ) {
            String key = lang.replace( '-', '_' ).toLowerCase();
            MsiLanguages value = null;
            try {
                value = MsiLanguages.valueOf( key );
            } catch( IllegalArgumentException ex ) {
                // The completely name was not found.
                // now we check if this is only a language without a country 
                for( MsiLanguages msiLanguage : MsiLanguages.values() ) {
                    if( msiLanguage.toString().startsWith( key ) ) {
                        value = msiLanguage;
                        break;
                    }
                }
                if( value == null ) {
                    throw ex; // not supported language
                }
            }
            result.add( value );
        }

        if( result.isEmpty() ) {
            result.add( MsiLanguages.en_us );
        }
        return result;
    }

    /**
     * Set a list of languages for the setup GUI. For example "en-us". By default all available languages will be added to the setup. This can need some time on creation.
     * @param languages the languages
     */
    public void setLanguages( Object languages ) {
        List<String> list;
        if( languages == null ) {
            list = null;
        } else if( languages instanceof Iterable ) {
            list = new ArrayList<>();
            for( Object str : (Iterable<?>)languages ) {
                list.add( str.toString() );
            }
        } else {
            list = Arrays.asList( languages.toString() );
        }

        this.languages = list;
    }
}
