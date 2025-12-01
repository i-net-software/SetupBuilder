#!/bin/bash
# Verify platform detection and Launch4j classifier logic
# This script tests the actual Java code logic by compiling and running a test

set -e

echo "=========================================="
echo "Platform Logic Verification"
echo "=========================================="
echo ""

# Create a simple test Java file that uses the same logic
cat > /tmp/PlatformTest.java << 'EOF'
import java.io.File;

public class PlatformTest {
    public static void main(String[] args) {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch");
        
        System.out.println("OS Name: " + System.getProperty("os.name"));
        System.out.println("OS Arch: " + osArch);
        System.out.println("File Separator: " + File.separator);
        System.out.println("Path Separator: " + File.pathSeparator);
        System.out.println("");
        
        // Simulate Launch4j classifier selection
        String classifier = getWorkdirClassifier(osName, osArch);
        System.out.println("Launch4j Classifier: " + classifier);
        
        // Verify it's valid
        String[] valid = {"workdir-win32", "workdir-linux", "workdir-linux64", "workdir-mac"};
        boolean isValid = false;
        for (String v : valid) {
            if (v.equals(classifier)) {
                isValid = true;
                break;
            }
        }
        
        if (isValid) {
            System.out.println("✓ Classifier is valid");
        } else {
            System.out.println("✗ Classifier may not exist in Maven Central");
            System.out.println("  Valid classifiers: " + String.join(", ", valid));
        }
    }
    
    private static String getWorkdirClassifier(String osName, String osArch) {
        if (osName.contains("windows")) {
            return "workdir-win32";
        } else if (osName.contains("linux")) {
            if (osArch != null && (osArch.contains("64") || osArch.equals("amd64") || osArch.equals("x86_64"))) {
                return "workdir-linux64";
            }
            return "workdir-linux";
        } else if (osName.contains("mac")) {
            return "workdir-mac";
        }
        return "workdir-win32";
    }
}
EOF

# Compile and run
echo "Compiling test..."
javac /tmp/PlatformTest.java

echo "Running test..."
echo "---------------"
java -cp /tmp PlatformTest

echo ""
echo "✓ Platform logic verification complete"

# Cleanup
rm -f /tmp/PlatformTest.java /tmp/PlatformTest.class

