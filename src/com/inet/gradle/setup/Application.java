package com.inet.gradle.setup;


public class Application {
	protected final SetupBuilder			setup;
	private String 							name, baseName, displayName, mainJar, mainClass, executable, description, workDir;
	private Object icons;

	/**
     * Create a new DesktopStarter
     * @param setup current SetupBuilder
     */
    public Application( SetupBuilder setup ) {
        this.setup = setup;
    }

    /**
     * Returns the name of this starter.
     * @return the name of this starter
     */
    public String getName() {
        if( name != null ) {
            return name;
        }
        return setup.getBaseName();
    }

    /**
     * Set the name of the application.
     * @param name the name to set
     */
    public void setName( String name ) {
        this.name = name;
    }
    
    /**
     * Get the baseName for the application
     * @return the base name
     */
    public String getBaseName() {
        if( baseName != null ) {
            return baseName;
        }
        return setup.getBaseName();
    }

    /**
     * Set the base name of the service name. Can contains slashes if it should be in a sub directory. This change the
     * working directory of the service. On Windows ".exe" is added for the file name of the service.
     * 
     * @param baseName the new baseName
     */
    public void setBaseName( String baseName ) {
        this.baseName = baseName;
    }

    /**
     * get the displayName of the application.
     * @return the display name
     */
    public String getDisplayName() {
        if( displayName != null ) {
            return displayName;
        }
        return getName();
	}

    /**
     * Set the displayName of the application.
     * @param displayName the name to set
     */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
     * Returns the description
     * @return the description
     */
    public String getDescription() {
        if( description != null ) {
            return description;
        }
        return setup.getDescription();
    }

    /**
     * Sets the description.
     * @param description the description to set
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * Get an executable
     * @return the executable
     */
    public String getExecutable() {
        return executable;
    }

    /**
     * Set the executable file. If this is a relative pfad then it is relative to the install directory.
     * @param executable Set the executable file.
     */
    public void setExecutable( String executable ) {
        this.executable = executable;
    }
 
    /**
     * Get the main jar file.
     * 
     * @return the main jar
     */
    public String getMainJar() {
        if( mainJar != null ) {
            return mainJar;
        }
        return setup.getMainJar();
    }

    /**
     * Set the jar which contains the main class. This jar must contains all references to all other needed jar files in the manifest.
     * 
     * @param mainJar the main jar file
     */
    public void setMainJar( String mainJar ) {
        this.mainJar = mainJar;
    }

    /**
     * Get the main class.
     * 
     * @return the class name
     */
    public String getMainClass() {
        if( mainClass != null ) {
            return mainClass;
        }
        return setup.getMainClass();
    }

    /**
     * Set the main class of the Java application.
     * 
     * @param mainClass the class name
     */
    public void setMainClass( String mainClass ) {
        this.mainClass = mainClass;
    }

    /**
     * Get the icons for this desktop/shortcut entry.
     * @return the icons
     */
    public Object getIcons() {
        return icons;
    }

    /**
     * Set the icons for desktop/shortcut entry. This can be one or multiple images in different size. The usage depends on the
     * platform. This can be an *.ico file, *.icns file or an list of Java readable image files like *.png or *.jpeg.
     * 
     * @param icons the icons
     */
    public void setIcons( Object icons ) {
        this.icons = icons;
    }

    /**
     * Get the working directory of this desktop/shortcut entry.
     * @return the working directory
     */
    public String getWorkDir() {
        return workDir;
    }

    /**
     * Set the working directory. If not set then the installation directory is used.
     * @param workDir the work directory.
     */
    public void setWorkDir( String workDir ) {
        this.workDir = workDir;
    }
}
