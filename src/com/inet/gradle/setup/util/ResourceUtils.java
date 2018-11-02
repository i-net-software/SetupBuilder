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
package com.inet.gradle.setup.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Some Utils for working with resources.
 */
public class ResourceUtils {

    /**
     * Extract a resource file and save it as file.
     *
     * @param clazz the class name
     * @param name the relative resource file
     * @param dir the directory
     * @return the new file
     * @throws IOException if any error occur
     */
    public static File extract( Class<?> clazz, String name, File dir ) throws IOException {
        File file = new File( dir, name );
        file.getParentFile().mkdirs();
        try (InputStream input = clazz.getResourceAsStream( name )) {
            Files.copy( input, file.toPath(), StandardCopyOption.REPLACE_EXISTING );
        }
        return file;
    }

    /**
     * Unzip it
     *
     * @param file input zip file
     * @param folder zip file output folder
     */
    public static void unZipIt( File file, File folder ) {
        unZipIt( file, folder, null );
    }

    public static void unZipIt( File file, File folder, Function<String, String> nameClosure ) {
        unZipIt( file, folder, nameClosure, null );
    }

    public static void unZipIt( File file, File folder, Function<String, String> nameClosure, Function<InputStream, InputStream> streamClosure ) {
        unZipIt( file, folder, "", nameClosure, null );
    }

    public static void unZipIt( File file, File folder, String startsWith, Function<String, String> nameClosure, Function<InputStream, InputStream> streamClosure ) {

        // create output directory is not exists
        if( !folder.exists() ) {
            folder.mkdir();
        }

        // get the zip file content
        try (ZipFile zipFile = new ZipFile( file )) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while( entries.hasMoreElements() ) {
                ZipEntry zipEntry = entries.nextElement();

                String fileName = zipEntry.getName();
                if( !zipEntry.isDirectory() ) {
                    if ( fileName.startsWith( startsWith ) ) {
                        fileName = fileName.substring( startsWith.length() );
                    } else {
                        continue;
                    }

                    if( nameClosure != null ) {
                        fileName = nameClosure.apply( fileName );
                    }

                    try (InputStream input = zipFile.getInputStream( zipEntry )) {
                        File target = new File( folder, fileName );
                        target.getParentFile().mkdirs();

                        @SuppressWarnings( "resource" )
                        InputStream stream = input;
                        if( streamClosure != null ) {
                            stream = streamClosure.apply( input );
                        }

                        Files.copy( stream, target.toPath(), StandardCopyOption.REPLACE_EXISTING );
                    }
                }
            }
        } catch( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes directory denoted by given path with its content.
     *
     * @param directoryPath directory path.
     * @throws IOException if an I/O error occurs, like something cannot be deleted.
     */
    public static void deleteDirectory( Path directoryPath ) throws IOException {
        Files.walkFileTree( directoryPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
                Files.delete( file );
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory( Path dir, IOException exception ) throws IOException {
                if( exception == null ) {
                    Files.delete( dir );
                    return FileVisitResult.CONTINUE;
                } else {
                    throw exception;
                }
            }
        } );
    }

    /**
     * Extract a resource directory with unknown entries. Will only extract flat structure without subdirectories
     * @param path the class location
     * @param destination the output directory
     * @return the output directory
     * @throws Exception in case of errors
     */
    public static File extractDirectory( String path, File destination ) throws Exception {

        path = path.replaceAll( "\\.", "/" ); // Class path to file path
        if ( !path.endsWith( "/" ) ) {
            path += "/"; // has to be a directory
        }

        URL dirURL = ResourceUtils.class.getClassLoader().getResource( path );

        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            // A file path already
            Logging.sysout( "Input is already an existing file: " + dirURL );
            return new File(dirURL.toURI());
        }

        if (dirURL.getProtocol().equals("jar")) {
            // a JAR path
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file

            JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
            Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar

            Logging.sysout( "Checking Jar for files: " + jarPath );
            Logging.sysout( "Path to look for: " + path );
            while(entries.hasMoreElements()) {
                JarEntry nextElement = entries.nextElement();
                String name = nextElement.getName();

                Logging.sysout( "Checking: " + name );
                if (name.startsWith(path)) { //filter according to the path
                    String entry = name.substring(path.length());
                    int checkSubdir = entry.indexOf("/");
                    if (checkSubdir >= 0 ) {
                        // if it is a subdirectory, we don't want it
                        File nDest = new File( destination, entry.substring( 0, checkSubdir ) );
                        if ( !nDest.exists() ) {
                            nDest.mkdirs();
                        }
                        extractDirectory( name, nDest );
                        continue;
                    } else if ( entry.isEmpty() ) {
                        continue;
                    }

                    try (InputStream input = jar.getInputStream( nextElement ) ) {
                        File output = new File(destination, entry);
                        Files.copy( input, output.toPath(), StandardCopyOption.REPLACE_EXISTING );
                    }
                }
            }
            jar.close();
            return destination;
        }

        throw new UnsupportedOperationException("Cannot list files for URL "+dirURL);
    }

    /**
     * Recursively copy files from source to destination
     * @param source the source
     * @param destination the destination
     * @throws IOException an Exception
     */
    public static void copy(File source, File destination) throws IOException {
        Path sourcePath = source.toPath();
        Path destinationPath = destination.toPath();

        destination.getParentFile().mkdirs();

        Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS);

        if (Files.isDirectory(sourcePath)) {
            String[] files = source.list();

            for (int i = 0; i < files.length; i++) {
                String file = files[i];
                copy(new File(source, file), new File(destination, file));
            }
        }
    }
}
