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

import com.inet.gradle.setup.AbstractSetupTask;

/**
 * The dmg Gradle task. It build a dmg package for Mac.
 * 
 * @author Volker Berlin
 */
public class Dmg extends AbstractSetupTask {

    private File backgroundImage;
    private Integer windowWidth = 400, windowHeight = 300, iconSize = 128;

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
     * {@inheritDoc}
     */
    @Override
    protected void copyTo( File target ) {
        super.copyTo( target );
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
     * @param height of Finder view
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
     * @param size of icons in Finder view
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
     * @param background Image for Finder View
     */
	public void setBackgroundImage(File backgroundFile) {
		this.backgroundImage = backgroundFile;
	}
}
