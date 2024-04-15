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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.Template;
import com.inet.gradle.setup.abstracts.LocalizedResource;

/**
 * Builder for the control, postinst and prerm files, that are required for the Debian package tool.
 * <dl>
 * <dt>control</dt>
 * <dd>contains settings for the package like dependencies, architecture, description</dd>
 * <dt>postinst</dt>
 * <dd>contains scripts and commands that are executed after the files are copied</dd>
 * <dt>prerm</dt>
 * <dd>contains scripts and commands that are executed before the files are removed</dd>
 * </dl>
 *
 * @author Stefan Heidrich
 */
class DebControlFileBuilder {

    private static final char  NEWLINE   = '\n';

    private final Deb          deb;

    private final SetupBuilder setup;

    private File               buildDir;

    private Collection<String> confFiles = new ArrayList<>();

    enum Script {
        PREINST, POSTINST, PRERM, POSTRM
    }

    Map<Script, StringBuilder> scriptHeadMap = new HashMap<>();

    Map<Script, StringBuilder> scriptTailMap = new HashMap<>();

    /**
     * the constructor setting the fields
     *
     * @param deb the task for the debian package
     * @param setup the generic task for all setups
     * @param buildDir the directory to build the package in
     */
    DebControlFileBuilder( Deb deb, SetupBuilder setup, File buildDir ) {
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

        createControlFile();
        createConfFilesFile();
        createScripts();
    }

