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

import org.gradle.api.internal.project.ProjectInternal;

import com.inet.gradle.setup.AbstractSetupTask;

/**
 * The msi Gradle task. It build a msi setup for Windows.
 * 
 * @author Volker Berlin
 */
public class Msi extends AbstractSetupTask {

    private String arch = "x64";

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

    public String getArch() {
        return arch;
    }

    /**
     * Set the architecture of the setup. The default is x64. Possible values are:
     * <li>x86
     * <li>x64
     * <li>ia64
     * @param arch the architecture
     */
    public void setArch( String arch ) {
        this.arch = arch;
    }

    /**
     * if the setup is a 64 bit setup.
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
}
