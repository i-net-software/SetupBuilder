package com.inet.gradle.setup.dmg;

/**
 * Link for the preferences
 * Takes a title and an action
 * @author gamma
 *
 */
public class PreferencesLink {

	private String title;
	private String action;
	private boolean runAsRoot = false;
	
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @return the action
	 */
	public String getAction() {
		return action;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @param action the action to set
	 */
	public void setAction(String action) {
		this.action = action;
	}
	/**
	 * @return the runAsRoot
	 */
	public boolean isRunAsRoot() {
		return runAsRoot;
	}
	/**
	 * @param runAsRoot the runAsRoot to set
	 */
	public void setRunAsRoot(boolean runAsRoot) {
		this.runAsRoot = runAsRoot;
	}
}
