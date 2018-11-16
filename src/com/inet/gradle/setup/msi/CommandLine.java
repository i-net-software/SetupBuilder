/*
 * Copyright 2015 - 2017 i-net software
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

import com.inet.gradle.setup.abstracts.Application;

/**
 * Get the parts of a windows command line.
 */
class CommandLine {

    final String relativTarget; // executable inside the installation
    final String relativFull;   // executable inside the installation
    final String target;
    final String arguments;
    final String full;
    final String workDir;

    /**
     * Generate the parts of the command line.
     * @param starter a command description
     * @param javaDir path to java if embedded
     */
    CommandLine( Application starter, String javaDir ) {
        String target = starter.getExecutable();
        String arguments = starter.getStartArguments();
        String vmArguments = String.join( " ", starter.getJavaVMArguments() ).trim();
        String dir;
        String workDir = starter.getWorkDir();
        if( workDir == null ) {
            workDir = "";
        } else {
            workDir = workDir.replace( '/', '\\' );
            if( !workDir.isEmpty() && !workDir.endsWith( "\\" ) ){
                workDir += '\\';
            }
        }
        if( target == null || target.isEmpty() ) {
            if( javaDir != null ) {
                target = "[INSTALLDIR]" + javaDir + "\\bin\\javaw.exe";
            } else {
                target = "javaw.exe";
            }
            dir = "";
            arguments = vmArguments + " -cp \"[INSTALLDIR]" + workDir + starter.getMainJar() + "\" " + starter.getMainClass() + " " + arguments;
        } else {
            if( !target.startsWith( "[" ) ) {
                dir = "[INSTALLDIR]";
                target = workDir + target;
            } else {
                dir = "";
            }
        }
        this.relativTarget = target;
        this.target = dir + target;
        this.arguments = arguments;
        if( this.target.indexOf( ' ' ) >=  0 || this.target.indexOf( '[' ) >=  0 ) {
            this.full = '\"' + this.target + "\" " + arguments;
        } else {
            this.full = this.target + ' ' + arguments;
        }
        if( this.relativTarget.indexOf( ' ' ) >=  0 || this.relativTarget.indexOf( '[' ) >=  0 ) {
            this.relativFull = '\"' + this.relativTarget + "\" " + arguments;
        } else {
            this.relativFull = this.relativTarget + ' ' + arguments;
        }
        this.workDir = workDir;
    }
}
