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
package com.inet.gradle.setup.abstracts;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.gradle.api.internal.file.FileResolver;
import org.gradle.initialization.DefaultBuildCancellationToken;
import org.gradle.internal.concurrent.DefaultExecutorFactory;
import org.gradle.process.internal.DefaultExecAction;

import com.inet.gradle.setup.util.IndentationOutputStream;

/**
 * Some basic builder functionally.
 *
 * @author Volker Berlin
 *
 * @param <T> the task
 * @param <S> the builder type
 */
public abstract class AbstractBuilder<T extends AbstractTask, S extends AbstractSetupBuilder> implements Serializable {

    protected transient T task;

    protected transient FileResolver fileResolver;

    protected transient File buildDir;

    /**
     * Builder abstraction
     * @param task concrete task
     * @param fileResolver resolver for files
     */
    protected AbstractBuilder( T task, FileResolver fileResolver ) {
        this.task = task;
        this.fileResolver = fileResolver;
        this.buildDir = task.getTemporaryDir();
    }

    /**
     * Call a program from the WIX installation.
     *
     * @param parameters the parameters
     */
    protected void exec( ArrayList<String> parameters ) {
        exec( parameters, null, null );
    }

    /**
     * Execute an external process.
     *
     * @param parameters command line
     * @param input optional InputStream for the process
     * @param output optional OutputStream for the process
     */
    protected void exec( ArrayList<String> parameters, InputStream input, OutputStream output ) {
        exec( parameters, input, output, false);
    }

    /**
     * Execute an external process.
     * Returns the response and ignores the exit value.
     *
     * @param parameters command line
     * @return the output
     */
    protected String exec( String... parameters ) {
        return exec(true, parameters);
    }

    /**
     * Execute an external process.
     * Returns the response.
     *
     * @param ignoreExitValue if the exit value should be ignored
     * @param parameters command line
     * @return the output
     */
    protected String exec( boolean ignoreExitValue, String... parameters ) {
        ArrayList<String> command = new ArrayList<>();
        command.addAll( Arrays.asList( parameters ) );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exec( command, null, baos, ignoreExitValue );
        return baos.toString().trim();
    }

    /**
     * Execute an external process.
     * Returns the response.
     *
     * @param ignoreExitValue if the exit value should be ignored
     * @param parameters command line
     * @param error the error output stream
     * @return the output
     */
    protected String exec( boolean ignoreExitValue, OutputStream error, String... parameters ) {
        ArrayList<String> command = new ArrayList<>();
        command.addAll( Arrays.asList( parameters ) );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exec( command, null, baos, error, ignoreExitValue );
        return baos.toString().trim();
    }

    /**
     * Execute an external process.
     *
     * @param parameters command line
     * @param input optional InputStream for the process
     * @param output optional OutputStream for the process
     * @param ignoreExitValue true, does not throw an exception if the return code is not equals zero.
     */
    @SuppressWarnings( "resource" )
    protected void exec( ArrayList<String> parameters, InputStream input, OutputStream output, boolean ignoreExitValue ) {
        exec( parameters, input, output, output, ignoreExitValue );
    }

    /**
     * Execute an external process.
     *
     * @param parameters command line
     * @param input optional InputStream for the process
     * @param output optional OutputStream for the process
     * @param error optional error OutputStream for the process
     * @param ignoreExitValue true, does not throw an exception if the return code is not equals zero.
     */
    @SuppressWarnings( "resource" )
    protected void exec( ArrayList<String> parameters, InputStream input, OutputStream output, OutputStream error, boolean ignoreExitValue ) {
        // print command line to the log
        StringBuilder log = new StringBuilder( "\tCommand: " );
        for( String para : parameters ) {

            if ( para == null ) {
                task.getProject().getLogger().lifecycle( "Parameter not set. This will fail now:" + log.toString() );
            } else {
                log.append( '\"' ).append( para );
                if( para.endsWith( "\\" ) ) {
                    log.append( '\\' );
                }
                log.append( "\" " );
            }
        }
        task.getProject().getLogger().lifecycle( log.toString() );

        /*// if gradleVersion < 4.5
        DefaultExecAction action = new DefaultExecAction( fileResolver );
        //// elif gradleVersion < 4.8
        DefaultExecAction action = new DefaultExecAction( fileResolver, new DefaultExecutorFactory().create( "exec setup" ) );
        */// else
        DefaultExecAction action = new DefaultExecAction( fileResolver, new DefaultExecutorFactory().create( "exec setup" ), new DefaultBuildCancellationToken());
        //// endif
        action.setCommandLine( parameters );
        action.setIgnoreExitValue( ignoreExitValue );
        action.setWorkingDir( buildDir );

        if( input != null ) {
            action.setStandardInput( input );
        }

        if( output == null ) {
            output = new IndentationOutputStream( System.out );
        }

        if( error == null ) {
            error = new IndentationOutputStream( System.err );
        }

        action.setStandardOutput( output );
        action.setErrorOutput( error );
        try {
            action.execute();
        } catch( Throwable th ) {
            throw new RuntimeException( th );
        } finally {
            try {
                output.flush();
            } catch( IOException e ) {
                task.getProject().getLogger().error( e.getLocalizedMessage() );
            }
        }
    }

    /**
     * Returns the concrete task
     * @return the task
     */
    protected T getTask() {
        return task;
    }
}
