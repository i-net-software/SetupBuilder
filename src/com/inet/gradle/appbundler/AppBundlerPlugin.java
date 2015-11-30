package com.inet.gradle.appbundler;

import java.util.HashMap;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;

/**
 * Plugin for the appBundler Task that will create a bundled application for OSX
 * @author gamma
 *
 */
public class AppBundlerPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
        // apply the BasePlugin to make the base features like "clean" available by default.
        HashMap<String, Class<?>> plugin = new HashMap<>();
        plugin.put( "plugin", BasePlugin.class );
        project.apply( plugin );
        
        project.getExtensions().create( "appBundler", AppBundler.class, project );
        project.getTasks().create( "bundleApp", AppBundlerGradleTask.class );
	}
}
