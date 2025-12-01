# Cross-Platform Testing Guide

This document describes how to test the cross-platform MSI/Launch4j support (Phases 1-3) on different platforms.

## Test Scripts

### 1. `test-cross-platform.sh`
Comprehensive test script that checks:
- Platform detection
- WiX v4 availability
- Launch4j workdir classifier selection
- Java availability
- Gradle compilation

**Usage:**
```bash
./test-cross-platform.sh
```


## Testing on macOS

### Prerequisites
1. **WiX v4** (optional, for MSI builds):
   ```bash
   brew install wix
   ```

2. **Java 8+** (required for Launch4j):
   ```bash
   java -version  # Should show Java 8 or higher
   ```

3. **Gradle** (already included via gradlew)

### Run Tests

```bash
# Run comprehensive test
./test-cross-platform.sh

# Test compilation
./gradlew compileJava --no-daemon

# Test Launch4j classifier selection
groovy test-launch4j-classifier.groovy

# Test WiX detection
groovy test-wix-detection.groovy
```

### Expected Results

**Platform Detection:**
- ✓ Should detect macOS
- ✓ Should identify architecture (x86_64 or arm64)

**WiX v4:**
- If installed: ✓ Should find `wix` command
- If not installed: ⚠ Warning, but build should still compile

**Launch4j:**
- ✓ Should select `workdir-mac` classifier
- ✓ Should compile successfully

## Testing on Linux

### Prerequisites
1. **WiX v4** (optional, for MSI builds):
   ```bash
   # Ubuntu/Debian (if available in repos)
   sudo apt-get install wix
   
   # Or download from wixtoolset.org
   # Or use package manager specific to your distribution
   ```

2. **Java 8+** (required for Launch4j):
   ```bash
   java -version  # Should show Java 8 or higher
   ```

3. **Gradle** (already included via gradlew)

### Run Tests

```bash
# Run comprehensive test
./test-cross-platform.sh

# Test compilation
./gradlew compileJava --no-daemon

# Test Launch4j classifier selection
groovy test-launch4j-classifier.groovy

# Test WiX detection
groovy test-wix-detection.groovy
```

### Expected Results

**Platform Detection:**
- ✓ Should detect Linux
- ✓ Should identify architecture (x86_64, arm64, etc.)

**WiX v4:**
- If installed: ✓ Should find `wix` command
- If not installed: ⚠ Warning, but build should still compile

**Launch4j:**
- ✓ Should select `workdir-linux64` (for 64-bit) or `workdir-linux` (for 32-bit)
- ✓ Should compile successfully

## Testing on Windows

### Prerequisites
1. **WiX v3 or v4** (required for MSI builds):
   - WiX v3: Install from wixtoolset.org
   - WiX v4: Install via .NET tool: `dotnet tool install --global wix`

2. **Java 8+** (required for Launch4j)

### Run Tests

```bash
# Run comprehensive test (Git Bash)
./test-cross-platform.sh

# Test compilation
./gradlew compileJava --no-daemon
```

### Expected Results

**Platform Detection:**
- ✓ Should detect Windows

**WiX:**
- ✓ Should find WiX tools (candle.exe/light.exe for v3, or wix.exe for v4)

**Launch4j:**
- ✓ Should select `workdir-win32` classifier
- ✓ Should compile successfully

## Manual Testing Checklist

### Phase 1: Platform Detection & Path Handling
- [ ] Platform correctly detected (Windows/Linux/macOS)
- [ ] Path separators use `File.separator` (not hardcoded `\\`)
- [ ] WiX tool path detection works on all platforms
- [ ] Error messages are platform-appropriate

### Phase 2: WiX v4 Support
- [ ] WiX v4 automatically detected when available
- [ ] Falls back to WiX v3 when v4 not available
- [ ] `wix build` command used for v4
- [ ] `candle`/`light` commands used for v3
- [ ] Works on Windows, Linux, and macOS (with WiX v4 installed)

### Phase 3: Launch4j Cross-Platform
- [ ] Correct workdir classifier selected per platform:
  - Windows → `workdir-win32`
  - Linux 64-bit → `workdir-linux64`
  - Linux 32-bit → `workdir-linux`
  - macOS → `workdir-mac`
- [ ] Launch4j downloads and extracts workdir correctly
- [ ] Works on all platforms (can create Windows executables)

## CI/CD Testing

To add automated testing to GitHub Actions:

```yaml
# Add to .github/workflows/ci.yml
- name: Test cross-platform support
  run: |
    ./test-cross-platform.sh
    ./gradlew compileJava --no-daemon
```

## Troubleshooting

### WiX v4 Not Found
- **macOS**: Install with `brew install wix`
- **Linux**: Install via package manager or download binaries
- **Windows**: Install via `dotnet tool install --global wix`
- Set `WIX` environment variable to WiX installation directory

### Launch4j Workdir Not Found
- Check Maven Central for available classifiers
- Verify classifier matches your platform/architecture
- Workdir is optional - Launch4j may work without it

### Compilation Errors
- Ensure Java 8+ is installed
- Check Gradle version compatibility
- Verify all dependencies are available

## Next Steps

After verifying Phases 1-3 work correctly:
1. Test actual MSI build on Linux/macOS (requires WiX v4)
2. Test Launch4j executable creation on Linux/macOS
3. Add automated tests to CI/CD pipeline
4. Document any platform-specific issues found

