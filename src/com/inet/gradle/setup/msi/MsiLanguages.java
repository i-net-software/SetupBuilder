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
package com.inet.gradle.setup.msi;

/**
 * A mapping between Windows cultures and languages IDs
 * 
 * @author Volker Berlin
 */
enum MsiLanguages {
    zh_tw( 1028 ), //
    de_de( 1031 ), //
    en_us( 1033 ), //
    fr_fr( 1036 ), //
    ja_jp( 1041 ), //
    zh_cn( 2052 ), //
    es_es( 3082 ), //
    ;

    private final String culture;

    private final int    langID;

    MsiLanguages( int langID ) {
        this.culture = name().replace( '_', '-' );
        this.langID = langID;
    }

    MsiLanguages( String culture, int langID ) {
        this.culture = culture;
        this.langID = langID;
    }

    String getCulture() {
        return culture;
    }

    String getLangID() {
        return Integer.toString( langID );
    }
}
