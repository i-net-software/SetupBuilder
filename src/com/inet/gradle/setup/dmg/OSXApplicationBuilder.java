package com.inet.gradle.setup.dmg;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.apache.tools.ant.types.FileSet;
import org.gradle.api.GradleException;
import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.AbstractBuilder;
import com.inet.gradle.setup.Application;
import com.inet.gradle.setup.DesktopStarter;
import com.inet.gradle.setup.DocumentType;
import com.inet.gradle.setup.Service;
import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.image.ImageFactory;
import com.inet.gradle.setup.util.ResourceUtils;
import com.oracle.appbundler.AppBundlerTask;
import com.oracle.appbundler.Architecture;
import com.oracle.appbundler.BundleDocument;

public class OSXApplicationBuilder extends AbstractBuilder<Dmg> {

	/**
	 * Setup this builder.
	 * @param task - original task
	 * @param setup - original setup
	 * @param fileResolver - original fileResolver
	 */
	protected OSXApplicationBuilder(Dmg task, SetupBuilder setup, FileResolver fileResolver) {
		super(task, setup, fileResolver);
	}

	/**
	 * Create Application from service provided. Also create the preference panel
	 * and put it into the application. Will also create the installer wrapper package of this application
	 * @param service the service
	 * @throws Throwable error.
	 */
	public void buildService(Service service) throws Throwable {

		new PreparedAppBundlerTask(service).prepare().finish();
		createPreferencePane( service );
	}

	/**
	 * Create Application from the desktop starter provided
	 * @param application
	 * @throws Exception
	 */
	public void buildApplication(DesktopStarter application) throws Exception {

		PreparedAppBundlerTask bundlerTask = new PreparedAppBundlerTask( application );
		bundlerTask.prepare();

		// add file extensions
		for (DocumentType doc : application.getDocumentType()) {
			BundleDocument bundle = new BundleDocument();
			bundle.setExtensions(String.join(",", doc.getFileExtension()));
			bundle.setName(doc.getName());
			bundle.setRole(doc.getRole()); // Viewer or Editor
			bundle.setIcon(getApplicationIcon().toString());
			bundlerTask.appBundler.addConfiguredBundleDocument(bundle);
		}

		bundlerTask.finish();
	}

	private class PreparedAppBundlerTask {

		private Application application;
		public AppBundlerTask appBundler;

		public PreparedAppBundlerTask(Application application) {
			this.application = application;
			appBundler = new AppBundlerTask();
		}

		public PreparedAppBundlerTask prepare() throws Exception {
			appBundler.setOutputDirectory(buildDir);
			appBundler.setName(application.getDisplayName());
			appBundler.setDisplayName(application.getDisplayName());

			String version = setup.getVersion();
			appBundler.setVersion(version);
			int idx = version.indexOf('.');
			if (idx >= 0) {
				idx = version.indexOf('.', idx + 1);
				if (idx >= 0) {
					version = version.substring(0, idx);
				}
			}
			appBundler.setShortVersion(version);

			appBundler.setExecutableName(application.getClass().equals(Service.class) ? ((Service)application).getId() : setup.getAppIdentifier() );
			appBundler.setIdentifier(application.getMainClass());
			appBundler.setMainClassName(application.getMainClass());
			appBundler.setJarLauncherName(application.getMainJar());
			appBundler.setCopyright(setup.getVendor());

			appBundler.setIcon(getApplicationIcon());
			Architecture x86_64 = new Architecture();
			x86_64.setName("x86_64");
			appBundler.addConfiguredArch(x86_64);

			return this;
		}

		public PreparedAppBundlerTask finish() throws Exception {
			bundleJre();

			org.apache.tools.ant.Project antProject = new org.apache.tools.ant.Project();
			appBundler.setProject(antProject);
			appBundler.execute();

			File destiantion = new File(buildDir, application.getDisplayName() + ".app");
			task.copyTo( new File(destiantion, "Contents/Java") );

			setApplicationFilePermissions( destiantion );
			return this;
		}

		/**
		 * Bundle the Java VM if set.
		 * 
		 * @param appBundler
		 *            the ANT Task
		 */
		private void bundleJre() {
			Object jre = setup.getBundleJre();
			if (jre == null) {
				return;
			}
			File jreDir;
			try {
				jreDir = task.getProject().file(jre);
			} catch (Exception e) {
				jreDir = null;
			}
			if (jreDir == null || !jreDir.isDirectory()) {
				ArrayList<String> command = new ArrayList<>();
				command.add("/usr/libexec/java_home");
				command.add("-v");
				command.add(jre.toString());
				command.add("-F");
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				exec(command, null, baos);
				jreDir = new File(baos.toString().trim());
				if (!jreDir.isDirectory()) {
					throw new GradleException("bundleJre version " + jre
							+ " can not be found in: " + jreDir);
				}
			}
			task.getProject().getLogger().lifecycle("\tbundle JRE: " + jreDir);
			FileSet fileSet = new FileSet();
			fileSet.setDir(jreDir);

			fileSet.appendIncludes(new String[] { "jre/*", "jre/lib/",
					"jre/bin/java" });

			fileSet.appendExcludes(new String[] { "jre/lib/deploy/",
					"jre/lib/deploy.jar", "jre/lib/javaws.jar",
					"jre/lib/libdeploy.dylib", "jre/lib/libnpjp2.dylib",
					"jre/lib/plugin.jar", "jre/lib/security/javaws.policy" });

			appBundler.addConfiguredRuntime(fileSet);
		}
	}

