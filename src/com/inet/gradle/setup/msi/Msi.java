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

    private String   arch = "x64";

    private Object   bannerBmp, dialogBmp;

    private SignTool signTool;

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

    public String getArch() {
        return arch;
    }

    /**
     * Set the architecture of the setup. The default is x64. Possible values are: 
     * <ul><li>x86 <li>x64 <li>ia64</ul>
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
     * @param closue
     */
    public void signTool( Closure closue ) {
        signTool = ConfigureUtil.configure( closue, new SignTool( this ) );
    }

    /**
     * Get the SignTool configuration if set
     * 
     * @return the settings or null
     */
    public SignTool getSignTool() {
        return signTool;
    }
}
