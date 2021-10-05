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

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Contains information for a SignTool
 *
 * @author Volker Berlin
 *
 */
class SignTool implements Serializable {

    private Object       certificate;

    private String       password, sha1;

    private List<String> timestamps = Arrays.asList( //
                                                    "http://time.certum.pl", //
                                                    //"http://timestamp.verisign.com/scripts/timstamp.dll", // no connect possible
                                                    "http://timestamp.globalsign.com/scripts/timestamp.dll", //
                                                    "http://tsa.starfieldtech.com", //
                                                    "http://timestamp.comodoca.com/authenticode" // certificate is expired
                                                    );

    /**
     * Get a certificate file.
     * 
     * @return a file or null
     */
    public Object getCertificate() {
        return certificate;
    }

    /**
     * Set a certificate file. This is used for the /f option of signtool.
     * 
     * @param certificate the file
     */
    public void setCertificate( Object certificate ) {
        this.certificate = certificate;
    }

    /**
     * Get the password for a certificate file.
     * 
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the password for a certificate file. This is used for the /p option of signtool.
     * 
     * @param password the password
     */
    public void setPassword( String password ) {
        this.password = password;
    }

    /**
     * Get a SHA1 thumbprint of the signing cert.
     * 
     * @return the thumbprint
     */
    public String getSha1() {
        return sha1;
    }

    /**
     * Set the SHA1 thumbprint of the signing cert. This is used for the /sha1 option of signtool.
     * 
     * @param sha1 the thumbprint
     */
    public void setSha1( String sha1 ) {
        this.sha1 = sha1;
    }

    /**
     * Get a list of timestamp servers.
     * 
     * @return the list or null
     */
    public List<String> getTimestamp() {
        return timestamps;
    }

    /**
     * Set a list of timestamp servers. By default there is already a list of popular timestamp servers. With null you
     * can disable the timestamp signing. If one server is failing then the next server is try.
     * 
     * @param timestamps new timestamp servers.
     */
    public void setTimestamp( List<String> timestamps ) {
        this.timestamps = timestamps;
    }

    /**
     * Set a single timestamp server. With null you can disable the timestamp signing.
     * 
     * @param timestamp a single timestamp server
     */
    public void setTimestamp( String timestamp ) {
        this.timestamps = timestamp == null ? null : Arrays.asList( timestamp );
    }
}
