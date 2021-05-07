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
package com.inet.gradle.setup.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A OutputStream that add an indentation on efery line for formatting.
 * 
 * @author Volker Berlin
 */
public class IndentationOutputStream extends FilterOutputStream {
    private boolean needIndentation = true;

    /**
     * Create a instance.
     * @param out underling stream.
     */
    public IndentationOutputStream( OutputStream out ) {
        super( out );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write( int b ) throws IOException {
        if( needIndentation ) {
            super.write( '\t' );
            super.write( '\t' );
        }
        super.write( b );
        needIndentation = b == '\n';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        if( !needIndentation ) {
            super.write( '\n' );
            needIndentation = true;
        }
        super.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        // Ignore to not close System.out
    }
}
