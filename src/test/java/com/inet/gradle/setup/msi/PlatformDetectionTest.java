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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.gradle.internal.os.OperatingSystem;
import org.junit.Test;

/**
 * Tests for Phase 1: Platform Detection & Path Handling
 */
public class PlatformDetectionTest {

    private static final OperatingSystem OS = OperatingSystem.current();

    @Test
    public void testPlatformDetection() {
        // At least one platform should be detected
        assertTrue("Platform should be detected",
            OS.isWindows() || OS.isLinux() || OS.isMacOsX());
    }

    @Test
    public void testFileSeparator() {
        String separator = File.separator;
        assertNotNull("File separator should not be null", separator);
        assertEquals("File separator should be single character", 1, separator.length());
        
        // Should be either / or \ depending on platform
        assertTrue("File separator should be / or \\",
            separator.equals("/") || separator.equals("\\"));
        
        // Unix-like systems should use /
        if (OS.isLinux() || OS.isMacOsX()) {
            assertEquals("Unix systems should use /", "/", separator);
        }
    }

    @Test
    public void testPathSeparator() {
        String pathSeparator = File.pathSeparator;
        assertNotNull("Path separator should not be null", pathSeparator);
        assertTrue("Path separator should not be empty", pathSeparator.length() > 0);
        
        // Unix-like systems should use :
        if (OS.isLinux() || OS.isMacOsX()) {
            assertEquals("Unix systems should use :", ":", pathSeparator);
        }
        // Windows should use ;
        else if (OS.isWindows()) {
            assertEquals("Windows should use ;", ";", pathSeparator);
        }
    }

    @Test
    public void testOperatingSystemInstance() {
        assertNotNull("OperatingSystem instance should not be null", OS);
    }

    @Test
    public void testPlatformSpecificChecks() {
        // Only one platform should be true at a time
        int platformCount = 0;
        if (OS.isWindows()) platformCount++;
        if (OS.isLinux()) platformCount++;
        if (OS.isMacOsX()) platformCount++;
        
        assertEquals("Exactly one platform should be detected", 1, platformCount);
    }
}

