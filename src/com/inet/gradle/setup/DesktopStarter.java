package com.inet.gradle.setup;

/**
 * Definition of an executable which can be started on the desktop (e.g. an entry in the start menu on Windows)
 */
public class DesktopStarter {
    private final SetupBuilder setup;
    private String             name, mainJar, mainClass;
    private String             startArguments;
    private String             description;
    private String             mimeTypes;
    private String             categories;

    /**
     * Create a new DesktopStarter
     * @param setup current SetupBuilder
     */
    public DesktopStarter( SetupBuilder setup ) {
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
     * Set the name of the service.
     * @param name the name to set
     */
    public void setName( String name ) {
        this.name = name;
    }
    
    /**
     * Returns the description
     * @return the description
     */
    public String getDescription() {
        if( description == null ) {
            return "";
        }
        return description;
    }

    /**
     * Sets the description.
     * @param description the description to set
     */
    public void setDescription( String description ) {
        this.description = description;
    }
 
    /**
     * Returns the command-line arguments for starting.
     * @return the command-line arguments for starting
     */
    public String getStartArguments() {
        if (startArguments == null) {
            return "";
        }
        return startArguments;
    }

    /**
     * Sets the command-line arguments for starting .
     * @param startArguments the command-line arguments for starting
     */
    public void setStartArguments( String startArguments ) {
        this.startArguments = startArguments;
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
     * Sets the mime type is application is associated with. Multiple mime types are separated 
     * by semicolons.
     * @param mimeTypes
     */
    public void setMimeTypes( String mimeTypes ) {
        this.mimeTypes = mimeTypes;
    }
    
    /**
     * Returns the mime types separated by semicolon.
     * @return the mime types or <tt>null</tt> if not set
     */
    public String getMimeTypes() {
        return mimeTypes;
    }
    
    /**
     * Sets the categories as defined by Freedesktop. Multiple categories are separated
     * by semicolons.
     * @see http://standards.freedesktop.org/menu-spec/latest/apa.html
     * @param categories
     */
    public void setCategories( String categories ) {
        this.categories = categories;
    }
    
    /**
     * Returns the categories separated by semicolon.
     * @return the categories or <tt>null</tt> if not set
     */
    public String getCategories() {
        return categories;
    }
}
