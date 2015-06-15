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
package com.inet.gradle.setup;

import groovy.lang.Closure;

import java.io.FilterReader;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.gradle.api.Action;
import org.gradle.api.file.CopyProcessingSpec;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.internal.file.copy.CopySpecInternal;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.InputFiles;

/**
 * Implementation of the CopySpec interface.
 * 
 * @author Volker Berlin
 */
public interface SetupSources extends CopySpec {

    CopySpecInternal getCopySpec();

    /**
     * Returns the source files for this task.
     * 
     * @return The source files. Never returns null.
     */
    @InputFiles
    default FileTree getSource() {
        return getCopySpec().buildRootResolver().getAllSource();
    }

    default CopySpec eachFile( Action<? super FileCopyDetails> arg0 ) {
        return getCopySpec().eachFile( arg0 );
    }

    default CopySpec eachFile( Closure arg0 ) {
        return getCopySpec().eachFile( arg0 );
    }

    default CopySpec exclude( Closure arg0 ) {
        return getCopySpec().exclude( arg0 );
    }

    default CopySpec exclude( Iterable<String> arg0 ) {
        return getCopySpec().exclude( arg0 );
    }

    default CopySpec exclude( Spec<FileTreeElement> arg0 ) {
        return getCopySpec().exclude( arg0 );
    }

    default CopySpec exclude( String... arg0 ) {
        return getCopySpec().exclude( arg0 );
    }

    default CopySpec expand( Map<String, ?> arg0 ) {
        return getCopySpec().expand( arg0 );
    }

    default CopySpec filesMatching( String arg0, Action<? super FileCopyDetails> arg1 ) {
        return getCopySpec().filesMatching( arg0, arg1 );
    }

    default CopySpec filesNotMatching( String arg0, Action<? super FileCopyDetails> arg1 ) {
        return getCopySpec().filesNotMatching( arg0, arg1 );
    }

    default CopySpec filter( Class<? extends FilterReader> arg0 ) {
        return getCopySpec().filter( arg0 );
    }

    default CopySpec filter( Closure arg0 ) {
        return getCopySpec().filter( arg0 );
    }

    default CopySpec filter( Map<String, ?> arg0, Class<? extends FilterReader> arg1 ) {
        return getCopySpec().filter( arg0, arg1 );
    }

    default CopySpec from( Object arg0, Closure arg1 ) {
        return getCopySpec().from( arg0, arg1 );
    }

    default CopySpec from( Object... arg0 ) {
        return getCopySpec().from( arg0 );
    }

    default Integer getDirMode() {
        return getCopySpec().getDirMode();
    }

    default DuplicatesStrategy getDuplicatesStrategy() {
        return getCopySpec().getDuplicatesStrategy();
    }

    default Set<String> getExcludes() {
        return getCopySpec().getExcludes();
    }

    default Integer getFileMode() {
        return getCopySpec().getFileMode();
    }

    default boolean getIncludeEmptyDirs() {
        return getCopySpec().getIncludeEmptyDirs();
    }

    default Set<String> getIncludes() {
        return getCopySpec().getIncludes();
    }

    default CopySpec include( Closure arg0 ) {
        return getCopySpec().include( arg0 );
    }

    default CopySpec include( Iterable<String> arg0 ) {
        return getCopySpec().include( arg0 );
    }

    default CopySpec include( Spec<FileTreeElement> arg0 ) {
        return getCopySpec().include( arg0 );
    }

    default CopySpec include( String... arg0 ) {
        return getCopySpec().include( arg0 );
    }

    default CopySpec into( Object arg0, Closure arg1 ) {
        return getCopySpec().into( arg0, arg1 );
    }

    default CopySpec into( Object arg0 ) {
        return getCopySpec().into( arg0 );
    }

    default boolean isCaseSensitive() {
        return getCopySpec().isCaseSensitive();
    }

    default CopySpec rename( Closure arg0 ) {
        return getCopySpec().rename( arg0 );
    }

    default CopyProcessingSpec rename( Pattern arg0, String arg1 ) {
        return getCopySpec().rename( arg0, arg1 );
    }

    default CopySpec rename( String arg0, String arg1 ) {
        return getCopySpec().rename( arg0, arg1 );
    }

    default void setCaseSensitive( boolean arg0 ) {
        getCopySpec().setCaseSensitive( arg0 );
    }

    default CopyProcessingSpec setDirMode( Integer arg0 ) {
        return getCopySpec().setDirMode( arg0 );
    }

    default void setDuplicatesStrategy( DuplicatesStrategy arg0 ) {
        getCopySpec().setDuplicatesStrategy( arg0 );
    }

    default CopySpec setExcludes( Iterable<String> arg0 ) {
        return getCopySpec().setExcludes( arg0 );
    }

    default CopyProcessingSpec setFileMode( Integer arg0 ) {
        return getCopySpec().setFileMode( arg0 );
    }

    default void setIncludeEmptyDirs( boolean arg0 ) {
        getCopySpec().setIncludeEmptyDirs( arg0 );
    }

    default CopySpec setIncludes( Iterable<String> arg0 ) {
        return getCopySpec().setIncludes( arg0 );
    }

    default CopySpec with( CopySpec... arg0 ) {
        return getCopySpec().with( arg0 );
    }
}
