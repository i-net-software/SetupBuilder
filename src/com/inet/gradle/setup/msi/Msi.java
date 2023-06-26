/*
 * Copyright 2015 -2016 i-net software
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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.internal.file.CopyActionProcessingStreamAction;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.util.ConfigureUtil;

import com.inet.gradle.setup.abstracts.AbstractSetupTask;
import com.inet.gradle.setup.util.ResourceUtils;

import groovy.lang.Closure;
import org.gradle.api.tasks.Optional;

/**
 * The msi Gradle task. It build a msi setup for Windows.
 *
 * @author Volker Berlin
 */
public class Msi extends AbstractSetupTask {

    private String                     arch;

    private boolean                    only32bit;

    private Object                     bannerBmp, dialogBmp, wxsTemplate, multiInstanceScript;

    private List<String>               languages;

    private SignTool                   signTool;

    private double                     minOS;

    private List<Launch4j>             launch4j           = new ArrayList<>();

    private int                        multiInstanceCount = 1;

    private InstallScope               installScope;

    private List<String>               preGUI             = new ArrayList<String>();

    private List<String>               wixextensions      = new ArrayList<String>();

    // Will generate a checkbox.
    private boolean                    runAfterIsOptional = false;

    private List<MsiLocalizedResource> i18n               = new ArrayList<>();

    private List<File>                 externals = new ArrayList<>();

    private boolean                    skipValidation = false;

    private Closure<?>                 externalSignTool = null; 

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

        try {
            // Extract default language Resources
            MsiLocalizedResource resource = new MsiLocalizedResource( getSetupBuilder(), getTemporaryDir() );
            resource.setLocale( "en" );
            resource.setOverridable( true );
            resource.setResource( ResourceUtils.extract( getClass(), "i18n/language-en.properties", getTemporaryDir() ) ) ;
            i18n.add( resource );

            resource = new MsiLocalizedResource( getSetupBuilder(), getTemporaryDir() );
            resource.setLocale( "de" );
            resource.setOverridable( true );
            resource.setResource( ResourceUtils.extract( getClass(), "i18n/language-de.properties", getTemporaryDir() ) );
            i18n.add( resource );
        } catch( IOException e ) {
            throw new GradleException( "Could not extract required ressources.", e );
        }

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
     * If this installer should run only on windows 32 bit versions.
     *
     * @return true, if only 32 bit
     */
    @Input
    boolean isOnly32Bit() {
        return only32bit;
    }

    /**
     * Get the architecture of the installer.
     * @return the architecture
     */
    @Input
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
     * <li>x86-only - does not run on a 64 bit system
     * </ul>
     *
     * @param arch the architecture
     */
    public void setArch( String arch ) {
        if( "x86-only".equals( arch ) ) {
            this.arch = "x86";
            this.only32bit = true;
        } else {
            this.arch = arch;
            this.only32bit = false;
        }
    }

    /**
     * if the setup is a 64 bit setup.
     *
     * @return true, if 64 bit.
     */
    @Input
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
    @InputFile
    @Optional
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
    @InputFile
    @Optional
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
     * Set the needed information for signing the setup.
     *
     * @param action the data for signing
     */
    public void signTool( Action<? super SignTool> action ) {
        signTool = new SignTool();
        action.execute(signTool);
    }

    /**
     * Get the SignTool configuration if set
     *
     * @return the settings or null
     */
    @Input
    @Optional
    public SignTool getSignTool() {
        return signTool;
    }

    /**
     * Get a URL to a *.wxs file for the WIX Toolset
     *
     * @return the template
     * @throws MalformedURLException if any error occur
     */
    @Input
    public URL getWxsTemplateURL() throws MalformedURLException {
        if( wxsTemplate != null ) {
            return getProject().file( wxsTemplate ).toURI().toURL();
        }
        return getClass().getResource( "template.wxs" );
    }

