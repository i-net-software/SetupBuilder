/*
 * Copyright 2015 - 20016 i-net software
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

import java.io.File;
import java.io.IOException;

import org.w3c.dom.Element;

import com.inet.gradle.setup.SetupBuilder;
import com.inet.gradle.setup.abstracts.DesktopStarter;
import com.inet.gradle.setup.util.XmlFileBuilder;

/**
 * Create a XML configuration file for lauch4j.
 * 
 * @author Volker
 */
class Launch4jManifest extends XmlFileBuilder<Msi> {

    private Launch4j launch;

    /**
     * Create a instance.
     * 
     * @param launch the launch description
     * @param task current task
     * @param setup the SetupBuilder
     * @throws Exception if any error occur
     */
    Launch4jManifest( Launch4j launch, Msi task, SetupBuilder setup ) throws Exception {
        super( task, setup, File.createTempFile( "launch4j", ".manifest", task.getTemporaryDir() ), task.getTemporaryDir(), null );
        this.launch = launch;
    }

    /**
     * Create the XML file.
     * 
     * @throws IOException if an error occurs on reading the image files
     */
    void build() throws IOException {
        Element assembly = getOrCreateChild( doc, "assembly" );
        assembly.setAttributeNS( "http://www.w3.org/2000/xmlns/", "xmlns", "urn:schemas-microsoft-com:asm.v1" );
        addAttributeIfNotExists( assembly, "manifestVersion", "1.0" );

        Element identity = getOrCreateChild( assembly, "assemblyIdentity" );
        addAttributeIfNotExists( identity, "version", Launch4jConfig.normalizeVersionNumber( setup.getVersion() ) );
        addAttributeIfNotExists( identity, "processorArchitecture", "X86" );
        addAttributeIfNotExists( identity, "name", launch.getDisplayName() );
        addAttributeIfNotExists( identity, "type", "win32" );

        Element compatibility = getOrCreateChild( assembly, "compatibility" );
        compatibility.setAttributeNS( "http://www.w3.org/2000/xmlns/", "xmlns", "urn:schemas-microsoft-com:compatibility.v1" );

        Element application = getOrCreateChild( compatibility, "application" );
        getOrCreateChildById( application, "supportedOS", "{e2011457-1546-43c5-a5fe-008deee3d3f0}" ); // support for Vista
        getOrCreateChildById( application, "supportedOS", "{35138b9a-5d96-4fbd-8e2d-a2440225f93a}" ); // support for Windows 7
        getOrCreateChildById( application, "supportedOS", "{4a2f28e3-53b9-4441-ba9c-d69d4a4a6e38}" ); // support for Windows 8
        getOrCreateChildById( application, "supportedOS", "{1f676c76-80e1-4239-95bb-83d0f6d0da78}" ); // support for Windows 8.1
        getOrCreateChildById( application, "supportedOS", "{8e0f7a12-bfb3-4fe8-b9a5-48fd50a15a9a}" ); // support for Windows 10

        getOrCreateChild( assembly, "description" ).setTextContent( launch.getDescription() );

        Element trustInfo = getOrCreateChild( assembly, "trustInfo" );
        trustInfo.setAttributeNS( "http://www.w3.org/2000/xmlns/", "xmlns", "urn:schemas-microsoft-com:asm.v2" );

        Element security = getOrCreateChild( trustInfo, "security" );
        Element requestedPrivileges = getOrCreateChild( security, "requestedPrivileges" );
        Element requestedExecutionLevel = getOrCreateChildByKeyValue( requestedPrivileges, "requestedExecutionLevel", "level", launch.getRequestedExecutionLevel() );
        addAttributeIfNotExists( requestedExecutionLevel, "uiAccess", "false" );
    }
}