	/**
	 * Returns the icns application icon file
	 * 
	 * @return Icon file
	 * @throws IOException
	 */
	public File getApplicationIcon() throws IOException {
		Object iconData = setup.getIcons();
		return ImageFactory.getImageFile(task.getProject(), iconData, buildDir, "icns");
	}

	
	/**
	 * * Unpack the preference pane from the SetupBuilder,
	 * * Modify the icon and text
	 * * Put into the main application bundle
	 * 
	 * @throws Throwable
	 */
	private void createPreferencePane( Service service ) throws Throwable {

		String displayName = service.getDisplayName();

		// Unpack
		File setPrefpane = TempPath.getTempFile("packages", "SetupBuilderOSXPrefPane.prefPane.zip");
		InputStream input = getClass().getResourceAsStream("service/SetupBuilderOSXPrefPane.prefPane.zip");
		FileOutputStream output = new FileOutputStream( setPrefpane );
        ResourceUtils.copyData(input, output);
        input.close();
        output.close();

		File resourcesOutput = new File(buildDir, displayName  + ".app/Contents/Resources");
        ResourceUtils.unZipIt(setPrefpane, resourcesOutput);

		// Rename to app-name
        File prefPaneLocation = new File(resourcesOutput, displayName + ".prefPane");

        // rename helper Tool
        File prefPaneContents = new File(resourcesOutput, "SetupBuilderOSXPrefPane.prefPane/Contents");
		Path iconPath = getApplicationIcon().toPath();

		Files.copy(iconPath, new File(prefPaneContents, "Resources/SetupBuilderOSXPrefPane.app/Contents/Resources/applet.icns").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		Files.move(new File(prefPaneContents, "MacOS/SetupBuilderOSXPrefPane").toPath(), new File( prefPaneContents, "MacOS/"+ displayName ).toPath(),  java.nio.file.StandardCopyOption.REPLACE_EXISTING );
		Files.move(new File(prefPaneContents, "Resources/SetupBuilderOSXPrefPane.app").toPath(), new File(prefPaneContents, "Resources/"+ displayName +".app").toPath(),  java.nio.file.StandardCopyOption.REPLACE_EXISTING );
		System.out.println("Unpacked the Preference Pane to: " + prefPaneContents.getAbsolutePath() );

		// Make executable
		setApplicationFilePermissions( new File(prefPaneContents, "Resources/"+ displayName +".app/Contents/MacOS/applet") );
    	
		// Rename prefPane
		Files.move(new File(resourcesOutput, "SetupBuilderOSXPrefPane.prefPane").toPath(), prefPaneLocation.toPath(),  java.nio.file.StandardCopyOption.REPLACE_EXISTING );
        Files.delete(setPrefpane.toPath());

		// Copy Icon
		Files.copy(iconPath, new File(prefPaneLocation, "Contents/Resources/ProductIcon.icns").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		
		// Patch Info.plist
		File prefPanePLIST = new File(prefPaneLocation, "Contents/Info.plist");
		setPlistProperty( prefPanePLIST, ":CFBundleIdentifier", setup.getMainClass() != null ? setup.getMainClass() : setup.getAppIdentifier() + ".prefPane" );
		setPlistProperty( prefPanePLIST, ":CFBundleName", displayName + " Preference Pane" );
		setPlistProperty( prefPanePLIST, ":CFBundleExecutable", displayName );
		setPlistProperty( prefPanePLIST, ":NSPrefPaneIconLabel", displayName );

		setPlistProperty( prefPanePLIST, ":CFBundleExecutable", displayName );
		
		File servicePLIST = new File(prefPaneLocation, "Contents/Resources/service.plist");
		setPlistProperty( servicePLIST, ":Name", displayName );
		setPlistProperty( servicePLIST, ":Label", setup.getMainClass() != null ? setup.getMainClass() : setup.getAppIdentifier() );
		setPlistProperty( servicePLIST, ":Program", "/Library/" + setup.getApplication() + "/" + displayName + ".app/Contents/MacOS/" + service.getId() );
		setPlistProperty( servicePLIST, ":Description", setup.getServices().get(0).getDescription() );
		setPlistProperty( servicePLIST, ":Version", setup.getVersion() );
	}

	/**
	 * Modify a plist file
	 * @param plist file to modify
	 * @param property property to set
	 * @param value of property
	 */
	public void setPlistProperty(File plist, String property, String value) {
		
		// Set Property in plist file
		// /usr/libexec/PlistBuddy -c "Set PreferenceSpecifiers:19:Titles:0 $buildDate" "$BUNDLE/Root.plist"
        ArrayList<String> command = new ArrayList<>();
        command.add( "/usr/libexec/PlistBuddy" );
        command.add( "-c" );
        command.add( "Set " + property +  " " + value );
        command.add( plist.getAbsolutePath() );
    	exec( command );
	}


	/**
	 * Set File permissions to the resulting application
	 * @throws IOException
	 */
	private void setApplicationFilePermissions( File destiantion ) throws IOException {
		
		// Set Read on all files and folders
		ArrayList<String> command = new ArrayList<>();
        command.add( "chmod" );
        command.add( "-R" );
        command.add( "a+r" );
		command.add( destiantion.getAbsolutePath() );
        exec( command );
        
		// Set execute on all folders.
        command = new ArrayList<>();
        command.add( "find" );
        command.add( destiantion.getAbsolutePath() );
        command.add( "-type" );
        command.add( "d" );
        command.add( "-exec" );
        command.add( "chmod" );
        command.add( "a+x" );
        command.add( "{}" );
        command.add( ";" );
        exec( command );
	}
}
