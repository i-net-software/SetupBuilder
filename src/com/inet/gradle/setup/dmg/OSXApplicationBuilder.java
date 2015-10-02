package com.inet.gradle.setup.dmg;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
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
import com.oracle.appbundler.AppBundlerTask;
import com.oracle.appbundler.Architecture;
import com.oracle.appbundler.BundleDocument;

public class OSXApplicationBuilder extends AbstractBuilder<Dmg> {

	protected OSXApplicationBuilder(Dmg task, SetupBuilder setup,
			FileResolver fileResolver) {
		super(task, setup, fileResolver);
	}

	public void buildService(Service service) throws Exception {

		new PreparedAppBundlerTask(service).prepare().finish();
	}

	public void buildApplication(DesktopStarter application) throws Exception {

		PreparedAppBundlerTask bundlerTask = new PreparedAppBundlerTask(
				application);
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
			appBundler.setName(application.getName());
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

			appBundler.setExecutableName(application.getBaseName());
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

		public PreparedAppBundlerTask finish() {
			bundleJre();

			org.apache.tools.ant.Project antProject = new org.apache.tools.ant.Project();
			appBundler.setProject(antProject);
			appBundler.execute();

			task.copyTo(new File(buildDir, application.getDisplayName()
					+ ".app/Contents/Java"));

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
	private File getApplicationIcon() throws IOException {
		Object iconData = setup.getIcons();
		return ImageFactory.getImageFile(task.getProject(), iconData, buildDir,
				"icns");
	}

}
