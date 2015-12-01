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
package com.inet.gradle.setup.dmg;

import java.io.File;

import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.util.ConfigureUtil;

import com.inet.gradle.appbundler.OSXCodeSign;
import com.inet.gradle.setup.AbstractSetupTask;
import com.inet.gradle.setup.SetupBuilder;

import groovy.lang.Closure;

/**
 * The dmg Gradle task. It build a dmg package for Mac.
 * 
 * @author Volker Berlin
 */
public class Dmg extends AbstractSetupTask {

    private File backgroundImage;
    private Integer windowWidth = 400, windowHeight = 300, iconSize = 128, fontSize = 16;
	private OSXCodeSign<Dmg,SetupBuilder> codeSign;

	/**
     * Create the task.
     */
    public Dmg() {
        super( "dmg" );
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
     * Return width of Finder view
     * @return width of Finder view
     */
    public Integer getWindowWidth() {
		return windowWidth;
	}

    /**
     * Set width of Finder view
     * @param windowWidth width of Finder view 
     */
	public void setWindowWidth(Integer windowWidth) {
		this.windowWidth = windowWidth;
	}

    /**
     * Return height of Finder view
     * @return height of Finder view
     */
	public Integer getWindowHeight() {
		return windowHeight;
	}

    /**
     * Set height of Finder view
     * @param windowHeight of Finder view
     */
	public void setWindowHeight(Integer windowHeight) {
		this.windowHeight = windowHeight;
	}

    /**
     * Return size of icons in Finder view
     * @return size of icons in Finder view
     */
	public Integer getIconSize() {
		return iconSize;
	}

    /**
     * Set size of icons in Finder view
     * @param iconSize of icons in Finder view
     */
	public void setIconSize(Integer iconSize) {
		this.iconSize = iconSize;
	}

    /**
     * Return background Image for Finder View
     * @return background Image for Finder View
     */
	public File getBackgroundImage() {
		return backgroundImage;
	}

    /**
     * Set background Image for Finder View
     * @param backgroundFile Image for Finder View
     */
	public void setBackgroundImage(File backgroundFile) {
		this.backgroundImage = backgroundFile;
	}

    /**
     * Return font size for Finder View
     * @return font size for Finder View
     */
	public Integer getFontSize() {
		return fontSize;
	}

	/**
     * Set font size for Finder View
     * @param fontSize size for Finder View
     */
	public void setFontSize(Integer fontSize) {
		this.fontSize = fontSize;
	}

    
    /**
     * Set the needed information for signing the setup.
     * 
     * @param closue the data for signing
     */
    public void codeSign( Closure<OSXCodeSign<Dmg,SetupBuilder>> closue ) {
        ProjectInternal project = (ProjectInternal)getProject();
        codeSign = ConfigureUtil.configure( closue, new OSXCodeSign<Dmg,SetupBuilder>(this, project.getFileResolver()) );
    }

    /**
     * Get the SignTool configuration if set
     * 
     * @return the settings or null
     */
    public OSXCodeSign<Dmg,SetupBuilder> getCodeSign() {
        return codeSign;
    }
}
