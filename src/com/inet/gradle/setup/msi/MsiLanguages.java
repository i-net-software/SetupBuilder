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
    // Available Language Packs for Windows: https://technet.microsoft.com/en-us/library/hh825678.aspx
    // from WixUIExtension supported languages: https://github.com/wixtoolset/wix3/tree/develop/src/ext/UIExtension/wixlib

    en_us( 1033 ), // this should be the default, that it is on position 0

    ar_sa( 1025 ), //
    bg_bg( 1026 ), //
    ca_es( 1027 ), //
    zh_tw( 1028 ), //
    cs_cz( 1029 ), //
    da_dk( 1030 ), //
    de_de( 1031 ), //
    el_gr( 1032 ), //
    fi_fi( 1035 ), //
    fr_fr( 1036 ), //
    he_IL( 1037 ), //
    hu_hu( 1038 ), //
    it_it( 1040 ), //
    ja_jp( 1041 ), //
    ko_kr( 1042 ), //
    nl_nl( 1043 ), //
    nb_no( 1044 ), //
    pl_pl( 1045 ), //
    pt_br( 1046 ), //
    ro_ro( 1048 ), //
    ru_ru( 1049 ), //
    hr_hr( 1050 ), //
    sk_sk( 1051 ), //
    sv_se( 1053 ), //
    th_th( 1054 ), //
    tr_tr( 1055 ), //
    uk_ua( 1058 ), //
    sl_si( 1060 ), //
    et_ee( 1061 ), //
    lv_lv( 1062 ), //
    lt_lt( 1063 ), //
//    hi_in( 1081 ), // error LGHT0311 : A string was provided with characters that are not available in the specified database code page '1252'. Either change these characters to ones that exist in the database's code page, or update the database's code page by modifying one of the following attributes: Product/@Codepage, Module/@Codepage, Patch/@Codepage, PatchCreation/@Codepage, or WixLocalization/@Codepage.
//    kk_kz( 1087 ), // error LGHT0311 : A string was provided with characters that are not available in the specified database code page '1252'. Either change these characters to ones that exist in the database's code page, or update the database's code page by modifying one of the following attributes: Product/@Codepage, Module/@Codepage, Patch/@Codepage, PatchCreation/@Codepage, or WixLocalization/@Codepage.
    zh_cn( 2052 ), //
    pt_pt( 2070 ), //
    sr_latn_cs( 2074 ), //
    zh_hk( 3076 ), //
    es_es( 3082 ), //
    ;

    private final String culture;

    private final int    langID;

    /**
     * Create a instance of the enum.
     * @param langID the numeric language ID.
     */
    private MsiLanguages( int langID ) {
        this.culture = name().replace( '_', '-' );
        this.langID = langID;
    }

    /**
     * The culture like en-us.
     * @return the culture
     */
    String getCulture() {
        return culture;
    }

    /**
     * The numeric language ID.
     * @return the id
     */
    String getLangID() {
        return Integer.toString( langID );
    }

    public static MsiLanguages getMsiLanguage( String input ) {

        String key = input.replace( '-', '_' ).toLowerCase();
        MsiLanguages value = null;
        try {
            value = MsiLanguages.valueOf( key );
        } catch( IllegalArgumentException ex ) {
            // The complete name was not found.
            // now we check if this is only a language without a country
            for( MsiLanguages msiLanguage : MsiLanguages.values() ) {
                if( msiLanguage.toString().startsWith( key ) ) {
                    value = msiLanguage;
                    break;
                }
            }
            if( value == null ) {
                throw ex; // not supported language
            }
        }
        return value;
    }
}
