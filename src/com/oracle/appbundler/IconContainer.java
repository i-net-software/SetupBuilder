/*
 * Copyright 2015, Quality First Software GmbH and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package com.oracle.appbundler;

import java.io.File;

/**
 * Interface for classes that provide icon information.
 */
public interface IconContainer {
    /**
     * Checks if an icon is set.
     * @return true if an icon is set
     */
    public boolean hasIcon();
    
    /**
     * Gets the icon path.
     * @return the icon path
     */
    public String getIcon();
    
    /**
     * Gets the icon file.
     * @return the icon file, or null if not available
     */
    public File getIconFile();
}
