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
package com.inet.gradle.setup.unix.rpm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.function.BiConsumer;

import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.Template;
import com.inet.gradle.setup.abstracts.DesktopStarter;
import com.inet.gradle.setup.abstracts.DocumentType;
import com.inet.gradle.setup.abstracts.LocalizedResource;
import com.inet.gradle.setup.abstracts.Service;
import com.inet.gradle.setup.unix.UnixBuilder;
import com.inet.gradle.setup.util.Logging;

public class RpmBuilder extends UnixBuilder<Rpm, SetupBuilder> {

    private RpmControlFileBuilder controlBuilder;

    /**
     * Create a new instance
     *
     * @param rpm the calling task
     * @param setup the shared settings
     * @param fileResolver the file Resolver
     */
    public RpmBuilder( Rpm rpm, SetupBuilder setup, FileResolver fileResolver ) {
        super( rpm, setup, fileResolver );
    }

    /**
     * Build the RedHat package in different steps with the 'rpmbuild'.
     *
     * <dl>
     * <dt>copy files</dt>
     * <dd>copy the files specified in the gradle script to the BUILD/usr/share/archivesBaseName directory.</dd>
     * <dd>The files must be in the BUILD directory because the 'prep' step will copy all the files from there to the BUILDROOT directory.</dd>
     * <dd>The 'rpmbuild' deletes the BUILDROOT directory before building the package. Thats why we need to copy the files into it.</dd>
     * <dt>SPEC file creation</dt>
     * <dd>The 'rpmbuild' requires a configuration files ending with .spec.</dd>
     * <dd>This spec file contains all required informations (like name, version, dependencies) and scripts that are executed during the creation and installing of the package.</dd>
     * <dt>change file permissions</dt>
     * <dd>Before the package is created the permissions of all files need to be set correctly.</dd>
     * <dd>All directories and executables will be changed to 755 permission and other files to 644.</dd>
     * <dt>create the package</dt>
     * <dd>Creates the package with 'rpmbuild'</dd>
     * </dl>
     */
    public void build() {
        try {
            String release = task.getRelease();
            if( release == null || release.length() == 0 ) {
                release = "1";
            }
            File filesPath = new File( buildDir.getAbsolutePath() + "/BUILD" + task.getInstallationRoot() );
            task.copyTo( filesPath );
            changeFilePermissionsTo644( filesPath );

            // Add a bundled java vm if required. Will update the variable to indicate the java-main program
            addBundleJre( filesPath );

            controlBuilder = new RpmControlFileBuilder( super.task, setup, new File( buildDir, "SPECS" ), javaMainExecutable );

            controlBuilder.addScriptFragment( RpmControlFileBuilder.Script.PREINSTHEAD, "# check for java. the service will need it and other parts probably too"
                            + "[ ! -x '" + javaMainExecutable + "' ] && echo \"The program 'java' does not exist but will be needed. (Looked up at '" + javaMainExecutable + "')\" && exit 1 || :"
                            + "\n\n"
                            );

            String daemonuser = task.getDaemonUser();
            if( !daemonuser.equalsIgnoreCase( "root" ) ) {
                controlBuilder.addScriptFragment( RpmControlFileBuilder.Script.POSTINSTHEAD, "useradd -r -m -U " + daemonuser + " 2> /dev/null || true\n"
                                + "[ \"$(id " + daemonuser + " 2> /dev/null 1>&2; echo $?)\" == \"0\" ]"
                                + " && chown -R " + daemonuser + ":" + daemonuser + " '" + task.getInstallationRoot() + "'"
                                + " && chmod -R g+w '" + task.getInstallationRoot() + "' || true \n\n" );
            }

            for( Service service : setup.getServices() ) {
                setupService( service );
            }

            for( DesktopStarter starter : setup.getDesktopStarters() ) {
                setupStarter( starter );
            }

            if( !daemonuser.equalsIgnoreCase( "root" ) ) {
                controlBuilder.addScriptFragment( RpmControlFileBuilder.Script.POSTRMTAIL, "userdel -r " + daemonuser + " 2> /dev/null || true \n" );
                controlBuilder.addScriptFragment( RpmControlFileBuilder.Script.POSTRMTAIL, "groupdel " + daemonuser + " 2> /dev/null || true \n" );
            }

            // copy the license files
            for( LocalizedResource license : setup.getLicenseFiles() ) {
                File licensetarget = new File( buildDir.getAbsolutePath() + "/BUILD/usr/share/licenses/" + setup.getApplication() + "/" + license.getResource().getName() );
                licensetarget.mkdirs();
                Files.copy( license.getResource().toPath(), licensetarget.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
            }

            controlBuilder.build();

            changeDirectoryPermissionsTo755( buildDir );

            createRpmPackage();

        } catch( RuntimeException ex ) {
            throw ex;
        } catch( Exception ex ) {
            throw new RuntimeException( ex );
        }
    }

    /**
     * Creates the files and the corresponding script section for the specified service.
     *
     * @param service the service
     * @throws IOException on errors during creating or writing a file
     */
    private void setupService( Service service ) throws IOException {
    	String serviceUnixName = service.getId();
    	String installationRoot = task.getInstallationRoot();
    	String workingDir = installationRoot + (service.getWorkDir() != null ? "/" + service.getWorkDir() : "");
    	String mainJarPath = workingDir + "/" + service.getMainJar();
    	String version = task.getVersion();

    	if(task.isUseInitD()) {
    		String initTemplate = "unix/init-service.sh";
    		Template initScript = new Template( initTemplate  );
    		initScript.setPlaceholder( "name", serviceUnixName );
    		initScript.setPlaceholder( "majorversion", version.substring( 0, version.indexOf( '.' ) ) );
    		initScript.setPlaceholder( "displayName", setup.getApplication() );
    		initScript.setPlaceholder( "description", service.getDescription() );
    		initScript.setPlaceholder( "wait", "2" );

    		initScript.setPlaceholder( "workdir", workingDir );
    		initScript.setPlaceholder( "mainJar", mainJarPath );
    		initScript.setPlaceholder( "startArguments", (service.getStartArguments()).trim() );
    		initScript.setPlaceholder( "javaVMArguments", String.join( " ", service.getJavaVMArguments()).trim() );

    		initScript.setPlaceholder( "mainClass", service.getMainClass() );
    		initScript.setPlaceholder( "daemonUser", task.getDaemonUser() );
    		initScript.setPlaceholder( "daemonExec", javaMainExecutable );
    		initScript.setPlaceholder( "additionalServiceScript", task.getAdditionalServiceScript() );

    		String initScriptFile = "BUILD/etc/init.d/" + serviceUnixName;
    		initScript.writeTo( createFile( initScriptFile , true ) );
    		controlBuilder.addConfFile( initScriptFile );

    		controlBuilder.addScriptFragment( RpmControlFileBuilder.Script.PREINSTHEAD, "[ -f \"/etc/init.d/" + serviceUnixName + "\" ] && \"/etc/init.d/" + serviceUnixName + "\" stop || true" );
    		controlBuilder.addScriptFragment( RpmControlFileBuilder.Script.POSTINSTTAIL, "if [ -f \"/etc/init.d/" + serviceUnixName + "\" ]  && [ \"" + installationRoot + "\" != \"$RPM_INSTALL_PREFIX\" ] ; then\n"
    				+ "echo replace path\n"
    				+ "sed -i 's|'" + installationRoot + "'|'$RPM_INSTALL_PREFIX'|g' /etc/init.d/" + serviceUnixName
    				+ "\nfi" );
    		
    		controlBuilder.addScriptFragment( RpmControlFileBuilder.Script.POSTINSTTAIL, "( [ -f \"/etc/init.d/" + serviceUnixName + "\" ] && chkconfig --add " + serviceUnixName + " && systemctl enable " + serviceUnixName + " ) || true" );
            controlBuilder.addScriptFragment( RpmControlFileBuilder.Script.PRERMHEAD, "[ -f \"/etc/init.d/" + serviceUnixName + "\" ] && /etc/init.d/" + serviceUnixName + " stop || true" );
            controlBuilder.addScriptFragment( RpmControlFileBuilder.Script.PRERMHEAD, "( [ -f \"/etc/init.d/" + serviceUnixName + "\" ] && systemctl disable " + serviceUnixName + " && chkconfig --del " + serviceUnixName + " ) || true" );    
    	} else {

    		String systemdTemplate = "unix/systemd.service";
    		Template systemdScript = new Template( systemdTemplate  );
    		systemdScript.setPlaceholder( "name", serviceUnixName );
    		systemdScript.setPlaceholder( "majorversion", version.substring( 0, version.indexOf( '.' ) ) );
    		systemdScript.setPlaceholder( "displayName", setup.getApplication() );
    		systemdScript.setPlaceholder( "description", service.getDescription() );
    		systemdScript.setPlaceholder( "wait", "2" );

    		systemdScript.setPlaceholder( "workdir", workingDir );
    		systemdScript.setPlaceholder( "mainJar", mainJarPath );
    		systemdScript.setPlaceholder( "startArguments", (service.getStartArguments()).trim() );
    		systemdScript.setPlaceholder( "javaVMArguments", String.join( " ", service.getJavaVMArguments()).trim() );

    		systemdScript.setPlaceholder( "mainClass", service.getMainClass() );
    		systemdScript.setPlaceholder( "daemonUser", task.getDaemonUser() );
    		systemdScript.setPlaceholder( "daemonExec", javaMainExecutable );
    		systemdScript.setPlaceholder( "servicePreScript", task.getServicePreScript() );
    		systemdScript.setPlaceholder( "servicePostScript", task.getServicePostScript() );

    		String systemdScriptFile = "BUILD/usr/lib/systemd/system/" + serviceUnixName + ".service";
    		systemdScript.writeTo( createFile( systemdScriptFile , true ) );
    		
    		controlBuilder.addScriptFragment( RpmControlFileBuilder.Script.POSTINSTTAIL, "( [ -f \"/usr/lib/systemd/system/" + serviceUnixName + ".service\" ] && systemctl enable " + serviceUnixName + " ) || true" );
    		controlBuilder.addScriptFragment( RpmControlFileBuilder.Script.PREINSTHEAD, "[ -f \"/usr/lib/systemd/system/" + serviceUnixName + ".service\" ] && systemctl stop \"" + serviceUnixName + "\" || true" );
    		controlBuilder.addScriptFragment( RpmControlFileBuilder.Script.PRERMHEAD, "[ -f \"/usr/lib/systemd/system/" + serviceUnixName + ".service\" ] && systemctl stop " + serviceUnixName + " || true" );
    		controlBuilder.addScriptFragment( RpmControlFileBuilder.Script.PRERMHEAD, "( [ -f \"/usr/lib/systemd/system/" + serviceUnixName + ".service\" ] && systemctl disable " + serviceUnixName + " ) || true" );
    	}
        
        

        // copy a default service file if set
        if( task.getDefaultServiceFile() != null ) {
            File serviceDestFile = createFile(  "BUILD/etc/sysconfig/" + serviceUnixName, true );
            Files.copy( task.getDefaultServiceFile().toPath(), serviceDestFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
        }
        
        if ( task.shouldStartDefaultService() ) {
        	if(task.isUseInitD()) {
        		controlBuilder.addScriptFragment( RpmControlFileBuilder.Script.POSTINSTTAIL, "service " + serviceUnixName + " start || true" );
        	} else {
        		controlBuilder.addScriptFragment( RpmControlFileBuilder.Script.POSTINSTTAIL, "systemctl start " + serviceUnixName + " || true" );
        	}
        }
        
    }

    /**
     * Creates the files and the corresponding scripts for the specified desktop starter.
     *
     * @param starter the desktop starter
     * @throws IOException on errors during creating or writing a file
     */
    // share
    private void setupStarter( DesktopStarter starter ) throws IOException {
        String unixName = starter.getDisplayName();
        String consoleStarterPath = "/usr/bin/" + unixName;
        try (FileWriter fw = new FileWriter( createFile( "BUILD" + consoleStarterPath, true ) )) {
            fw.write( "#!/bin/sh\n" );
            if( starter.getExecutable() != null ) {
                fw.write( "\"" + task.getInstallationRoot() + "/" + starter.getExecutable() + "\" " + starter.getStartArguments() + " \"$@\"" );
            } else {
                fw.write( "\"" + javaMainExecutable + "\" " + String.join( " ", starter.getJavaVMArguments()) + " -cp \"" + task.getInstallationRoot() + "/" + starter.getMainJar() + "\" " + starter.getMainClass() + " " + starter.getStartArguments() + " \"$@\"" );
            }
        }

        int[] iconSizes = { 16, 32, 48, 64, 128, 256, 512 };

        String iconName = "";
        if( starter.getIcons() != null ) {
            iconName = starter.getIcons().toString();
            int index = iconName.lastIndexOf( '/' );
            if( index > -1 ) {
                iconName = iconName.substring( index + 1 );
            }
            // icons must be png files and should named like that
            if( !iconName.endsWith( ".png" ) ) {
                index = iconName.lastIndexOf( '.' );
                if( index > -1 ) {
                    iconName = iconName.substring( 0, index );
                }
                iconName = iconName + ".png";
            }
        }

        for( int size : iconSizes ) {
            File iconDir = new File( buildDir, "BUILD/usr/share/icons/hicolor/" + size + "x" + size + "/apps/" );
            iconDir.mkdirs();
            File scaledFile = setup.getIconForType( iconDir, "png" + size );
            if( scaledFile != null ) {
                File iconFile;
                if( starter.getIcons() != null ) {

                    iconFile = new File( iconDir, iconName );
                } else {
                    iconFile = new File( iconDir, unixName + ".png" );
                }

                scaledFile.renameTo( iconFile );
                setPermissions( iconFile, false );
            }
        }

        try (FileWriter fw = new FileWriter( createFile( "BUILD/usr/share/applications/" + unixName + ".desktop", false ) )) {
            fw.write( "[Desktop Entry]\n" );
            fw.write( "Name=" + starter.getDisplayName() + "\n" );
            fw.write( "Comment=" + starter.getDescription().replace( '\n', ' ' ) + "\n" );
            if( starter.getExecutable() != null ) {
                fw.write( "Exec=\"" + task.getInstallationRoot() + "/" + starter.getExecutable() + "\"\n" );
            } else {
                fw.write( "Exec=\"/" + consoleStarterPath + "\" %F\n" );
            }
            if( starter.getIcons() != null ) {
                int index = iconName.lastIndexOf( '.' );
                if( index > -1 ) {
                    iconName = iconName.substring( 0, index ); // as of the Icon Theme Specification the icon name should be without extension
                }
                fw.write( "Icon=" + iconName + "\n" );
            } else {
                fw.write( "Icon=" + unixName + "\n" );
            }
            String cwd = starter.getWorkDir();
            if( cwd != null ) {
                if( cwd.isEmpty() || cwd.equals( "." )) {
                    fw.write( "Path=" + task.getInstallationRoot() + "\n" );
                } else {
                    fw.write( "Path=" + cwd + "\n" );
                }
            }
            fw.write( "Terminal=false\n" );
            fw.write( "StartupNotify=true\n" );
            fw.write( "Type=Application\n" );
            
            String mime = "";
            if( starter.getMimeTypes() != null ) {
            	mime = starter.getMimeTypes();
            }
            for( String scheme: starter.getSchemes() ) {
            	if(mime.length() > 0) {
            		mime = mime + ";";
            	}
            	mime = mime + "x-scheme-handler/" +scheme;
            }
            if( mime.length() > 0 ) {
                fw.write( "MimeType=" + mime + "\n" );
            }
            if( starter.getCategories() != null ) {
                fw.write( "Categories=" + starter.getCategories() + "\n" );
            }
        }

        // register the mime type and the default app for the extensions
        for( DocumentType docType : starter.getDocumentType() ) {
            for( String extension : docType.getFileExtension() ) {
                String simpleVendor = setup.getVendor();
                simpleVendor = simpleVendor.replaceAll( "\\W", "" );
                try (FileWriter fw = new FileWriter( createFile( "BUILD/" + task.getInstallationRoot() + "/" + simpleVendor + "-" + extension + ".xml", false ) )) {
                    fw.write( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" );
                    fw.write( "<mime-info xmlns=\"http://www.freedesktop.org/standards/shared-mime-info\">\n" );

                    // if there was a mime type for the starter it will override the mime types of the documentType
                    if( starter.getMimeTypes() != null ) {
                        fw.write( "    <mime-type type=\"" + starter.getMimeTypes() + "\">\n" );
                    } else {
                        fw.write( "    <mime-type type=\"" + docType.getMimetype() + "\">\n" );
                    }

                    fw.write( "        <comment>" + setup.getApplication() + "</comment>\n" );
                    fw.write( "        <glob-deleteall/>\n" );
                    fw.write( "        <glob pattern=\"*." + extension + "\"/>\n" );
                    fw.write( "    </mime-type>\n" );
                    fw.write( "</mime-info>\n" );
                }
                controlBuilder.addScriptFragment( RpmControlFileBuilder.Script.POSTINSTTAIL, "xdg-mime install \"" + task.getInstallationRoot() + "/" + simpleVendor + "-" + extension + ".xml\" || true" );
                controlBuilder.addScriptFragment( RpmControlFileBuilder.Script.PRERMHEAD, "xdg-mime uninstall \"" + task.getInstallationRoot() + "/" + simpleVendor + "-" + extension + ".xml\" || true" );

                String iconame = unixName;
                if( starter.getIcons() != null ) {
                    iconame = iconName;
                }
                controlBuilder.addScriptFragment( RpmControlFileBuilder.Script.POSTINSTTAIL, "xdg-icon-resource install --context mimetypes --novendor --size 48 /usr/share/icons/hicolor/48x48/apps/" + iconame + ".png " + iconame + " || true" );
                controlBuilder.addScriptFragment( RpmControlFileBuilder.Script.PRERMHEAD, "xdg-icon-resource uninstall --context mimetypes --size 48 " + iconame + " || true" );

                // we don't want to overwrite the default application and it seems that doing it per hand is the proper way under unix.
                // so we don't do it here.
//                String mimetypes = docType.getMimetype();
//                if( starter.getMimeTypes() != null ) {
//                    mimetypes = starter.getMimeTypes();
//                }
//
//                controlBuilder.addScriptFragment( Script.POSTINSTTAIL, "if [ -z \"$SUDO_USER\" ]; then" );
//                controlBuilder.addScriptFragment( Script.POSTINSTTAIL, "  su $LOGNAME -c \"xdg-mime default '" + unixName + ".desktop' " + mimetypes + " || true\";" );
//                controlBuilder.addScriptFragment( Script.POSTINSTTAIL, "else" );
//                controlBuilder.addScriptFragment( Script.POSTINSTTAIL, "  su $SUDO_USER -c \"xdg-mime default '" + unixName + ".desktop' " + mimetypes + " || true\";" );
//                controlBuilder.addScriptFragment( Script.POSTINSTTAIL, "fi" );
            }
        }

    }

    /**
     * Creates a file in the build path structure.
     *
     * @param path the path relative to the root of the build path
     * @param executable if set to <tt>true</tt> the executable bit will be set in the permission flags
     * @return the created file
     * @throws IOException on errors during creating the file or setting the permissions
     */
    // share
    private File createFile( String path, boolean executable ) throws IOException {
        File file = new File( buildDir, path );
        if( !file.getParentFile().exists() ) {
            file.getParentFile().mkdirs();
        }
        file.createNewFile();

        setPermissions( file, executable );
        return file;
    }

    /**
     * execute the command to generate the RPM package
     *
     * rpmbuild -ba -clean "--define=_topdir buildDir(rpm)" SPECS/basename.spec
     *
     */
    private void createRpmPackage() {

        ArrayList<String> command = new ArrayList<>();
        command.add( "rpmbuild" );
        command.add( "-ba" );
        command.add( "-v" );
        command.add( "--clean" );
        command.add( "--define=_topdir " + buildDir.getAbsolutePath() );
        command.add( "--define=_build_id_links none" );
        command.add( "SPECS/" + setup.getAppIdentifier() + ".spec" );
        exec( command );
    }

    /**
     * A consumer interface that can throw exceptions further up the chain.
     * @author gamma
     *
     * @param <T> first element that will be accepted
     * @param <P> second element that will be accepted
     */
    @FunctionalInterface
    private interface ThrowingBiConsumer<T,P> extends BiConsumer<T,P> {

        @Override
        default void accept(final T elem, final P mele) {
            try {
                acceptThrows(elem, mele);
            } catch (final Exception e) {
                /* Do whatever here ... */
                Logging.syserr("handling an exception...");
                throw new RuntimeException(e);
            }
        }

        void acceptThrows(T elem, P mele) throws Exception;
    }
}
