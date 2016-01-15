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
}
