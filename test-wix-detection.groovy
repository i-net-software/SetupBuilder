// Test script to verify WiX v4 detection logic
// Run with: groovy test-wix-detection.groovy

import java.io.File

def isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("windows")
}

def findCommandInPath( String command ) {
    String path = System.getenv( "PATH" )
    if( path == null ) {
        return null
    }

    String[] pathDirs = path.split( File.pathSeparator )
    for( String dir : pathDirs ) {
        File commandFile = new File( dir, command )
        if( commandFile.exists() && commandFile.canExecute() ) {
            return commandFile.getAbsolutePath()
        }
    }

    String wixEnv = System.getenv( "WIX" )
    if( wixEnv != null ) {
        File commandFile = new File( wixEnv, "bin" + File.separator + command )
        if( commandFile.exists() && commandFile.canExecute() ) {
            return commandFile.getAbsolutePath()
        }
    }

    return null
}

def getUnixToolPath( String tool ) {
    List<String> searchPaths = []
    
    String path = System.getenv( "PATH" )
    if( path != null ) {
        String[] pathDirs = path.split( File.pathSeparator )
        for( String dir : pathDirs ) {
            File toolFile = new File( dir, tool )
            if( toolFile.exists() && toolFile.canExecute() ) {
                return toolFile.getAbsolutePath()
            }
        }
    }

    if( System.getProperty("os.name").toLowerCase().contains("mac") ) {
        searchPaths.add( "/usr/local/bin/" + tool )
        searchPaths.add( "/opt/homebrew/bin/" + tool )
        searchPaths.add( System.getProperty( "user.home" ) + "/.local/bin/" + tool )
    } else {
        searchPaths.add( "/usr/bin/" + tool )
        searchPaths.add( "/usr/local/bin/" + tool )
        searchPaths.add( System.getProperty( "user.home" ) + "/.local/bin/" + tool )
    }

    String userHome = System.getProperty( "user.home" )
    searchPaths.add( userHome + "/wix/bin/" + tool )
    searchPaths.add( userHome + "/.wix/bin/" + tool )

    String wixEnv = System.getenv( "WIX" )
    if( wixEnv != null ) {
        searchPaths.add( 0, wixEnv + "/bin/" + tool )
    }

    for( String pathStr : searchPaths ) {
        File toolFile = new File( pathStr )
        if( toolFile.exists() && toolFile.canExecute() ) {
            return toolFile.getAbsolutePath()
        }
    }

    return null
}

println "WiX Detection Test"
println "==================="
println "Platform: ${System.getProperty('os.name')}"
println ""

String wixCommand = isWindows() ? "wix.exe" : "wix"
String wixPath = findCommandInPath( wixCommand )

if( wixPath != null ) {
    println "✓ Found WiX command: ${wixPath}"
    
    // Try to get version
    try {
        ProcessBuilder pb = new ProcessBuilder( wixPath, "--version" )
        Process process = pb.start()
        BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream() ) )
        String line = reader.readLine()
        if( line != null ) {
            println "  Version: ${line}"
            if( line.contains("v4") || line.contains("WiX Toolset v4") ) {
                println "  ✓ Detected as WiX v4"
            } else {
                println "  ⚠ May not be WiX v4"
            }
        }
        process.waitFor()
    } catch( Exception e ) {
        println "  ⚠ Could not check version: ${e.message}"
    }
} else {
    println "✗ WiX command '${wixCommand}' not found in PATH"
    
    if( !isWindows() ) {
        println ""
        println "Checking common Unix locations..."
        String unixPath = getUnixToolPath( wixCommand )
        if( unixPath != null ) {
            println "  Found: ${unixPath}"
        } else {
            println "  Not found in common locations"
            if( System.getProperty("os.name").toLowerCase().contains("mac") ) {
                println "  Install with: brew install wix"
            } else {
                println "  Install via package manager or download from wixtoolset.org"
            }
        }
    }
}

String wixEnv = System.getenv( "WIX" )
if( wixEnv != null ) {
    println ""
    println "WIX environment variable: ${wixEnv}"
    File wixDir = new File( wixEnv )
    if( wixDir.exists() ) {
        println "  Directory exists: ✓"
        File binDir = new File( wixDir, "bin" )
        if( binDir.exists() ) {
            println "  bin/ directory exists: ✓"
        } else {
            println "  bin/ directory missing: ✗"
        }
    } else {
        println "  Directory does not exist: ✗"
    }
} else {
    println ""
    println "WIX environment variable: not set"
}

