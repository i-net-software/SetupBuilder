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
package com.inet.gradle.setup;

/**
 * Base task for all setup builder tasks.
 * 
 * @author Volker Berlin
 */
public abstract class AbstractSetupTask extends AbstractTask {

    /**
     * Constructor with indication to artifact result Runs with the default SetupBuilder for dmg, msi ...
     * 
     * @param extension of the setup
     */
    public AbstractSetupTask( String extension ) {
        super( extension, SetupBuilder.class );
    }

    /**
     * Get the setup builder extension.
     * @return the instance of the SetupBuilder
     */
    public SetupBuilder getSetupBuilder() {
        return (SetupBuilder)super.getAbstractSetupBuilder();
    }
}