    /**
     * Get a file to a *.wxs file for the WIX Toolset
     *
     * @return the template
     * @throws MalformedURLException if any error occur
     */
    @InputFile
    @Optional
    public File getWxsTemplate() throws MalformedURLException {
        if( wxsTemplate != null ) {
            return getProject().file( wxsTemplate );
        }
        return null;
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
    @Input
    public List<MsiLanguages> getLanguages() {
        ArrayList<MsiLanguages> result = new ArrayList<>();
        if( languages == null ) {
            Collections.addAll( result, MsiLanguages.values() );
            return result;
        }

        for( String lang : languages ) {
            result.add( MsiLanguages.getMsiLanguage( lang ) );
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

    /**
     * Get the minimum OS version.
     *
     * @return the version
     */
    @Input
    public double getMinOS() {
        return minOS;
    }

    /**
     * Set the minimum OS version.
     * <ul>
     * <li>10.0 - Windows 10
     * <li>10.0 - Windows Server 2016
     * <li> 6.3 - Windows 8.1
     * <li> 6.3 - Windows Server 2012 R2
     * <li> 6.2 - Windows 8
     * <li> 6.2 - Windows Server 2012
     * <li> 6.1 - Windows 7
     * <li> 6.1 - Windows Server 2008 R2
     * <li> 6.0 - Windows Server 2008
     * <li> 6.0 - Windows Vista
     * <li> 5.2 - Windows Server 2003 R2
     * <li> 5.2 - Windows Server 2003
     * <li> 5.2 - Windows XP 64-Bit Edition
     * <li> 5.1 - Windows XP
     * <li> 5.0 - Windows 2000
     * </ul>
     *
     * @param minVersion the version
     */
    public void setMinOS( double minVersion ) {
        this.minOS = minVersion;
    }

    /**
     * Register a lauch4j configuration.
     *
     * @param closue the closure of the launch4j definition
     */
    public void launch4j( Closure<Launch4j> closue ) {
        Launch4j service = ConfigureUtil.configure( closue, new Launch4j( getSetupBuilder() ) );
        launch4j.add( service );
    }
    
    /**
     * Register a lauch4j configuration. Every configuration create an *.exe file with the given settings.
     *
     * @param action the closure of the launch4j definition
     */
    public void launch4j( Action<Launch4j> action ) {
        Launch4j launcher = new Launch4j( getSetupBuilder() );
        action.execute(launcher);
        launch4j.add( launcher );
    }

    /**
     * Returns the registered launcher.
     *
     * @return the registered launcher
     */
    @Input
    public List<Launch4j> getLaunch4js() {
        return launch4j;
    }

    /**
     * Get the count of possible multiple instances.
     *
     * @return instance count
     */
    @Input
    public int getMultiInstanceCount() {
        return multiInstanceCount;
    }

    /**
     * Set the count of possible multiple instances. The default is 1. A value lesser or equals 1 result in a single
     * instance setup.
     *
     * @param instanceCount the current instance count
     */
    public void setMultiInstanceCount( int instanceCount ) {
        this.multiInstanceCount = instanceCount;
    }

    /**
     * Get the URL to a vbscript that set the instance name.
     *
     * @return the URL
     * @throws MalformedURLException if any error occur
     */
    @Input
    public URL getMultiInstanceScript() throws MalformedURLException {
        if( multiInstanceScript != null ) {
            return getProject().file( multiInstanceScript ).toURI().toURL();
        }
        return getClass().getResource( "MultiInstance.vbs" );
    }

    /**
     * Set a vbscript that can change the product name, instance id and other things on a multi instance installation.
     *
     * @param multiInstanceScript the script
     */
    public void setMultiInstanceScript( Object multiInstanceScript ) {
        this.multiInstanceScript = multiInstanceScript;
    }

    /**
     * Possible values for install scope
     */
    public static enum InstallScope {
        perUser, perMachine
    }

    /**
     * Get the install scope. Never null.
     *
     * @return the scope
     */
    @Input
    public InstallScope getInstallScope() {
        if( installScope != null ) {
            return installScope;
        }
        return InstallScope.perMachine;
    }

    /**
     * Set the install scope. Possible values are perUser and perMachine. If not set then it is perMachine.
     * A perUser installer has no elevated rights and can not installed to program files.
     *
     * @param installScope the new scope
     */
    public void setInstallScope( InstallScope installScope ) {
        this.installScope = installScope;
    }

    /**
     * Returns the preGUI scripts.
     *
     * @return the scripts
     */
    @Input
    public List<String> getPreGUI() {
        return preGUI;
    }

    /**
     * Set a vbscript or jscript that should be executed before the GUI is displayed. This can be used for setting
     * a default install directory. This action has no elevated rights.
     *
     * @param script list or single script
     */
    public void setPreGUI( Object script ) {
        preGUI.clear();
        preGUI( script );
    }

    /**
     * Add a vbscript or jscript that should be executed before the GUI is displayed. This can be used for setting
     * a default install directory. This action has no elevated rights.
     *
     * @param script the content for the entry
     */
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public void preGUI( Object script ) {
        if( script == null ) {
            // nothing
        } else if( script instanceof Collection ) {
            preGUI.addAll( (Collection)script );
        } else {
            preGUI.add( script.toString() );
        }
    }

    /**
     * Returns if the runAfter is optional
     * @return the runAfterIsOptional
     */
    @Input
    public boolean isRunAfterOptional() {
        return runAfterIsOptional;
    }

    /**
     * Set that the runAfter is optional
     * @param runAfterIsOptional the runAfterIsOptional to set
     */
    public void setRunAfterIsOptional( boolean runAfterIsOptional ) {
        this.runAfterIsOptional = runAfterIsOptional;
    }

    /**
     * add a localized resource to the project.
     * The files have to bi in the property file format and will be converted to wixLocalization files
     *
     * @param localization the localization object
     */
    public void i18n( Object localization ) {
        MsiLocalizedResource.addLocalizedResource( getSetupBuilder(), getTemporaryDir(), i18n, localization );
    }

    /**
     * return the file for optional language resource locations
     * @return the localized resources
     */
    @Input
    public List<MsiLocalizedResource> getI18n() {
        return i18n;
    }

    /**
     * Adds reference to external wsx file
     * @param external external  wsx file
     * @return this Msi instance
     */
    public Msi external(File external) {
        externals.add(external);
        return this;
    }

    /**
     * Sets references to external wsx files
     * @param externals external  wsx files
     * @return this Msi instance
     */
    public Msi  setExternals(Iterable<File> externals) {
        this.externals.clear();
        for (File external : externals) {
            this.externals.add(external);
        }
        return this;
    }

    /**
     * @return registered  external  wsx files
     */
    @InputFiles
    public List<File> getExternals() {
        return externals;
    }
    
    /**
     * Sets list of wix extension dll's
     * @param wixextensions extension dll's
     * @return this Msi instance
     */
    public Msi setWixExtensions(Iterable<String> wixextensions) {
        this.wixextensions.clear();
        for (String wixextension : wixextensions) {
            this.wixextensions.add(wixextension);
        }
        return this;
    }

    
    
    /**
     * @return registered extensions
     */
    @Input
    public List<String> getWixExtensions() {
        return wixextensions;
    }

    /**
     * Returns if the skipValidation is requested
     * @return the skipValidation
     */
    @Input
    public boolean isSkipValidation() {
        return skipValidation;
    }

    /**
     * Set that the skipValidation is requested
     * @param skipValidation the skipValidation to set
     */
    public void setSkipValidation( boolean skipValidation ) {
        this.skipValidation = skipValidation;
    }

    /**
     * Set a closure with an external sign tool configuration.
     *
     * @param externalSignTool the data for signing
     */
    public void setExternalSignTool( Closure<?> externalSignTool ) {
        this.externalSignTool = externalSignTool;
    }

    /**
     * Get a closure with an external sign tool configuration.
     *
     * @return closure the data for signing
     */
    @Internal
    public Closure<?> getExternalSignTool() {
        return externalSignTool;
    }
}
