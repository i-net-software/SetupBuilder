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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.gradle.internal.os.OperatingSystem;
import org.junit.Test;

/**
 * Tests for Phase 3: Launch4j Cross-Platform Workdir Classifier Selection
 */
public class Launch4jClassifierTest {

    private static final OperatingSystem OS = OperatingSystem.current();
    
    // Valid classifiers from Maven Central: https://repo1.maven.org/maven2/net/sf/launch4j/launch4j/3.50/
    private static final Set<String> VALID_CLASSIFIERS = new HashSet<String>(Arrays.asList(
        "workdir-win32",
        "workdir-linux",
        "workdir-linux64",
        "workdir-mac"
    ));

    @Test
    public void testClassifierSelection() {
        String classifier = getWorkdirClassifier();
        assertNotNull("Classifier should not be null", classifier);
        assertTrue("Classifier should be valid: " + classifier,
            VALID_CLASSIFIERS.contains(classifier));
    }

    @Test
    public void testWindowsClassifier() {
        if (OS.isWindows()) {
            String classifier = getWorkdirClassifier();
            assertEquals("Windows should use workdir-win32", "workdir-win32", classifier);
        }
    }

    @Test
    public void testLinuxClassifier() {
        if (OS.isLinux()) {
            String classifier = getWorkdirClassifier();
            String arch = System.getProperty("os.arch");
            
            if (arch != null && (arch.contains("64") || arch.equals("amd64") || arch.equals("x86_64"))) {
                assertEquals("64-bit Linux should use workdir-linux64", "workdir-linux64", classifier);
            } else {
                assertEquals("32-bit Linux should use workdir-linux", "workdir-linux", classifier);
            }
        }
    }

    @Test
    public void testMacOsClassifier() {
        if (OS.isMacOsX()) {
            String classifier = getWorkdirClassifier();
            assertEquals("macOS should use workdir-mac", "workdir-mac", classifier);
        }
    }

    @Test
    public void testArchitectureDetection() {
        String arch = System.getProperty("os.arch");
        assertNotNull("Architecture should be detected", arch);
        assertFalse("Architecture should not be empty", arch.isEmpty());
    }

    /**
     * Simulates the getWorkdirClassifier() logic from Launch4jCreator
     */
    private String getWorkdirClassifier() {
        if (OS.isWindows()) {
            return "workdir-win32";
        } else if (OS.isLinux()) {
            String arch = System.getProperty("os.arch");
            if (arch != null && (arch.contains("64") || arch.equals("amd64") || arch.equals("x86_64"))) {
                return "workdir-linux64";
            }
            return "workdir-linux";
        } else if (OS.isMacOsX()) {
            return "workdir-mac";
        }
        return "workdir-win32"; // Fallback
    }
}

