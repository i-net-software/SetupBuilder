/*
 * Copyright 2015 i-net software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.inet.gradle.setup.unix.deb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.zip.GZIPOutputStream;

import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.abstracts.LocalizedResource;

/**
 * Builder for the documentation files like changelog that are required for the Debian package tool.
 *
 *
 * @author Stefan Heidrich
 */
class DebDocumentFileBuilder {

    private static final String NEWLINE = "\n";

    private final Deb           deb;

    private final SetupBuilder  setup;

    private File                buildDir;

    /**
     * the constructor setting the fields
     *
     * @param deb the task for the debian package
     * @param setup the generic task for all setups
     * @param buildDir the directory to build the package in
     */
    DebDocumentFileBuilder( Deb deb, SetupBuilder setup, File buildDir ) {
        this.deb = deb;
        this.setup = setup;
        this.buildDir = buildDir;
    }

    /**
     * Create the configuration files for the Debian package based on the settings in the task.
     *
     * @throws Exception
     */
    void build() throws Exception {

        createChangelogFile();
        copyCopyrightFile();

    }

    /**
     * Writes the copyrights file to the specified directory. This should be /user/share/doc/package
     *
     * @throws FileNotFoundException if the copyright file could not be created
     * @throws IOException if the are problems writing the copyright file
     */
    void copyCopyrightFile() throws FileNotFoundException, IOException {
        File copyright = new File( buildDir, "copyright" );

        if( !copyright.getParentFile().exists() ) {
            copyright.getParentFile().mkdirs();
        }
        FileOutputStream fileoutput = null;
        OutputStreamWriter controlWriter = null;
        fileoutput = new FileOutputStream( copyright );

        controlWriter = new OutputStreamWriter( fileoutput, "UTF-8" );

        controlWriter.write( "Format: http://www.debian.org/doc/packaging-manuals/copyright-format/1.0/" + NEWLINE );
        controlWriter.write( "Upstream-Name: " + setup.getArchiveName() + NEWLINE );
        controlWriter.write( "Source: " + deb.getHomepage() + NEWLINE );

        for( LocalizedResource localizedResource : setup.getLicenseFiles() ) {

            StringBuffer content = new StringBuffer();
            content.append( NEWLINE );
            content.append( "File: *" + NEWLINE );
            content.append( "Copyright: " + setup.getCopyright() + NEWLINE );

            String lang = localizedResource.getLanguage().equalsIgnoreCase( setup.getDefaultResourceLanguage() ) ? "" : " and " + localizedResource.getLanguage();
            content.append( "License: commercial" + lang + NEWLINE );

            try (Scanner scanner = new Scanner( localizedResource.getResource(), "UTF8" )) {
                while ( scanner.hasNextLine() ) {
                    String line = scanner.nextLine();
                    content.append( " " + ( line.isEmpty() ? "." : line) + NEWLINE );
                }
            } finally {
                controlWriter.write( content + NEWLINE );
            }
        }

        controlWriter.flush();
        controlWriter.close();

        DebBuilder.setPermissions( copyright, false );
    }

    /**
     * Writes the changelog.gz file to the specified directory. This should be /user/share/doc/package
     *
     * @throws IOException if there are problems writing the changelog file
     */
    private void createChangelogFile() throws IOException {
        FileOutputStream fileoutput = null;
        OutputStreamWriter controlWriter = null;

        try {
            File changelog = new File( buildDir, "changelog.gz" );

            if( !changelog.getParentFile().exists() ) {
                changelog.getParentFile().mkdirs();
            }

            fileoutput = new FileOutputStream( changelog );
            GZIPOutputStream gzipstream = new GZIPOutputStream( fileoutput );

            controlWriter = new OutputStreamWriter( gzipstream, "UTF-8" );

            controlWriter.write( setup.getAppIdentifier() + " (" + deb.getVersion() + ") unstable; urgency=low" + NEWLINE + NEWLINE );

            String changes = deb.getChanges();
            if( changes != null && changes.length() > 0 ) {
                controlWriter.write( changes + NEWLINE );
            } else {
                controlWriter.write( "  * no changes" + NEWLINE );
            }

            controlWriter.write( NEWLINE + " -- " + setup.getVendor() + " <" + deb.getMaintainerEmail() + ">  " + new SimpleDateFormat( "dd MMM yyyy HH:mm:ss Z" ).format( new Date( System.currentTimeMillis() ) ) + NEWLINE );

            controlWriter.flush();

            DebBuilder.setPermissions( changelog, false );

        } finally {
            if( controlWriter != null ) {
                try {
                    controlWriter.close();
                } catch( IOException e ) {
                    // IGNORE
                }
            }
            if( fileoutput != null ) {
                try {
                    fileoutput.close();
                } catch( IOException e ) {
                    // IGNORE
                }
            }
        }
    }

}
