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

import java.util.ArrayList;

/**
 * Base task for all setup builder tasks.
 * 
 * @author Volker Berlin
 */
public abstract class AbstractSetupTask extends AbstractTask {

    private ArrayList<String> postinst = new ArrayList<String>();
	private ArrayList<String> preinst = new ArrayList<String>();
	private ArrayList<String> postrm = new ArrayList<String>();
	private ArrayList<String> prerm = new ArrayList<String>();

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


	/**
	 * Returns the preinst that should be used in the 'preinst' config file.
	 * @return the preinst specified in the gradle script
	 */
	public ArrayList<String> getPreinst() {
		return preinst;
	}

	/**
	 * Sets the value for the 'preinst' config file.
	 * @param preinst the value for the entry
	 */
	public void setPreinst(String preinst) {
		this.preinst.add( preinst );
	}


	/**
	 * Returns the postinst that should be used in the 'postinst' config file.
	 * @return the postinst specified in the gradle script
	 */
	public ArrayList<String> getPostinst() {
		return postinst;
	}
	
	/**
	 * Sets the value for the 'postinst' config file.
	 * @param postinst the value for the entry
	 */
	public void setPostinst(String postinst) {
		this.postinst.add( postinst );
	}


	/**
	 * Returns the prerm that should be used in the 'prerm' config file.
	 * @return the prerm specified in the gradle script
	 */
	public ArrayList<String> getPrerm() {
		return prerm;
	}
	
	/**
	 * Sets the value for the 'prerm' config file.
	 * @param prerm the value for the entry
	 */
	public void setPrerm(String prerm) {
		this.prerm.add( prerm );
	}
	
	/**
	 * Returns the postrm that should be used in the 'postrm' config file.
	 * @return the postrm specified in the gradle script
	 */
	public ArrayList<String> getPostrm() {
		return postrm;
	}
	
	/**
	 * Sets the value for the 'postrm' config file.
	 * @param postrm the value for the entry
	 */
	public void setPostrm(String postrm) {
		this.postrm.add( postrm );
	}
}
