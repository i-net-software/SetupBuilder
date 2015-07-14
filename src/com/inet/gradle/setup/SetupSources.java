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
import org.gradle.api.internal.file.copy.CopySpecSource;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.InputFiles;

/**
 * Implementation of the CopySpec interface.
 * 
 * @author Volker Berlin
 */
public interface SetupSources extends CopySpec, CopySpecSource {

    /**
     * Returns the source files for this task.
     * 
     * @return The source files. Never returns null.
     */
    @InputFiles
    default FileTree getSource() {
        return getRootSpec().buildRootResolver().getAllSource();
    }

    @Override
    default CopySpec eachFile( Action<? super FileCopyDetails> arg0 ) {
        return getRootSpec().eachFile( arg0 );
    }

    @Override
    default CopySpec eachFile( Closure arg0 ) {
        return getRootSpec().eachFile( arg0 );
    }

    @Override
    default CopySpec exclude( Closure arg0 ) {
        return getRootSpec().exclude( arg0 );
    }

    @Override
    default CopySpec exclude( Iterable<String> arg0 ) {
        return getRootSpec().exclude( arg0 );
    }

    @Override
    default CopySpec exclude( Spec<FileTreeElement> arg0 ) {
        return getRootSpec().exclude( arg0 );
    }

    @Override
    default CopySpec exclude( String... arg0 ) {
        return getRootSpec().exclude( arg0 );
    }

    @Override
    default CopySpec expand( Map<String, ?> arg0 ) {
        return getRootSpec().expand( arg0 );
    }

    @Override
    default CopySpec filesMatching( String arg0, Action<? super FileCopyDetails> arg1 ) {
        return getRootSpec().filesMatching( arg0, arg1 );
    }

    @Override
    default CopySpec filesNotMatching( String arg0, Action<? super FileCopyDetails> arg1 ) {
        return getRootSpec().filesNotMatching( arg0, arg1 );
    }

    @Override
    default CopySpec filter( Class<? extends FilterReader> arg0 ) {
        return getRootSpec().filter( arg0 );
    }

    @Override
    default CopySpec filter( Closure arg0 ) {
        return getRootSpec().filter( arg0 );
    }

    @Override
    default CopySpec filter( Map<String, ?> arg0, Class<? extends FilterReader> arg1 ) {
        return getRootSpec().filter( arg0, arg1 );
    }

    @Override
    default CopySpec from( Object arg0, Closure arg1 ) {
        return getRootSpec().from( arg0, arg1 );
    }

    @Override
    default CopySpec from( Object... arg0 ) {
        return getRootSpec().from( arg0 );
    }

    @Override
    default Integer getDirMode() {
        return getRootSpec().getDirMode();
    }

    @Override
    default DuplicatesStrategy getDuplicatesStrategy() {
        return getRootSpec().getDuplicatesStrategy();
    }

    @Override
    default Set<String> getExcludes() {
        return getRootSpec().getExcludes();
    }

    @Override
    default Integer getFileMode() {
        return getRootSpec().getFileMode();
    }

    @Override
    default boolean getIncludeEmptyDirs() {
        return getRootSpec().getIncludeEmptyDirs();
    }

    @Override
    default Set<String> getIncludes() {
        return getRootSpec().getIncludes();
    }

    @Override
    default CopySpec include( Closure arg0 ) {
        return getRootSpec().include( arg0 );
    }

    @Override
    default CopySpec include( Iterable<String> arg0 ) {
        return getRootSpec().include( arg0 );
    }

    @Override
    default CopySpec include( Spec<FileTreeElement> arg0 ) {
        return getRootSpec().include( arg0 );
    }

    @Override
    default CopySpec include( String... arg0 ) {
        return getRootSpec().include( arg0 );
    }

    @Override
    default CopySpec into( Object arg0, Closure arg1 ) {
        return getRootSpec().into( arg0, arg1 );
    }

    @Override
    default CopySpec into( Object arg0 ) {
        return getRootSpec().into( arg0 );
    }

    @Override
    default boolean isCaseSensitive() {
        return getRootSpec().isCaseSensitive();
    }

    @Override
    default CopySpec rename( Closure arg0 ) {
        return getRootSpec().rename( arg0 );
    }

    @Override
    default CopyProcessingSpec rename( Pattern arg0, String arg1 ) {
        return getRootSpec().rename( arg0, arg1 );
    }

    @Override
    default CopySpec rename( String arg0, String arg1 ) {
        return getRootSpec().rename( arg0, arg1 );
    }

    @Override
    default void setCaseSensitive( boolean arg0 ) {
        getRootSpec().setCaseSensitive( arg0 );
    }

    @Override
    default CopyProcessingSpec setDirMode( Integer arg0 ) {
        return getRootSpec().setDirMode( arg0 );
    }

    @Override
    default void setDuplicatesStrategy( DuplicatesStrategy arg0 ) {
        getRootSpec().setDuplicatesStrategy( arg0 );
    }

    @Override
    default CopySpec setExcludes( Iterable<String> arg0 ) {
        return getRootSpec().setExcludes( arg0 );
    }

    @Override
    default CopyProcessingSpec setFileMode( Integer arg0 ) {
        return getRootSpec().setFileMode( arg0 );
    }

    @Override
    default void setIncludeEmptyDirs( boolean arg0 ) {
        getRootSpec().setIncludeEmptyDirs( arg0 );
    }

    @Override
    default CopySpec setIncludes( Iterable<String> arg0 ) {
        return getRootSpec().setIncludes( arg0 );
    }

    @Override
    default CopySpec with( CopySpec... arg0 ) {
        return getRootSpec().with( arg0 );
    }
}