    /**
     * Creates the 'control' file for the Debian package
     *
     * @throws IOException if something could not be written to the file
     */
    private void createControlFile() throws IOException {
        if( !buildDir.exists() ) {
            buildDir.mkdirs();
        } else if( !buildDir.isDirectory() ) {
            throw new IllegalArgumentException( "The buildDir parameter must be a directory!" );
        }

        FileOutputStream fileoutput = null;
        OutputStreamWriter controlWriter = null;

        try {
            File control = new File( buildDir, "control" );
            fileoutput = new FileOutputStream( control );
            controlWriter = new OutputStreamWriter( fileoutput, "UTF-8" );

            putPackage( controlWriter );
            putVersion( controlWriter );
            putSection( controlWriter );
            putPriority( controlWriter );
            putArchitecture( controlWriter );
            putInstallSize( controlWriter );
            putRecommends( controlWriter );
            putPreDepends( controlWriter );
            putDepends( controlWriter );
            putMaintainer( controlWriter );
            putDescription( controlWriter );
            putHomepage( controlWriter );

            controlWriter.flush();

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

    /**
     * Write the description to the file. The description is mandatory. If no description is declared a runtime exception will be thrown.
     *
     * @param controlWriter the writer for the file
     * @throws IOException if the was an error while writing to the file
     */
    private void putDescription( OutputStreamWriter controlWriter ) throws IOException {

        List<LocalizedResource> longDescriptions = setup.getLongDescriptions();
        if ( longDescriptions.size() > 0 ) {
            for( LocalizedResource localizedResource : setup.getLongDescriptions() ) {
                String lang = localizedResource.getLanguage().equalsIgnoreCase( setup.getDefaultResourceLanguage() ) ? "" : "-" + localizedResource.getLanguage();
                StringBuffer content = new StringBuffer( "Description" + lang + ": " + deb.getDescription() + NEWLINE );
                try (Scanner scanner = new Scanner( localizedResource.getResource(), "UTF8" )) {
                    while ( scanner.hasNextLine() ) {
                        String line = scanner.nextLine();
                        content.append( " " + ( line.isEmpty() ? "." : line) + NEWLINE );
                    }
                } finally {
                    controlWriter.write( content.toString() );
                }
            }
        } else {
            // Write single line of short description.
            controlWriter.write( "Description: " + deb.getDescription() + NEWLINE );
        }
    }

    /**
     * Write the maintainer to the file. The maintainer is mandatory. If no maintainer is declared a runtime exception will be thrown.
     *
     * @param controlWriter the writer for the file
     * @throws IOException if the was an error while writing to the file
     */
    private void putMaintainer( OutputStreamWriter controlWriter ) throws IOException {
        String vendor = setup.getVendor();
        if( vendor == null || vendor.length() == 0 ) {
            throw new RuntimeException( "No vendor declared in the setup configuration." );
        } else {
            controlWriter.write( "Maintainer: " + vendor + " <" + deb.getMaintainerEmail() + ">" + NEWLINE );
        }
    }

    /**
     * Write the dependencies to the file. If no dependencies are specified, the java dependencies will be used.
     *
     * @param controlWriter the writer for the file
     * @throws IOException if the was an error while writing to the file
     */
    private void putDepends( OutputStreamWriter controlWriter ) throws IOException {
        String depends = deb.getDepends();
        if( depends != null && depends.length() > 0 ) {
            controlWriter.write( "Depends: " + depends + NEWLINE );
        }
    }

    /**
     * Writes the pre-dependencies to the file.
     *
     * @param controlWriter the writer for the file
     * @throws IOException if the was an error while writing to the file
     */
    private void putPreDepends( OutputStreamWriter controlWriter ) throws IOException {
        String depends = deb.getDepends();
        if( depends == null || depends.length() == 0 ) {
            depends = "debconf";
        }
        controlWriter.write( "Pre-Depends: " + depends + NEWLINE );
    }

    /**
     * Write the recommends to the file.
     *
     * @param controlWriter the writer for the file
     * @throws IOException if the was an error while writing to the file
     */
    private void putRecommends( OutputStreamWriter controlWriter ) throws IOException {
        String recommends = deb.getRecommends();
        if( ( recommends == null || recommends.length() == 0 ) && setup.getBundleJre() != null ) {
            recommends = "openjdk-8-jre | openjdk-8-jdk | default-jre | default-jdk, libgtk2-perl";
        }
        if( recommends != null && recommends.length() > 0 ) {
            controlWriter.write( "Recommends: " + recommends + NEWLINE );
        }
    }

    /**
     * Write the installation size to the file. If no size is specified it will count the size of all files it has to install and round it to full megabytes.
     *
     * @param controlWriter the writer for the file
     * @throws IOException if the was an error while writing to the file
     */
    private void putInstallSize( OutputStreamWriter controlWriter ) throws IOException {

        String installSize = deb.getInstallSize();
        if( installSize == null || installSize.length() == 0 ) {
            long fileSize = 0;
            for( File file : deb.getSetupSource().getFiles() ) {
                if( file.isFile() ) {
                    fileSize = fileSize + file.length();
                }
            }
            for( File file : deb.getSource().getFiles() ) {
                if( file.isFile() ) {
                    fileSize = fileSize + file.length();
                }
            }

            installSize = String.valueOf( fileSize / 1024 ); // Size wird in KB angegeben und nicht in Bytes

        }
        controlWriter.write( "Installed-Size: " + installSize + NEWLINE );
    }

    /**
     * Write the architecture to the file. If no architecture is specified then 'all' will be used.
     *
     * @param controlWriter the writer for the file
     * @throws IOException if the was an error while writing to the file
     */
    private void putArchitecture( OutputStreamWriter controlWriter ) throws IOException {
        controlWriter.write( "Architecture: " + deb.getArchitecture() + NEWLINE );
    }

    /**
     * Write the priority to the file. If no priority are specified then 'optional' will be used.
     *
     * @param controlWriter the writer for the file
     * @throws IOException if the was an error while writing to the file
     */
    private void putPriority( OutputStreamWriter controlWriter ) throws IOException {
        String priority = deb.getPriority();
        if( priority == null || priority.length() == 0 ) {
            priority = "optional";
        }
        controlWriter.write( "Priority: " + priority + NEWLINE );
    }

    /**
     * Write the section to the file. If no section is specified then 'java' will be used.
     *
     * @param controlWriter the writer for the file
     * @throws IOException if the was an error while writing to the file
     */
    private void putSection( OutputStreamWriter controlWriter ) throws IOException {
        String section = deb.getSection();
        if( section == null || section.length() == 0 ) {
            section = "java";
        }
        controlWriter.write( "Section: " + section + NEWLINE );
    }

    /**
     * Write the version to the file. The version is mandatory. If no version is declared a runtime exception will be thrown.
     *
     * @param controlWriter the writer for the file
     * @throws IOException if the was an error while writing to the file
     */
    private void putVersion( OutputStreamWriter controlWriter ) throws IOException {
        String version = deb.getVersion();
        if( version == null || version.length() == 0 ) {
            throw new RuntimeException( "No version declared in the setup configuration." );
        } else {
            controlWriter.write( "Version: " + version + NEWLINE );
        }
    }

    /**
     * Write the package to the file. The package is mandatory. If no package is declared a runtime exception will be thrown.
     *
     * @param controlWriter the writer for the file
     * @throws IOException if the was an error while writing to the file
     */
    private void putPackage( OutputStreamWriter controlWriter ) throws IOException {
        String packages = setup.getAppIdentifier();
        if( packages == null || packages.length() == 0 ) {
            throw new RuntimeException( "No package declared in the setup configuration." );
        } else {
            controlWriter.write( "Package: " + packages + NEWLINE );
        }
    }

    /**
     * Write the homepage to the file.
     *
     * @param controlWriter the writer for the file
     * @throws IOException if the was an error while writing to the file
     */
    private void putHomepage( OutputStreamWriter controlWriter ) throws IOException {
        String homepage = deb.getHomepage();
        if( homepage != null && homepage.length() > 0 ) {
            controlWriter.write( "Homepage: " + homepage + NEWLINE );
        }
    }

    /**
     * Adds a config file
     *
     * @param file the config file
     */
    public void addConfFile( String file ) {
        confFiles.add( file );
    }

    /**
     * Creates the <tt>conffiles</tt> file with a listing of all created configuration files.
     *
     * @throws IOException on I/O failures
     */
    private void createConfFilesFile() throws IOException {
        if( confFiles.size() > 0 ) {
            File cfile = new File( buildDir, "conffiles" );
            try (FileWriter writer = new FileWriter( cfile )) {
                for( String confFile : confFiles ) {
                    writer.write( '/' );
                    writer.write( confFile );
                    writer.write( '\n' );
                }
            }
            Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
            perms.add( PosixFilePermission.OWNER_READ );
            perms.add( PosixFilePermission.OWNER_WRITE );
            perms.add( PosixFilePermission.GROUP_READ );
            perms.add( PosixFilePermission.OTHERS_READ );
            Files.setPosixFilePermissions( cfile.toPath(), perms );
        }
    }

    /**
     * Adds a fragment to the specified install script at the tail section.
     *
     * @param script the install script
     * @param scriptFragment the fragment to add
     */
    public void addTailScriptFragment( Script script, String scriptFragment ) {
        StringBuilder sb = scriptTailMap.get( script );
        if( sb == null ) {
            sb = new StringBuilder();
            scriptTailMap.put( script, sb );
        } else {
            sb.append( "\n\n" );
        }
        sb.append( scriptFragment );
    }

    /**
     * Adds a fragment to the specified install script at the head section.
     *
     * @param script the install script
     * @param scriptFragment the fragment to add
     */
    public void addHeadScriptFragment( Script script, String scriptFragment ) {
        StringBuilder sb = scriptHeadMap.get( script );
        if( sb == null ) {
            sb = new StringBuilder();
            scriptHeadMap.put( script, sb );
        } else {
            sb.append( "\n\n" );
        }
        sb.append( scriptFragment );
    }

    /**
     * Creates the {post|pre}{inst|rm} install script files. Scripts are generated only when
     * they are required.
     *
     * @throws IOException on I/O failures
     */
    private void createScripts() throws IOException {
        for( Script script : Script.values() ) {
            String scriptName = script.toString().toLowerCase();
            Template tmpl = new Template( "unix/deb/template/" + scriptName + ".sh" );
            StringBuilder head = scriptHeadMap.get( script );
            StringBuilder tail = scriptTailMap.get( script );

            if( head == null && tail == null ) {
                continue;
            }

            tmpl.setPlaceholder( "variables", deb.getVariablesTemplate() );

            if( head != null ) {
                tmpl.setPlaceholder( "head", head.toString() );
            } else {
                tmpl.setPlaceholder( "head", "" );
            }

            if( tail != null ) {
                tmpl.setPlaceholder( "tail", tail.toString() );
            } else {
                tmpl.setPlaceholder( "tail", "" );
            }

            File file = new File( buildDir, scriptName );
            tmpl.writeTo( file );
            Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
            perms.add( PosixFilePermission.OWNER_READ );
            perms.add( PosixFilePermission.OWNER_WRITE );
            perms.add( PosixFilePermission.GROUP_READ );
            perms.add( PosixFilePermission.OTHERS_READ );
            perms.add( PosixFilePermission.OWNER_EXECUTE );
            perms.add( PosixFilePermission.GROUP_EXECUTE );
            perms.add( PosixFilePermission.OTHERS_EXECUTE );
            Files.setPosixFilePermissions( file.toPath(), perms );
        }
    }
}
