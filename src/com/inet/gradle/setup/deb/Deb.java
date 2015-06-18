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
package com.inet.gradle.setup.deb;

import org.gradle.api.internal.project.ProjectInternal;

import com.inet.gradle.setup.AbstractSetupTask;

/**
 * The deb Gradle task. It build a deb package for Debian / Ubuntu.
 * 
 * @author Volker Berlin
 */
public class Deb extends AbstractSetupTask {

    public Deb() {
        super( "deb" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void build() {
        ProjectInternal project = (ProjectInternal)getProject();
        new DebBuilder( this, getSetupBuilder(), project.getFileResolver() ).build();
    }
}
