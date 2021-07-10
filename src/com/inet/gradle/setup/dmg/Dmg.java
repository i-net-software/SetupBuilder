/*
 * Copyright 2015 i-net software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.inet.gradle.setup.dmg;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.tools.ant.types.FileSet;
import org.gradle.api.Action;
import org.gradle.api.internal.project.ProjectInternal;

import com.inet.gradle.appbundler.OSXCodeSign;
import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.abstracts.AbstractUnixSetupTask;
import com.inet.gradle.setup.abstracts.LocalizedResource;
import com.inet.gradle.setup.abstracts.Service;
import com.inet.gradle.setup.util.GradleUtils;

import groovy.lang.Closure;

/**
 * The dmg Gradle task. It build a dmg package for Mac.
 *
 * @author Volker Berlin
 */
public class Dmg extends AbstractUnixSetupTask {

    private Object                         backgroundImage, setupDarkBackground, setupBackground, setupIcon;

    private int                            windowWidth                  = 400, windowHeight = 300, iconSize = 128, fontSize = 14, windowWidthCorrection = 0, windowHeightCorrection = 22;

    private Color                          backgroundColor;

    private String                         applicationIdentifier;

    private boolean                        aquaSystemAppearanceRequired = false;

    private OSXCodeSign<Dmg, SetupBuilder> codeSign;

    private List<LocalizedResource>        welcomePages                 = new ArrayList<>();

    private List<LocalizedResource>        conclusionPages              = new ArrayList<>();

    private List<PreferencesLink>          preferencesLink              = new ArrayList<>();

    final List<OSXApplicationBuilder>      appBuilders                  = new ArrayList<>();

    private List<String>                   jreIncludes                  = Arrays.asList( new String[] {
                    "bin/java",
                    "lib/",
                    "COPYRIGHT",
                    "LICENSE",
                    "README",
                    "THIRDPARTYLICENSEREADME-JAVAFX.txt",
                    "THIRDPARTYLICENSEREADME.txt",
                    "Welcome.html"
    } );

    private List<String>                   jreExcludes                  = Arrays.asList( new String[] {
                    "lib/deploy/",
                    "lib/deploy.jar",
                    "lib/javaws.jar",
                    "lib/libdeploy.dylib",
                    "lib/libnpjp2.dylib",
                    "lib/plugin.jar",
                    "lib/security/javaws.policy"
    } );

    private List<Object>                   nativeLibraries              = new ArrayList<>();

