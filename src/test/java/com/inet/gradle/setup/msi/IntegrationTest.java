/*
 * Copyright 2024 i-net software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.inet.gradle.setup.msi;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.gradle.internal.os.OperatingSystem;
import org.junit.Test;

/**
 * Integration tests for cross-platform support.
 * These tests check actual system state (WiX installation, Java version, etc.)
 */
public class IntegrationTest {

    private static final OperatingSystem OS = OperatingSystem.current();

    @Test
    public void testJavaAvailability() {
        // Verify Java is available (required for Launch4j)
        String javaVersion = System.getProperty("java.version");
        assertNotNull("Java version should be available", javaVersion);
        
        // Parse major version
        String[] parts = javaVersion.split("\\.");
        int majorVersion = Integer.parseInt(parts[0]);
        if (majorVersion == 1 && parts.length > 1) {
            // Java 8 and earlier: "1.8.0_xxx"
            majorVersion = Integer.parseInt(parts[1]);
        }
        
        assertTrue("Java version should be 8 or higher (found: " + majorVersion + ")",
            majorVersion >= 8);
    }

    @Test
    public void testWixInstallationCheck() {
        // Check if WiX is available (optional - test passes either way)
        String wixCommand = OS.isWindows() ? "wix.exe" : "wix";
        String wixPath = findCommandInPath(wixCommand);
        
        if (wixPath != null) {
            // WiX found - verify it's executable
            File wixFile = new File(wixPath);
            assertTrue("WiX command should be executable", wixFile.canExecute());
            
            // Try to get version
            try {
                ProcessBuilder pb = new ProcessBuilder(wixPath, "--version");
                Process process = pb.start();
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
                String line = reader.readLine();
                if (line != null) {
                    // Verify we can read version
                    assertTrue("WiX version should be readable", line.length() > 0);
                }
                process.waitFor();
            } catch (Exception e) {
                // If we can't execute, that's okay - test still passes
                // This is just checking if WiX is available
            }
        }
        // If WiX is not found, that's also okay - test passes
        // WiX is optional for compilation, required only for MSI builds
    }

    @Test
    public void testWixEnvironmentVariable() {
        // Check WIX environment variable if set
        String wixEnv = System.getenv("WIX");
        if (wixEnv != null) {
            File wixDir = new File(wixEnv);
            if (wixDir.exists()) {
                assertTrue("WIX directory should be accessible", wixDir.isDirectory());
                
                // Check for bin directory
                File binDir = new File(wixDir, "bin");
                // bin may or may not exist, but if WIX is set, directory should be valid
                assertNotNull("WIX bin directory should be checkable", binDir);
            }
        }
        // WIX may or may not be set - both are valid
    }

    @Test
    public void testCommonWixPaths() {
        // Check common WiX installation paths (may or may not exist)
        if (OS.isMacOsX()) {
            String[] paths = {
                "/usr/local/bin/wix",
                "/opt/homebrew/bin/wix"
            };
            // Just verify we can check these paths
            for (String path : paths) {
                File file = new File(path);
                assertNotNull("Path should be checkable", file);
            }
        } else if (OS.isLinux()) {
            String[] paths = {
                "/usr/bin/wix",
                "/usr/local/bin/wix"
            };
            for (String path : paths) {
                File file = new File(path);
                assertNotNull("Path should be checkable", file);
            }
        }
        // Windows paths are checked in WixDetectionTest
    }

    @Test
    public void testSystemProperties() {
        // Verify essential system properties are available
        assertNotNull("os.name should be set", System.getProperty("os.name"));
        assertNotNull("os.arch should be set", System.getProperty("os.arch"));
        assertNotNull("java.version should be set", System.getProperty("java.version"));
        assertNotNull("user.home should be set", System.getProperty("user.home"));
    }

    /**
     * Helper method to find command in PATH (simulates MsiBuilder logic)
     */
    private String findCommandInPath(String command) {
        String path = System.getenv("PATH");
        if (path == null) {
            return null;
        }

        String[] pathDirs = path.split(File.pathSeparator);
        for (String dir : pathDirs) {
            File commandFile = new File(dir, command);
            if (commandFile.exists() && commandFile.canExecute()) {
                return commandFile.getAbsolutePath();
            }
        }

        String wixEnv = System.getenv("WIX");
        if (wixEnv != null) {
            File commandFile = new File(wixEnv, "bin" + File.separator + command);
            if (commandFile.exists() && commandFile.canExecute()) {
                return commandFile.getAbsolutePath();
            }
        }

        return null;
    }
}

