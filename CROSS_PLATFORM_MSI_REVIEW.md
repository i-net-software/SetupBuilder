# Cross-Platform MSI/Launch4j Support - Review & Implementation Plan

## Current State Analysis

### MSI Builder (`MsiBuilder.java`)

**Platform-Specific Issues:**

1. **WiX Toolset Detection** (`getToolPath()` method):
   - Hardcoded Windows paths: `ProgramFiles(x86)`, `ProgramW6432`
   - Uses Windows path separators (`\\`)
   - Looks for `.exe` files only
   - Searches in Windows-specific locations (`C:\Program Files\WiX Toolset v3.x\bin\`)
   - Throws error if `ProgramFiles` environment variable not found

2. **Tool Execution**:
   - Calls `candle.exe` and `light.exe` directly
   - Uses Windows path separators in command line arguments (line 178: `buildDir.getAbsolutePath() + '\\'`)
   - Extracts Windows-specific tools: `MsiTran.exe`, `signtool.exe`

3. **Script Execution**:
   - Uses `cscript` (Windows Script Host) for VBScript files
   - Extracts Windows-specific VBS files: `wilangid.vbs`, `wisubstg.vbs`

### Launch4j Creator (`Launch4jCreator.java`)

**Current Implementation:**
- ✅ **Already Cross-Platform**: Uses Java-based Launch4j library (`net.sf.launch4j:launch4j:3.50`)
- ⚠️ **Platform-Specific Dependency**: Downloads `launch4j:3.50:workdir-win32` (Windows-specific workdir)
- ✅ **No Platform Checks**: Works on any platform that can run Java

**Issues:**
- Downloads Windows-specific workdir package even on Linux/macOS
- May need Linux/macOS workdir packages if available

## Requirements for Cross-Platform Support

### 1. WiX Toolset

**Option A: WiX v4 (Recommended)**
- WiX v4 has cross-platform support (Windows, Linux, macOS)
- Tools: `wix` (replaces `candle` and `light`)
- Can be installed via:
  - Windows: MSI installer
  - Linux: Package managers or binaries
  - macOS: Homebrew (`brew install wix`)
- **Pros**: Official cross-platform support, modern tooling
- **Cons**: Breaking changes from v3, may require WiX file updates

**Option B: WiX v3 with Cross-Platform Wrapper**
- Keep WiX v3 compatibility
- Use Wine or Docker to run WiX tools on Linux/macOS
- **Pros**: No breaking changes
- **Cons**: Requires Wine/Docker, more complex setup

**Option C: Alternative Tools**
- Use `msitools` (Linux) / `wixl` (part of msitools)
- Use `msi-packager` (Node.js, works on all platforms)
- **Pros**: Native cross-platform tools
- **Cons**: Different syntax, may require WiX file conversion

### 2. Launch4j

**Current Status:**
- Launch4j Java library works cross-platform
- Need to check if workdir packages exist for Linux/macOS
- If not, may need to:
  - Skip workdir extraction on non-Windows
  - Use alternative packaging for non-Windows builds
  - Download platform-specific workdir if available

### 3. Path Handling

**Issues:**
- Hardcoded backslashes (`\\`) in multiple places
- Windows-specific path construction
- Need to use `File.separator` or `File.pathSeparator`

### 4. Platform Detection

**Current State:**
- No platform checks - assumes Windows
- Need to add: `OperatingSystem.current().isWindows()`

## Implementation Plan

### Phase 1: Platform Detection & Path Handling

1. **Add Platform Detection**:
   ```java
   import org.gradle.internal.os.OperatingSystem;
   
   private boolean isWindows() {
       return OperatingSystem.current().isWindows();
   }
   ```

2. **Fix Path Separators**:
   - Replace hardcoded `\\` with `File.separator`
   - Use `File.pathSeparator` for PATH-like strings
   - Update `getToolPath()` to handle Unix paths

3. **Update Tool Path Detection**:
   - Check `WIX` environment variable (works on all platforms)
   - For Windows: Keep existing ProgramFiles search
   - For Linux/macOS: Check common installation paths:
     - `/usr/bin/wix` (if installed via package manager)
     - `~/wix/bin/wix` (user installation)
     - `$WIX/bin/wix` (environment variable)

### Phase 2: WiX Toolset Support

**Recommended: Support WiX v4 with v3 fallback**

1. **Detect WiX Version**:
   - Try WiX v4 first (`wix` command)
   - Fall back to WiX v3 (`candle.exe`/`light.exe`)

2. **Unified Tool Interface**:
   ```java
   private void callWixTool(String tool, ArrayList<String> parameters) {
       if (isWixV4()) {
           callWixV4Tool(parameters);
       } else {
           callWixV3Tool(tool, parameters);
       }
   }
   ```

3. **WiX v4 Support**:
   - Use `wix build` command instead of `candle` + `light`
   - Update command-line arguments for v4 syntax
   - Handle different output formats

### Phase 3: Launch4j Cross-Platform

1. **Platform-Specific Workdir**:
   ```java
   String workdirClassifier = isWindows() ? "workdir-win32" : 
                               OperatingSystem.current().isLinux() ? "workdir-linux" :
                               "workdir-macos";
   dependencies.add(configName, "net.sf.launch4j:launch4j:3.50:" + workdirClassifier);
   ```

2. **Fallback if Workdir Not Available**:
   - If platform-specific workdir doesn't exist, skip extraction
   - Launch4j should still work without workdir on non-Windows

### Phase 4: Script Execution

1. **VBScript Alternatives**:
   - For non-Windows: Use Java-based alternatives or skip
   - Or use Wine to run VBScript files
   - Or rewrite scripts in cross-platform language (Groovy/Java)

2. **MsiTran.exe**:
   - WiX v4 may have cross-platform equivalent
   - Or use Wine/Docker for v3 compatibility

## Files to Modify

### High Priority
1. `src/com/inet/gradle/setup/msi/MsiBuilder.java`
   - `getToolPath()` - Add cross-platform path detection
   - `candle()` - Fix path separators, add WiX v4 support
   - `light()` - Fix path separators, add WiX v4 support
   - `callWixTool()` - Add platform detection and version handling

2. `src/com/inet/gradle/setup/msi/Msi.java`
   - Add platform checks before build
   - Update error messages for cross-platform

### Medium Priority
3. `src/com/inet/gradle/setup/msi/Launch4jCreator.java`
   - Platform-specific workdir selection
   - Handle missing workdir gracefully

4. `src/com/inet/gradle/setup/msi/MsiBuilder.java` (continued)
   - `patchLangID()` - Replace `cscript` with cross-platform solution
   - `msitran()` - Cross-platform alternative or Wine wrapper

### Low Priority
5. Documentation
   - Update README with cross-platform requirements
   - Add installation instructions for WiX on Linux/macOS
   - Document Wine/Docker option for WiX v3

## Testing Strategy

1. **Unit Tests**:
   - Test platform detection
   - Test path separator handling
   - Test WiX tool detection on different platforms

2. **Integration Tests**:
   - Build MSI on Windows (baseline)
   - Build MSI on Linux (with WiX v4)
   - Build MSI on macOS (with WiX v4)
   - Test Launch4j on all platforms

3. **CI/CD Updates**:
   - Add Linux and macOS runners to GitHub Actions
   - Test MSI builds on all platforms

## Dependencies & Tools

### Required
- **WiX v4**: Cross-platform WiX toolset
  - Windows: Download from wixtoolset.org
  - Linux: Package manager or binaries
  - macOS: `brew install wix`

### Optional
- **Wine**: For running WiX v3 tools on Linux/macOS (fallback)
- **Docker**: Alternative to Wine for running Windows tools

## Breaking Changes

### Potential Breaking Changes
1. **WiX v4**: If we switch to WiX v4, users may need to:
   - Update WiX files (if using custom templates)
   - Install WiX v4 instead of v3
   - Update build scripts

2. **Path Handling**: Changes to path handling should be backward compatible

3. **Launch4j**: Should remain backward compatible

## Migration Path

1. **Phase 1**: Add platform detection, keep Windows-only behavior with warnings
2. **Phase 2**: Add WiX v4 support alongside v3
3. **Phase 3**: Add cross-platform Launch4j support
4. **Phase 4**: Full cross-platform support with documentation

## Estimated Effort

- **Phase 1** (Platform Detection): 2-4 hours
- **Phase 2** (WiX v4 Support): 8-16 hours
- **Phase 3** (Launch4j): 2-4 hours
- **Phase 4** (Scripts & Polish): 4-8 hours
- **Testing**: 8-16 hours
- **Documentation**: 2-4 hours

**Total**: ~26-52 hours

## Next Steps

1. ✅ Review current implementation (this document)
2. ⏭️ Update version to 8.4.24-SNAPSHOT
3. ⏭️ Implement Phase 1 (Platform Detection & Path Handling)
4. ⏭️ Test on Linux/macOS with WiX v4
5. ⏭️ Implement Phase 2 (WiX v4 Support)
6. ⏭️ Implement Phase 3 (Launch4j Cross-Platform)
7. ⏭️ Update documentation
8. ⏭️ Add CI/CD tests for cross-platform builds