    /**
     * Create the task.
     */
    public Dmg() {
        super( "dmg" );
        getProject().afterEvaluate( ( project ) -> {
            // if the "dmg" task should be executed then create some possible extra tasks on the end of the configuration phase
            boolean isExecute = GradleUtils.isTaskExecute( Dmg.this, project );
            if( isExecute ) {
                SetupBuilder setup = getSetupBuilder();
                for( Service service : setup.getServices() ) {
                    ProjectInternal projInternal = (ProjectInternal)project;
                    OSXApplicationBuilder builder = new OSXApplicationBuilder( Dmg.this, setup, projInternal.getFileResolver() );
                    builder.configSubTasks( service );
                    appBuilders.add( builder );
                }
            }
        } );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void build() {
        ProjectInternal project = (ProjectInternal)getProject();
        new DmgBuilder( this, getSetupBuilder(), project.getFileResolver() ).build();
    }

    /**
     * Return width of Finder view INCLUDING the correction settings
     *
     * @return width of Finder view
     */
    public int getWindowWidth() {
        return windowWidth + windowWidthCorrection;
    }

    /**
     * Set width of Finder view
     *
     * @param windowWidth width of Finder view
     */
    public void setWindowWidth( int windowWidth ) {
        this.windowWidth = windowWidth;
    }

    /**
     * Return correction width of Finder view
     * This may be needed if there is a background-image
     * set which does not take the borders of the window into account
     *
     * @return width correction of Finder view
     */
    public int getWindowWidthCorrection() {
        return windowWidthCorrection;
    }

    /**
     * Set correction width of Finder view
     * This may be needed if there is a background-image
     * set which does not take the borders of the window into account
     *
     * @param windowWidthCorrection width of Finder view
     */
    public void setWindowWidthCorrection( int windowWidthCorrection ) {
        this.windowWidthCorrection = windowWidthCorrection;
    }

    /**
     * Return height of Finder view INCLUDING the correction settings
     *
     * @return height of Finder view
     */
    public int getWindowHeight() {
        return windowHeight + windowHeightCorrection;
    }

    /**
     * Set height of Finder view
     *
     * @param windowHeight of Finder view
     */
    public void setWindowHeight( int windowHeight ) {
        this.windowHeight = windowHeight;
    }

    /**
     * Return correction height of Finder view
     * This may be needed if there is a background-image
     * set which does not take the borders of the window into account
     *
     * @return width correction of Finder view
     */
    public int getWindowHeightCorrection() {
        return windowHeightCorrection;
    }

    /**
     * Set correction height of Finder view
     * This may be needed if there is a background-image
     * set which does not take the borders of the window into account
     *
     * @param windowHeightCorrection height of Finder view
     */
    public void setWindowHeightCorrection( int windowHeightCorrection ) {
        this.windowHeightCorrection = windowHeightCorrection;
    }

    /**
     * Return size of icons in Finder view
     *
     * @return size of icons in Finder view
     */
    public int getIconSize() {
        return iconSize;
    }

    /**
     * Set size of icons in Finder view
     *
     * @param iconSize of icons in Finder view
     */
    public void setIconSize( int iconSize ) {
        this.iconSize = iconSize;
    }

    /**
     * Return background Image for Finder View
     *
     * @return background Image for Finder View
     */
    public File getBackgroundImage() {
        if( backgroundImage != null ) {
            return getProject().file( backgroundImage );
        }
        return null;
    }

    /**
     * Set background Image for Finder View
     *
     * @param backgroundFile Image for Finder View
     */
    public void setBackgroundImage( File backgroundFile ) {
        this.backgroundImage = backgroundFile;
    }

    /**
     * Return font size for Finder View
     *
     * @return font size for Finder View
     */
    public int getFontSize() {
        return fontSize;
    }

    /**
     * Set font size for Finder View
     *
     * @param fontSize size for Finder View
     */
    public void setFontSize( int fontSize ) {
        this.fontSize = fontSize;
    }

    /**
     * Set the needed information for signing the setup.
     *
     * @param action the data for signing
     */
    public void setCodeSign( Action<? super OSXCodeSign<? super Dmg,? super SetupBuilder>> action ) {
        ProjectInternal project = (ProjectInternal)getProject();
        codeSign = new OSXCodeSign<>(this, project.getFileResolver());
        action.execute(codeSign);
    }

    /**
     * Get the SignTool configuration if set
     *
     * @return the settings or null
     */
    public OSXCodeSign<Dmg, SetupBuilder> getCodeSign() {
        return codeSign;
    }

    /**
     * Return the welcome page list
     * Allowed Format: rtf, rtfd, txt, html
     *
     * @return welcome page
     */
    public List<LocalizedResource> getConclusionPages() {
        return conclusionPages;
    }

    /**
     * Set the welcome page
     * Allowed Format: rtf, rtfd, txt, html
     *
     * @param conclusionPage which is shown at the end
     */
    public void conclusionPage( Object conclusionPage ) {
        LocalizedResource.addLocalizedResource( getSetupBuilder(), conclusionPages, conclusionPage );
    }

    /**
     * Return the welcome page list
     * Allowed Format: rtf, rtfd, txt, html
     *
     * @return welcome page
     */
    public List<LocalizedResource> getWelcomePages() {
        return welcomePages.size() > 0 ? welcomePages : getSetupBuilder().getLongDescriptions();
    }

    /**
     * Set the welcome page
     * Allowed Format: rtf, rtfd, txt, html
     *
     * @param welcomePage welcome page file
     */
    public void welcomePage( Object welcomePage ) {
        LocalizedResource.addLocalizedResource( getSetupBuilder(), welcomePages, welcomePage );
    }

    /**
     * Return the background image for the setup
     *
     * @return background image
     */
    public File getSetupBackgroundImage() {
        if( setupBackground != null ) {
            return getProject().file( setupBackground );
        }
        return null;
    }

    /**
     * Set the background image for the setup
     * 
     * @param setupBackground to set
     */
    public void setSetupBackgroundImage( Object setupBackground ) {
        this.setupBackground = setupBackground;
    }

    /**
     * Returns a dark background image for the package installer
     * 
     * @return a dark background image for the package installer
     */
    public File getSetupDarkBackgroundImage() {
        if( setupDarkBackground != null ) {
            return getProject().file( setupDarkBackground );
        }
        return null;
    }

    /**
     * Set the dark background image for the package installer
     * 
     * @param setupDarkBackground to set
     */
    public void setSetupDarkBackgroundImage( Object setupDarkBackground ) {
        this.setupDarkBackground = setupDarkBackground;
    }

    /**
     * Returns the setup icon
     * 
     * @return the setupIcon
     */
    public Object getSetupIcon() {
        if( setupIcon == null ) {
            return getSetupBuilder().getIcons();
        }
        return setupIcon;
    }

    /**
     * Set up the setup icon
     * 
     * @param setupIcon the setupIcon to set
     */
    public void setSetupIcon( Object setupIcon ) {
        this.setupIcon = setupIcon;
    }

    /**
     * Return the list of preferences links
     * 
     * @return preferences links
     */
    public List<PreferencesLink> getPreferencesLinks() {
        return preferencesLink;
    }

    /**
     * Set a preferences link
     * 
     * @param action the link
     */
    public void preferencesLink( Action<? super PreferencesLink> action ) {
        final PreferencesLink link = new PreferencesLink();
        action.execute(link);
        preferencesLink.add(link);
    }

    /**
     * Get a list of string - defining files - that should be included in the bundled JRE
     * 
     * @return the jreIncludes
     */
    public List<String> getJreIncludes() {
        return jreIncludes;
    }

    /**
     * Set a list of string - defining files - that should be included in the bundled JRE
     * 
     * @param jreIncludes the jreIncludes to set
     */
    public void setJreIncludes( List<String> jreIncludes ) {
        this.jreIncludes = jreIncludes;
    }

    /**
     * Get a list of string - defining files - that should be excluded from the bundled JRE
     * 
     * @return the jreExclude
     */
    public List<String> getJreExcludes() {
        return jreExcludes;
    }

    /**
     * Set a list of string - defining files - that should be excluded from the bundled JRE
     * 
     * @param jreExclude the jreExclude to set
     */
    public void setJreExcludes( List<String> jreExclude ) {
        this.jreExcludes = jreExclude;
    }

    /**
     * Returns the converted background color as apple script string
     * 
     * @return the backgroundColor as apple script color string
     */
    public String getBackgroundColor() {
        if( backgroundColor == null ) {
            // Fallback
            backgroundColor = new Color( 255, 255, 255 );
        }

        return "{" +
                        String.join( ", ", Arrays.asList( String.valueOf( backgroundColor.getRed() * 257 ), String.valueOf( backgroundColor.getGreen() * 257 ), String.valueOf( backgroundColor.getBlue() * 257 ) ) ) +
                        "}";
    }

    /**
     * @param backgroundColor the backgroundColor to set
     */
    public void setBackgroundColor( Color backgroundColor ) {
        this.backgroundColor = backgroundColor;
    }

    /**
     * Returns an application identifier set for the DMG builder.
     * It is being used as ID in the Info.plist
     * 
     * @param setup the SetupBuilder instance for a fallback
     * @return the application identifier for macOS
     */
    public String getApplicationIdentifier( SetupBuilder setup ) {
        if( applicationIdentifier == null || applicationIdentifier.isEmpty() ) {
            if( setup.getMainClass() == null || setup.getMainClass().isEmpty() ) {
                return setup.getAppIdentifier();
            }
            return setup.getMainClass();
        }
        return applicationIdentifier;
    }

    /**
     * Sets an application identifier for the DMG builder.
     * It is being used as ID in the Info.plist
     * 
     * @param applicationIdentifier the application identifier for macOS
     */
    public void setApplicationIdentifier( String applicationIdentifier ) {
        this.applicationIdentifier = applicationIdentifier;
    }

    /**
     * Returns true, if the application requires the aqua system appearance. False by default
     * 
     * @return true, if the application requires the aqua system appearance.
     */
    public boolean isAquaSystemAppearanceRequired() {
        return aquaSystemAppearanceRequired;
    }

    /**
     * Define, that the application requires the system appearance. False by default
     * 
     * @param aquaSystemAppearanceRequired true, if the system appearance is required.
     */
    public void setAquaSystemAppearanceRequired( boolean aquaSystemAppearanceRequired ) {
        this.aquaSystemAppearanceRequired = aquaSystemAppearanceRequired;
    }
    
    /**
     * add a native library using any gradle compatible file syntax
     * @param library a native library to add
     */
    public void nativeLibraries( Object library ) {
        nativeLibraries.add( library );
    }
    
    /**
     * Returns the list of native libraries set for the current project
     * @return the list of native libraries set for the current project
     */
    public List<FileSet> getNativeLibraries() {
        return nativeLibraries.stream().map( e -> {
            FileSet set = new FileSet();
            set.setDir( getProject().file( e ) );
            return set;
        } ).collect( Collectors.toList() );
    }
}
