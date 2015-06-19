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

import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.UnionFileTree;

import com.inet.gradle.setup.SetupBuilder;

/**
 * Build a DMG image for OSX.
 * 
 * @author Volker Berlin
 */
public class DmgBuilder {

    private final Dmg          dmg;

    private final SetupBuilder setup;

    private FileResolver       fileResolver;

    private File               buildDir;

    /**
     * Create a new instance
     * 
     * @param dmg the calling task
     * @param setup the shared settings
     * @param fileResolver the file Resolver
     */
    public DmgBuilder( Dmg dmg, SetupBuilder setup, FileResolver fileResolver ) {
        this.dmg = dmg;
        this.setup = setup;
        this.fileResolver = fileResolver;
    }

    public void build() {
        buildDir = new File( dmg.getProject().getBuildDir(), "setup/dmg" );

        copyFiles();
    }

    /**
     * Copy the files to the build directory.
     */
    private void copyFiles() {
        FileTree fileTree = new UnionFileTree( setup.getSource(), dmg.getSource() );
        fileTree.visit( new FileVisitor() {
            @Override
            public void visitFile( FileVisitDetails details ) {
                details.copyTo( details.getRelativePath().getFile( buildDir ) );
            }

            @Override
            public void visitDir( FileVisitDetails arg0 ) {
                //ignore
            }
        } );
    }
}
