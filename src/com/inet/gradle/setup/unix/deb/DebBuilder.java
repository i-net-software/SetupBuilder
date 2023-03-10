/*
 * Copyright 2015 - 2016 i-net software
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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.Template;
import com.inet.gradle.setup.abstracts.DesktopStarter;
import com.inet.gradle.setup.abstracts.DocumentType;
import com.inet.gradle.setup.abstracts.LocalizedResource;
import com.inet.gradle.setup.abstracts.Service;
import com.inet.gradle.setup.unix.UnixBuilder;
import com.inet.gradle.setup.unix.deb.DebControlFileBuilder.Script;
import com.inet.gradle.setup.util.Logging;

public class DebBuilder extends UnixBuilder<Deb, SetupBuilder> {

    private DebControlFileBuilder  controlBuilder;

    private DebDocumentFileBuilder documentBuilder;

    /**
     * Create a new instance
     *
     * @param deb
     *            the calling task
     * @param setup
     *            the shared settings
     * @param fileResolver
     *            the file Resolver
     */
    public DebBuilder( Deb deb, SetupBuilder setup, FileResolver fileResolver ) {
        super( deb, setup, fileResolver );
    }

    /**
     * executes all necessary steps from copying to building the Debian package
     */
    public void build() {
        try {
            File filesPath = new File( buildDir, task.getInstallationRoot() );
            task.copyTo( filesPath );

            changeFilePermissionsTo644( filesPath );

            // Add a bundled java vm if required. Will update the variable to indicate the java-main program
            addBundleJre( filesPath );

            // create the package config files in the DEBIAN subfolder
            controlBuilder = new DebControlFileBuilder( super.task, setup, new File( buildDir, "DEBIAN" ) );

            addScriptsToControlFiles();

            for( Service service : setup.getServices() ) {
                setupService( service );
            }

            for( DesktopStarter starter : setup.getDesktopStarters() ) {
                setupStarter( starter );
            }

            if( setup.getLicenseFiles().size() > 0 ) {
                setupEula();
            }

            // removes only the files in the installation path
            List<String> del_files = setup.getDeleteFiles();
            for( String file : del_files ) {
                controlBuilder.addTailScriptFragment( Script.PREINST, "if [ -f \"" + task.getInstallationRoot() + "/" + file + "\" ]; then\n  rm -f \"" + task.getInstallationRoot() + "/" + file + "\"\nfi\n" );
                controlBuilder.addTailScriptFragment( Script.PRERM, "if [ -f \"" + task.getInstallationRoot() + "/" + file + "\" ]; then\n  rm -f \"" + task.getInstallationRoot() + "/" + file + "\"\nfi\n" );
            }
            // removes only the folders in the installation path
            List<String> del_folders = setup.getDeleteFolders();
            for( String folder : del_folders ) {
                controlBuilder.addTailScriptFragment( Script.PREINST, "rm -R -f \"" + task.getInstallationRoot() + "/" + folder + "\"\n" );
                controlBuilder.addTailScriptFragment( Script.PRERM, "rm -R -f \"" + task.getInstallationRoot() + "/" + folder + "\"\n" );
            }

            DesktopStarter runAfterStarter = setup.getRunAfter();
            String installationRoot = task.getInstallationRoot();
            if( runAfterStarter != null ) {
                String executable = runAfterStarter.getExecutable();
                String mainClass = runAfterStarter.getMainClass();
                String workingDir = installationRoot + (runAfterStarter.getWorkDir() != null ? "/" + runAfterStarter.getWorkDir() : "");
                String mainJarPath = workingDir + "/" + runAfterStarter.getMainJar();

                if( executable != null ) {
                    controlBuilder.addTailScriptFragment( Script.POSTINST, "( cd \"" + workingDir + "\" && " + executable + " " + runAfterStarter.getStartArguments() + " & )\n" );
                } else if( mainClass != null ) {
                    controlBuilder.addTailScriptFragment( Script.POSTINST, "( cd \"" + workingDir + "\" && \"" + javaMainExecutable + "\" " + String.join( " ", runAfterStarter.getJavaVMArguments()) + " -cp \"" + mainJarPath + "\" " + mainClass + " " + runAfterStarter.getStartArguments() + ")\n" );
                }
            }

            controlBuilder.addTailScriptFragment( Script.POSTINST, "gtk-update-icon-cache /usr/share/icons/hicolor &>/dev/null || : \n" );

            DesktopStarter runBeforeUninstall = setup.getRunBeforeUninstall();
            if( runBeforeUninstall != null ) {
                String executable = runBeforeUninstall.getExecutable();
                String mainClass = runBeforeUninstall.getMainClass();
                String workingDir = installationRoot + (runBeforeUninstall.getWorkDir() != null ? "/" + runBeforeUninstall.getWorkDir() : "");
                String mainJarPath = workingDir + "/" + runBeforeUninstall.getMainJar();

                controlBuilder.addTailScriptFragment( Script.PRERM, "case \"$1\" in remove|purge)" );
                if( executable != null ) {
                    if( task.getDaemonUser().equalsIgnoreCase( "root" ) ) {
                        controlBuilder.addTailScriptFragment( Script.PRERM, "( cd \"" + workingDir + "\" && " + executable + " " + runBeforeUninstall.getStartArguments() + " )" );
                    } else {
                        controlBuilder.addTailScriptFragment( Script.PRERM, "(su " + task.getDaemonUser() + " -c 'cd \"" + workingDir + "\" && " + executable + " " + runBeforeUninstall.getStartArguments() + "' )" );
                    }
                } else if( mainClass != null ) {
                    if( task.getDaemonUser().equalsIgnoreCase( "root" ) ) {
                        controlBuilder.addTailScriptFragment( Script.PRERM, "( cd \"" + workingDir + "\" && \"" + javaMainExecutable + "\" " + String.join( " ", runBeforeUninstall.getJavaVMArguments()) + " -cp \"" + mainJarPath + "\" " + mainClass + " " + runBeforeUninstall.getStartArguments() + ")" );
                    } else {
                        controlBuilder.addTailScriptFragment( Script.PRERM, "(su " + task.getDaemonUser() + " -c 'cd \"" + workingDir + "\" && \"" + javaMainExecutable + "\" " + String.join( " ", runBeforeUninstall.getJavaVMArguments()) + " -cp \"" + mainJarPath + "\" " + mainClass + " " + runBeforeUninstall.getStartArguments() + "' )" );
                    }
                }
                controlBuilder.addTailScriptFragment( Script.PRERM, "    ;;\nesac" );
            }

            controlBuilder.build();

            documentBuilder = new DebDocumentFileBuilder( super.task, setup, new File( buildDir, "/usr/share/doc/" + setup.getAppIdentifier() ) );
            documentBuilder.build();

            changeDirectoryPermissionsTo755( buildDir );

            createDebianPackage();

            checkDebianPackage();

        } catch( RuntimeException ex ) {
            throw ex;
        } catch( Exception ex ) {
            throw new RuntimeException( ex );
        }
    }

    /**
     * adds the pre and post step entries to the pre and post config files
     */
    private void addScriptsToControlFiles() {

        controlBuilder.addHeadScriptFragment( Script.PREINST, "# check for java. the service woll need it and other parts probably too"
                        + "[ ! -x '" + javaMainExecutable + "' ] && echo \"The program 'java' does not exist but will be needed. (Looked up at '" + javaMainExecutable + "')\" && exit 1 || :"
                        + "\n\n"
                        );

        String daemonuser = task.getDaemonUser();
        if( !daemonuser.equalsIgnoreCase( "root" ) ) {
            controlBuilder.addTailScriptFragment( Script.POSTINST, "useradd -r -m -U " + daemonuser + " 2> /dev/null || true\n"
                            + "[ \"$(id " + daemonuser + " 2> /dev/null 1>&2; echo $?)\" == \"0\" ]"
                            + " && chown -R " + daemonuser + ":" + daemonuser + " '" + task.getInstallationRoot() + "'"
                            + " && chmod -R g+w '" + task.getInstallationRoot() + "' || true \n\n"
                            );
        }

        ArrayList<String> preinsts = task.getPreinst();
        for( String preinst : preinsts ) {
            controlBuilder.addTailScriptFragment( Script.PREINST, preinst );
        }
        ArrayList<String> postinsts = task.getPostinst();
        for( String postinst : postinsts ) {
            controlBuilder.addTailScriptFragment( Script.POSTINST, postinst );
        }

        ArrayList<String> prerms = task.getPrerm();
        for( String prerm : prerms ) {
            controlBuilder.addTailScriptFragment( Script.PRERM, prerm );
        }
        ArrayList<String> postrms = task.getPostrm();
        for( String postrm : postrms ) {
            controlBuilder.addTailScriptFragment( Script.POSTRM, postrm );
        }

        if( !daemonuser.equalsIgnoreCase( "root" ) ) {
            controlBuilder.addTailScriptFragment( Script.POSTRM, "if [ \"$1\" = \"purge\" ] ; then\n" + "pkill -U " + daemonuser + " 2> /dev/null || true \n" + "sleep 2 \n" + "userdel -r " + daemonuser + " 2> /dev/null || true \n" + "groupdel " + daemonuser + " 2> /dev/null || true \n" + "fi" );
        }

    }

    private void setupEula() throws IOException {
        String templateLicenseName = setup.getAppIdentifier() + "/license";
        String templateAcceptName = setup.getAppIdentifier() + "/accept-license";
        String templateErrorName = setup.getAppIdentifier() + "/error-license";
        try (FileWriter fw = new FileWriter( createFile( "DEBIAN/templates", false ) ) ) {

            fw.write( "Template: " + templateLicenseName + "\n" );
            fw.write( "Type: note\n" );

            for( LocalizedResource localizedResource : setup.getLicenseFiles() ) {

                StringBuffer content = new StringBuffer();

                String lang = localizedResource.getLanguage().equalsIgnoreCase( setup.getDefaultResourceLanguage() ) ? "" : "-" + localizedResource.getLanguage();
                content.append( "Description" + lang + ": License agreement\n" );

                try (Scanner scanner = new Scanner( localizedResource.getResource(), "UTF8" )) {
                    while ( scanner.hasNextLine() ) {
                        String line = scanner.nextLine();
                        content.append( " " + ( line.isEmpty() ? "." : line) + "\n" );
                    }
                } finally {
                    fw.write( content.toString() );
                }
            }

            fw.write( '\n' );
            fw.write( "Template: " + templateAcceptName + "\n" );
            fw.write( "Type: boolean\n" );
            fw.write( "Description: Do you accept the license agreement?\n" );
            fw.write( "Description-de.UTF-8: Akzeptieren Sie die Lizenzvereinbarung?\n" );
            fw.write( '\n' );
            fw.write( "Template: " + templateErrorName + "\n" );
            fw.write( "Type: error\n" );
            fw.write( "Description: It is required to accept the license to install this package.\n" );
            fw.write( "Description-de.UTF-8: Zur Installation dieser Anwendung müssen Sie die Lizenz akzeptieren.\n" );
            fw.write( '\n' );
        }

        controlBuilder.addTailScriptFragment( Script.POSTRM, "if [ \"$1\" = \"remove\" ] || [ \"$1\" = \"purge\" ]  ; then\n" + "  db_purge\n" + "fi" );

        controlBuilder.addHeadScriptFragment( Script.PREINST,
                        "if [ \"$1\" = \"install\" ] ; then\n" +
                        "  db_get " + templateAcceptName + "\n" +
                        "  if [ \"$RET\" = \"true\" ]; then\n" +
                        "    echo \"License already accepted\"\n" +
                        "  else\n" +
                        "    db_input high " + templateLicenseName + " || true\n" +
//                        "    db_go\n" +
                        "    db_input high " + templateAcceptName + " || true\n" +
                        "    db_go\n" +
                        "    db_get " + templateAcceptName + "\n" +
                        "    if [ \"$RET\" != \"true\" ]; then\n" +
                        "        echo \"License was not accepted by the user\"\n" +
                        "        db_input high " + templateErrorName + " || true\n" +
                        "        db_go\n" +
                        "        db_purge\n" +
                        "        exit 1\n" +
                        "    fi\n" +
                        "  fi\n" +
                        "fi" );
    }

    /**
     * Creates the files and the corresponding script section for the specified service.
     *
     * @param service
     *            the service
     * @throws IOException
     *             on errors during creating or writing a file
     */
    private void setupService( Service service ) throws IOException {
        String serviceUnixName = service.getId();
        String installationRoot = task.getInstallationRoot();
        String workingDir = installationRoot + (service.getWorkDir() != null ? "/" + service.getWorkDir() : "");
        String mainJarPath = workingDir + "/" + service.getMainJar();
        String version = task.getVersion();

        if(task.isUseInitD()) {
        	Template initScript = new Template( "unix/init-service.sh" );
        	initScript.setPlaceholder( "name", serviceUnixName );
        	initScript.setPlaceholder( "majorversion", version.substring( 0, version.indexOf( '.' ) ) );
        	initScript.setPlaceholder( "displayName", setup.getApplication() );
        	initScript.setPlaceholder( "description", service.getDescription() );
        	initScript.setPlaceholder( "wait", "2" );

        	initScript.setPlaceholder( "workdir", workingDir );
        	initScript.setPlaceholder( "mainJar", mainJarPath );
        	initScript.setPlaceholder( "startArguments", (service.getStartArguments()).trim() );
        	initScript.setPlaceholder( "javaVMArguments", (String.join( " ", service.getJavaVMArguments())).trim() );

        	initScript.setPlaceholder( "mainClass", service.getMainClass() );
        	initScript.setPlaceholder( "daemonUser", task.getDaemonUser() );
        	initScript.setPlaceholder( "daemonExec", javaMainExecutable );
        	initScript.setPlaceholder( "additionalServiceScript", task.getAdditionalServiceScript() );

        	String initScriptFile = "etc/init.d/" + serviceUnixName;
        	initScript.writeTo( createFile( initScriptFile, true ) );
        	
        	controlBuilder.addTailScriptFragment( Script.POSTINST, "[ -f \"/etc/init.d/" + serviceUnixName + "\" ] && update-rc.d " + serviceUnixName + " defaults 91 09 >/dev/null || true" );
        	controlBuilder.addTailScriptFragment( Script.PRERM, "[ -f \"/etc/init.d/" + serviceUnixName + "\" ] && service " + serviceUnixName + " stop || true" );
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

    		String systemdScriptFile = "usr/lib/systemd/system/" + serviceUnixName + ".service";
    		systemdScript.writeTo( createFile( systemdScriptFile , true ) );
    		
    		controlBuilder.addTailScriptFragment( Script.POSTINST, "( [ -f \"/usr/lib/systemd/system/" + serviceUnixName + ".service\" ] && systemctl enable " + serviceUnixName + " ) || true" );
    		controlBuilder.addTailScriptFragment( Script.PREINST, "[ -f \"/usr/lib/systemd/system/" + serviceUnixName + ".service\" ] && systemctl stop \"" + serviceUnixName + "\" || true" );
    		controlBuilder.addTailScriptFragment( Script.PRERM, "[ -f \"/usr/lib/systemd/system/" + serviceUnixName + ".service\" ] && systemctl stop " + serviceUnixName + " || true" );
    		controlBuilder.addTailScriptFragment( Script.PRERM, "( [ -f \"/usr/lib/systemd/system/" + serviceUnixName + ".service\" ] && systemctl disable " + serviceUnixName + " ) || true" );
        }
        
        // copy a default service file if set
        if( task.getDefaultServiceFile() != null ) {
            File serviceDestFile = new File( buildDir.getAbsolutePath(), "/etc/default/" + serviceUnixName );
            serviceDestFile.mkdirs();
            Files.copy( task.getDefaultServiceFile().toPath(), serviceDestFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
        }

        
        if ( task.shouldStartDefaultService() ) {
        	if(task.isUseInitD()) {
        		controlBuilder.addTailScriptFragment( Script.POSTINST, "[ -f \"/etc/init.d/" + serviceUnixName + "\" ] && service " + serviceUnixName + " start || true" );
        	} else {
        		controlBuilder.addTailScriptFragment( Script.POSTINST, "systemctl start " + serviceUnixName + " || true" );
        	}
        	
        }

        
        controlBuilder.addTailScriptFragment( Script.POSTRM, "[ \"$1\" = \"purge\" ] && update-rc.d " + serviceUnixName + " remove >/dev/null || true " );
    }

    /**
     * Creates the files and the corresponding scripts for the specified desktop starter.
     *
     * @param starter
     *            the desktop starter
     * @throws IOException
     *             on errors during creating or writing a file
     */
    private void setupStarter( DesktopStarter starter ) throws IOException {
        String unixName = starter.getDisplayName();
        String consoleStarterPath = "usr/bin/" + unixName;
        try (FileWriter fw = new FileWriter( createFile( consoleStarterPath, true ) )) {
            fw.write( "#!/bin/bash\n" );
            if( starter.getExecutable() != null ) {
                fw.write( "\"" + task.getInstallationRoot() + "/" + starter.getExecutable() + "\" " + starter.getStartArguments() + " \"$@\"" );
            } else {
                fw.write( "\"" + javaMainExecutable + "\" " + String.join( " ", starter.getJavaVMArguments()) + " -cp \"" + task.getInstallationRoot() + "/" + starter.getMainJar() + "\" " + starter.getMainClass() + " " + starter.getStartArguments() + " \"$@\"" );
            }
        }
        int[] iconSizes = { 16, 32, 48, 64, 128 };

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
            File iconDir = new File( buildDir, "usr/share/icons/hicolor/" + size + "x" + size + "/apps/" );
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
        try (FileWriter fw = new FileWriter( createFile( "usr/share/applications/" + unixName + ".desktop", false ) )) {
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
                try (FileWriter fw = new FileWriter( createFile( task.getInstallationRoot() + "/" + simpleVendor + "-" + extension + ".xml", false ) )) {
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

                controlBuilder.addTailScriptFragment( Script.POSTINST, "xdg-mime install \"" + task.getInstallationRoot() + "/" + simpleVendor + "-" + extension + ".xml\" || true" );
                controlBuilder.addTailScriptFragment( Script.PRERM, "xdg-mime uninstall \"" + task.getInstallationRoot() + "/" + simpleVendor + "-" + extension + ".xml\" || true" );
                String iconame = unixName;
                if( starter.getIcons() != null ) {
                    iconame = iconName;
                }
                controlBuilder.addTailScriptFragment( Script.POSTINST, "xdg-icon-resource install --context mimetypes --novendor --size 48 /usr/share/icons/hicolor/48x48/apps/" + iconame + ".png " + iconame + " || true" );
                controlBuilder.addTailScriptFragment( Script.PRERM, "xdg-icon-resource uninstall --context mimetypes --size 48 " + iconame + " || true" );

                // we don't want to overwrite the default application and it seems that doing it per hand is the proper way under unix.
                // so we don't do it here.
//                String mimetypes = docType.getMimetype();
//                if( starter.getMimeTypes() != null ) {
//                    mimetypes = starter.getMimeTypes();
//                }

//                controlBuilder.addTailScriptFragment( Script.POSTINST, "if [ -z \"$SUDO_USER\" ]; then" );
//                controlBuilder.addTailScriptFragment( Script.POSTINST, "  su $LOGNAME -c \"xdg-mime default '" + unixName + ".desktop' " + mimetypes + " || true\";" );
//                controlBuilder.addTailScriptFragment( Script.POSTINST, "else" );
//                controlBuilder.addTailScriptFragment( Script.POSTINST, "  su $SUDO_USER -c \"xdg-mime default '" + unixName + ".desktop' " + mimetypes + " || true\";" );
//                controlBuilder.addTailScriptFragment( Script.POSTINST, "fi" );

            }
        }
    }

    /**
     * Creates a file in the build path structure.
     *
     * @param path
     *            the path relative to the root of the build path
     * @param executable
     *            if set to <tt>true</tt> the executable bit will be set in the permission flags
     * @return the created file
     * @throws IOException
     *             on errors during creating the file or setting the permissions
     */
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
     * execute the lintian tool to check the Debian package This will only be executed if the task 'checkPackage'
     * property is set to true
     */
    private void checkDebianPackage() {
        if( task.shouldCheckPackage() ) {
            ArrayList<String> command = new ArrayList<>();
            command.add( "lintian" );
            command.add( task.getLintianOptions() );
            command.add( task.getSetupFile().getPath() );
            exec( command );
        }
    }

    /**
     * execute the command to generate the Debian package
     */
    private void createDebianPackage() {
        try {
        ArrayList<String> command = new ArrayList<>();
        command.add( "fakeroot" );
        command.add( "dpkg-deb" );
        command.add( "--build" );
        command.add( buildDir.getAbsolutePath() );
        command.add( task.getSetupFile().getPath() );
        exec( command );
        } catch( Throwable e ) {
            Logging.syserr( "Error. Control File was:" );
            try {
                Logging.syserr( String.join( "\n", Files.readAllLines( new File(buildDir.getAbsolutePath(), "DEBIAN/control" ).toPath() ) ) );
            } catch( IOException e1 ) {
                e1.printStackTrace();
            }
            throw e;
        }
    }
}
