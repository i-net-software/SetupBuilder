/*
 * Copyright 2015 i-net software
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
package com.inet.gradle.setup.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Some Utils for working with resources.
 */
public class ResourceUtils {

    /**
     * Extract a resource file and save it as file.
     * 
     * @param clazz the class name
     * @param name the relative resource file
     * @param dir the directory
     * @return the new file
     * @throws IOException if any error occur
     */
    public static File extract( Class<?> clazz, String name, File dir ) throws IOException {
        File file = new File( dir, name );
        file.getParentFile().mkdirs();
        try( InputStream input = clazz.getResourceAsStream( name ) ) {
            Files.copy( input, file.toPath(), StandardCopyOption.REPLACE_EXISTING );
        }
        return file;
    }
}
