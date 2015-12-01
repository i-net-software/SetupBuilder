package com.inet.gradle.setup.dmg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.AbstractSetupTask;
import com.inet.gradle.setup.Application;
import com.inet.gradle.setup.DesktopStarter;
import com.inet.gradle.setup.Service;
import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.util.ResourceUtils;

/**
 * Build an OSX Application - service
 * @author gamma
 *
 */
public class OSXApplicationBuilder extends AbstractOSXApplicationBuilder<Dmg, SetupBuilder> {

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
	void buildService(Service service) throws Throwable {

		// We need the executable. It has a different meaning than on other systems.
		if ( service.getExecutable() == null || service.getExecutable().isEmpty() ) {
			service.setExecutable( service.getId() );
		}
		
		System.err.println("Having executable of: '" + service.getExecutable() +"'" );
		prepareApplication(service);
		finishApplication();
		copyBundleFiles( service );
		createPreferencePane( service );
		codeSignApplication( service );
	}

	/**
	 * Create Application from the desktop starter provided
	 * @param application - the application
	 * @throws Exception on errors
	 */
	void buildApplication(DesktopStarter application) throws Exception {

		// We need the executable. It has a different meaning than on other systems.
		if ( application.getExecutable() == null || application.getExecutable().isEmpty() ) {
			application.setExecutable( setup.getAppIdentifier() );
		}
		
		prepareApplication( application );
		setDocumentTypes( application.getDocumentType() );
		finishApplication();
		copyBundleFiles( application );

		codeSignApplication( application );
	}

	/**
	 * * Unpack the preference pane from the SetupBuilder,
	 * * Modify the icon and text
	 * * Put into the main application bundle
	 * @param service to create prefpane for
	 * @throws Throwable in case of error
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
		setPlistProperty( prefPanePLIST, ":CFBundleIdentifier", (setup.getMainClass() != null ? setup.getMainClass() : setup.getAppIdentifier()) + ".prefPane" );
		setPlistProperty( prefPanePLIST, ":CFBundleName", displayName + " Preference Pane" );
		setPlistProperty( prefPanePLIST, ":CFBundleExecutable", displayName );
		setPlistProperty( prefPanePLIST, ":NSPrefPaneIconLabel", displayName );

		setPlistProperty( prefPanePLIST, ":CFBundleExecutable", displayName );
		
		File servicePLIST = new File(prefPaneLocation, "Contents/Resources/service.plist");
		setPlistProperty( servicePLIST, ":Name", displayName );
		setPlistProperty( servicePLIST, ":Label", service.getMainClass() != null ? service.getMainClass() : setup.getAppIdentifier() );
		setPlistProperty( servicePLIST, ":Program", "/Library/" + setup.getApplication() + "/" + displayName + ".app/Contents/MacOS/" + service.getId() );
		setPlistProperty( servicePLIST, ":Description", service.getDescription() );
		setPlistProperty( servicePLIST, ":Version", setup.getVersion() );
		setPlistProperty( servicePLIST, ":KeepAlive", String.valueOf(service.isKeepAlive()) );
		setPlistProperty( servicePLIST, ":RunAtBoot", String.valueOf(service.isStartOnBoot()) );
		setPlistProperty( servicePLIST, ":RunAtLoad", "true" );
	}

	@Override
	protected AbstractSetupTask<SetupBuilder> getTask() {
		return task;
	}
	
	/**
	 * Sign an application
	 * @param application to sign
	 */
	private void codeSignApplication( Application application ) {
		if ( task.getCodeSign() == null ) {
			return;
		}
		task.getCodeSign().signApplication( new File(buildDir, application.getDisplayName() + ".app") );
	}
}
