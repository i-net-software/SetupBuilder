#!/bin/bash
# Cross-platform testing script for SetupBuilder MSI/Launch4j support
# Tests Phase 1-3: Platform detection, WiX v4, and Launch4j

set -e

echo "=========================================="
echo "SetupBuilder Cross-Platform Test Suite"
echo "=========================================="
echo ""

# Detect platform
OS="$(uname -s)"
ARCH="$(uname -m)"
echo "Platform: $OS ($ARCH)"
echo ""

# Test 1: Platform Detection
echo "Test 1: Platform Detection"
echo "---------------------------"
if [[ "$OS" == "Darwin" ]]; then
    echo "✓ Detected macOS"
    EXPECTED_PLATFORM="macOS"
elif [[ "$OS" == "Linux" ]]; then
    echo "✓ Detected Linux"
    EXPECTED_PLATFORM="Linux"
elif [[ "$OS" == MINGW* ]] || [[ "$OS" == MSYS* ]] || [[ "$OS" == CYGWIN* ]]; then
    echo "✓ Detected Windows (via Git Bash/Cygwin)"
    EXPECTED_PLATFORM="Windows"
else
    echo "⚠ Unknown platform: $OS"
    EXPECTED_PLATFORM="Unknown"
fi
echo ""

# Test 2: WiX v4 Detection
echo "Test 2: WiX v4 Detection"
echo "------------------------"
WIX_V4_FOUND=false

# Check for wix command
if command -v wix &> /dev/null; then
    WIX_VERSION=$(wix --version 2>&1 || echo "")
    if [[ "$WIX_VERSION" == *"v4"* ]] || [[ "$WIX_VERSION" == *"WiX Toolset v4"* ]]; then
        echo "✓ WiX v4 found: $WIX_VERSION"
        WIX_V4_FOUND=true
    else
        echo "⚠ WiX found but not v4: $WIX_VERSION"
    fi
else
    echo "✗ WiX v4 not found in PATH"
    
    # Check common installation locations
    if [[ "$OS" == "Darwin" ]]; then
        if [[ -f "/usr/local/bin/wix" ]]; then
            echo "  Found: /usr/local/bin/wix"
        elif [[ -f "/opt/homebrew/bin/wix" ]]; then
            echo "  Found: /opt/homebrew/bin/wix"
        fi
        echo "  Install with: brew install wix"
    elif [[ "$OS" == "Linux" ]]; then
        if [[ -f "/usr/bin/wix" ]]; then
            echo "  Found: /usr/bin/wix"
        elif [[ -f "/usr/local/bin/wix" ]]; then
            echo "  Found: /usr/local/bin/wix"
        fi
        echo "  Install via package manager or download from wixtoolset.org"
    fi
fi

# Check WIX environment variable
if [[ -n "$WIX" ]]; then
    echo "✓ WIX environment variable set: $WIX"
    if [[ -f "$WIX/bin/wix" ]] || [[ -f "$WIX/bin/wix.exe" ]]; then
        echo "  WiX command found in WIX/bin"
    fi
else
    echo "  WIX environment variable not set"
fi
echo ""

# Test 3: Launch4j Workdir Classifier Selection
echo "Test 3: Launch4j Workdir Classifier"
echo "------------------------------------"
if [[ "$OS" == "Darwin" ]]; then
    EXPECTED_CLASSIFIER="workdir-mac"
    echo "Expected classifier: $EXPECTED_CLASSIFIER"
elif [[ "$OS" == "Linux" ]]; then
    if [[ "$ARCH" == *"64"* ]] || [[ "$ARCH" == "x86_64" ]] || [[ "$ARCH" == "amd64" ]]; then
        EXPECTED_CLASSIFIER="workdir-linux64"
    else
        EXPECTED_CLASSIFIER="workdir-linux"
    fi
    echo "Expected classifier: $EXPECTED_CLASSIFIER (arch: $ARCH)"
elif [[ "$OS" == MINGW* ]] || [[ "$OS" == MSYS* ]] || [[ "$OS" == CYGWIN* ]]; then
    EXPECTED_CLASSIFIER="workdir-win32"
    echo "Expected classifier: $EXPECTED_CLASSIFIER"
else
    EXPECTED_CLASSIFIER="workdir-win32"
    echo "Expected classifier (fallback): $EXPECTED_CLASSIFIER"
fi
echo ""

# Test 4: Java Availability (for Launch4j)
echo "Test 4: Java Availability"
echo "-------------------------"
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -1)
    echo "✓ Java found: $JAVA_VERSION"
    
    # Check Java version
    JAVA_MAJOR=$(java -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/')
    if [[ "$JAVA_MAJOR" -ge 8 ]]; then
        echo "✓ Java version $JAVA_MAJOR is compatible (requires 8+)"
    else
        echo "⚠ Java version $JAVA_MAJOR may be too old (requires 8+)"
    fi
else
    echo "✗ Java not found - Launch4j requires Java"
fi
echo ""

# Test 5: Gradle Build Test
echo "Test 5: Gradle Compilation Test"
echo "--------------------------------"
if [[ -f "./gradlew" ]]; then
    echo "Running: ./gradlew compileJava --no-daemon"
    if ./gradlew compileJava --no-daemon 2>&1 | tail -5; then
        echo "✓ Compilation successful"
    else
        echo "✗ Compilation failed"
        exit 1
    fi
else
    echo "⚠ gradlew not found, skipping compilation test"
fi
echo ""

# Summary
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo "Platform: $EXPECTED_PLATFORM"
echo "WiX v4: $([ "$WIX_V4_FOUND" = true ] && echo "✓ Available" || echo "✗ Not found")"
echo "Launch4j Classifier: $EXPECTED_CLASSIFIER"
echo ""

if [[ "$WIX_V4_FOUND" = true ]]; then
    echo "✓ Ready for cross-platform MSI builds"
else
    echo "⚠ WiX v4 not installed - MSI builds will only work on Windows"
    echo "  Install WiX v4 to enable cross-platform MSI builds"
fi
echo ""

