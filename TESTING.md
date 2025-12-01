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

2. **Java 8+** (required):
   ```bash
   java -version  # Should show Java 8 or higher
   ```

3. **Gradle** (already included via gradlew)

### Run Tests

```bash
# Run all tests (unit + integration)
./gradlew test

# Run only unit tests
./gradlew test --tests "*Test" --exclude-task integrationTest

# View test results
cat build/test-results/test/TEST-*.xml
```

### Expected Results

All 24 tests should pass:
- ✓ Platform detection tests (5)
- ✓ WiX detection tests (9)
- ✓ Launch4j classifier tests (5)
- ✓ Integration tests (5) - checks Java, WiX installation, system properties

## Testing on Linux

### Prerequisites
1. **WiX v4** (optional, for MSI builds):
   ```bash
   # Ubuntu/Debian (if available in repos)
   sudo apt-get install wix
   
   # Or download from wixtoolset.org
   # Or use package manager specific to your distribution
   ```

2. **Java 8+** (required):
   ```bash
   java -version  # Should show Java 8 or higher
   ```

3. **Gradle** (already included via gradlew)

### Run Tests

```bash
# Run all tests
./gradlew test
```

### Expected Results

All 24 tests should pass:
- ✓ Platform detection tests (5)
- ✓ WiX detection tests (9)
- ✓ Launch4j classifier tests (5)
- ✓ Integration tests (5)

## Testing on Windows

### Prerequisites
1. **WiX v3 or v4** (required for MSI builds):
   - WiX v3: Install from wixtoolset.org
   - WiX v4: Install via .NET tool: `dotnet tool install --global wix`

2. **Java 8+** (required)

### Run Tests

```bash
# Run all tests
./gradlew test
```

### Expected Results

All 24 tests should pass:
- ✓ Platform detection tests (5)
- ✓ WiX detection tests (9)
- ✓ Launch4j classifier tests (5)
- ✓ Integration tests (5)

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
- name: Run tests
  run: ./gradlew test --no-daemon

- name: Upload test results
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: test-results
    path: build/test-results/test/
    retention-days: 7
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

