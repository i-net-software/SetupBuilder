// Test script to verify Launch4j workdir classifier selection
// Run with: groovy test-launch4j-classifier.groovy

import org.gradle.internal.os.OperatingSystem

def OS = OperatingSystem.current()

def getWorkdirClassifier() {
    if( OS.isWindows() ) {
        return "workdir-win32"
    } else if( OS.isLinux() ) {
        String arch = System.getProperty( "os.arch" )
        if( arch != null && ( arch.contains( "64" ) || arch.equals( "amd64" ) || arch.equals( "x86_64" ) ) ) {
            return "workdir-linux64"
        }
        return "workdir-linux"
    } else if( OS.isMacOsX() ) {
        return "workdir-mac"
    }
    return "workdir-win32"
}

println "Platform Detection Test"
println "======================="
println "OS: ${System.getProperty('os.name')}"
println "Arch: ${System.getProperty('os.arch')}"
println "Windows: ${OS.isWindows()}"
println "Linux: ${OS.isLinux()}"
println "macOS: ${OS.isMacOsX()}"
println ""
println "Selected Launch4j classifier: ${getWorkdirClassifier()}"
println ""

// Expected classifiers from Maven Central
def expectedClassifiers = [
    "workdir-win32",
    "workdir-linux",
    "workdir-linux64",
    "workdir-mac"
]

def selected = getWorkdirClassifier()
if( selected in expectedClassifiers ) {
    println "✓ Selected classifier '${selected}' is valid"
} else {
    println "✗ Selected classifier '${selected}' may not exist in Maven Central"
    println "  Available classifiers: ${expectedClassifiers.join(', ')}"
}

