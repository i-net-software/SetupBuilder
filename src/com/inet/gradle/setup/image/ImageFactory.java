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
package com.inet.gradle.setup.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.imageio.ImageIO;

import org.gradle.api.GradleException;
import org.gradle.api.Project;

import com.inet.gradle.setup.image.icns.IcnsCodec;
import com.inet.gradle.setup.image.icns.IconSuite;
import com.inet.gradle.setup.image.image4j.codec.ico.ICODecoder;
import com.inet.gradle.setup.image.image4j.codec.ico.ICOEncoder;
import com.inet.gradle.setup.image.image4j.util.ImageUtil;

/**
 * Factory for platform dependent image formats.
 * 
 * @author Volker Berlin
 */
public class ImageFactory {

    /**
     * Get a file to an icon in the platform format if set or null if not set in the gradle script
     * 
     * @param project current project for resolving the file locations
     * @param data the set values
     * @param dir directory for temporary build files if the file(s) need converted
     * @param format the platform format, currently "ico", "icns" and png<size>
     * @return a file or null
     * @throws IOException if an error occur on reading the image files
     */
    public static File getImageFile( Project project, Object data, File dir, String format ) throws IOException {
        if( data == null ) {
            return null;
        }
        ArrayList<File> files = new ArrayList<>();
        if( data instanceof Iterable ) {
            for( Object img : (Iterable)data ) {
                files.add( project.file( img ) );
            }
        } else {
            files.add( project.file( data ) );
        }
        if( files.size() == 0 ) {
            return null;
        }
        // Search if there is already an matching image 
        for( File file : files ) {
            if( file.getName().endsWith( '.' + format ) ) {
                return file;
            }
        }

        ArrayList<BufferedImage> images = new ArrayList<>();
        for( File file : files ) {
            String extension = file.getName();
            extension = extension.substring( extension.lastIndexOf( '.' ) + 1 );
            switch( extension ) {
                case "ico":
                    images.addAll( ICODecoder.read( file ) );
                    break;
                case "icns":
                    try( FileInputStream fis = new FileInputStream( file ) ) {
                        IconSuite suite = new IcnsCodec().decode( fis );
                        if( suite.getSmallIcon() != null ) {
                            images.add( suite.getSmallIcon() );
                        }
                        if( suite.getLargeIcon() != null ) {
                            images.add( suite.getLargeIcon() );
                        }
                        if( suite.getHugeIcon() != null ) {
                            images.add( suite.getHugeIcon() );
                        }
                        if( suite.getThumbnailIcon() != null ) {
                            images.add( suite.getThumbnailIcon() );
                        }
                    }
                    break;
                default:
                    images.add( ImageIO.read( file ) );
            }
        }

        File file = new File( dir, "icon." + format );
        switch( format ) {
            case "ico":
                try( FileOutputStream fos = new FileOutputStream( file ) ) {
                    ICOEncoder.write( images, fos );
                }
                break;
            case "icns":
                try( FileOutputStream fos = new FileOutputStream( file ) ) {
                    IconSuite suite = new IconSuite();
                    for( BufferedImage img : images ) {
                        switch( img.getWidth() ) {
                            case IcnsCodec.SMALL_SIZE:
                                suite.setSmallIcon( img );
                                break;
                            case IcnsCodec.LARGE_SIZE:
                                suite.setLargeIcon( img );
                                break;
                            case IcnsCodec.HUGE_SIZE:
                                suite.setHugeIcon( img );
                                break;
                            case IcnsCodec.THUMBNAIL_SIZE:
                                suite.setThumbnailIcon( img );
                                break;
                            default:
                                project.getLogger().error( "Ignore icon size: " + img.getWidth() );
                        }
                    }
                    new IcnsCodec().encode( suite, fos );
                }
                break;
            default:
                if( format.startsWith( "png" ) ) {
                    try {
                        int size = Integer.parseInt( format.substring( 3 ) );
                        BufferedImage scaledImage = scaleBestFromList( images, size );
                        if( scaledImage != null ) {
                            ImageIO.write( scaledImage, "png", file );
                        }
                        break;
                    } catch( NumberFormatException e ) {
                        // throw GradleException later
                    }             
                }
                throw new GradleException( "Unsupported image format: " + format );
        }
        return file;
    }

    /**
     * Scales the best matching image from the specified list to the specified size.
     * @param images the source images
     * @param size the target size
     * @return the scaled image, or <tt>null</tt> when the list of source images is empty
     */
    private static BufferedImage scaleBestFromList( Collection<BufferedImage> images, int size ) {
        BufferedImage best = null;
        int min = Integer.MAX_VALUE;
        for( BufferedImage img : images ) {
            int diff = Math.min( img.getWidth(), img.getHeight() ) - size;
            int p = diff < 0 ? 10000 - diff : diff;
            if( p < min ) {
                min = p;
                best = img;
            }
        }
        if( best == null ) {
            return null;
        }
        return ImageUtil.scaleImage( best, size, size );
    }
}
