package com.inet.gradle.setup.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.gradle.api.GradleException;
import org.gradle.api.Project;

import com.inet.gradle.setup.image.icns.IcnsCodec;
import com.inet.gradle.setup.image.icns.IconSuite;
import com.inet.gradle.setup.image.image4j.codec.ico.ICODecoder;
import com.inet.gradle.setup.image.image4j.codec.ico.ICOEncoder;

public class ImageFactory {

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

        File file = new File( dir, "icons." + format );
        switch( format ) {
            case "ico":
                ICOEncoder.write( images, file );
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
                throw new GradleException( "Unsupported image format: " + format );
        }
        return file;
    }
}
