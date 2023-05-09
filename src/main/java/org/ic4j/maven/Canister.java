/*
 * Copyright 2023 Exilor Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.ic4j.maven;

import java.math.BigInteger;

import org.apache.maven.plugins.annotations.Parameter;
import org.ic4j.candid.parser.IDLArgType;
import org.ic4j.management.Mode;

public class Canister {
    /**
     * Canister WASM file.
     * 
     */	
	@Parameter(property = "wasmFile")
	String wasmFile;	
    /**
     * Specifies the id of the canister you want to install or uninstall.  
     * If not specified in install task , new canister will be created.
     * 
     */		
	@Parameter(property = "canisterId")
	String canisterId;

    /**
     * Specifies if the canister is deleted in uninstall task.
     * 
     */		
	@Parameter(property = "delete")
	boolean delete = false;
    		
    /**
	* Force the type of deployment to be reinstall, which overwrites the module. In other
    * words, this erases all data in the canister. By default, upgrade will be chosen
    * automatically if the canister already exists, or install if it does not       
    * [possible values: reinstall]
    * 
    */    		  		
	@Parameter(property = "mode")	
	Mode mode = Mode.upgrade;
	
	/** 
	 *  Specifies the initial cycle balance to deposit into the newly created canister. 
	 *  The specified amount needs to take the canister create fee into account. This amount is
	 *  deducted from the wallet's cycle balance
	 *  
	 */
	@Parameter(property = "withCycles")
	BigInteger withCycles;
	
	/**
	 * Specifies the argument to pass to the method
	 * 
	 */	
	@Parameter(property = "argument")
	String argument;
	
	/**
	 * Specifies the data type for the argument when making the call using an argument
     *      
     * [possible values: idl, raw]
	 */
	@Parameter(property = "argumentType")
	IDLArgType argumentType = IDLArgType.idl;		

	/**
	 * @return the wasmFile
	 */
	public String getWasmFile() {
		return wasmFile;
	}

	/**
	 * @param wasmFile the wasmFile to set
	 */
	public void setWasmFile(String wasmFile) {
		this.wasmFile = wasmFile;
	}

	/**
	 * @return the canisterId
	 */
	public String getCanisterId() {
		return canisterId;
	}

	/**
	 * @param canisterId the canisterId to set
	 */
	public void setCanisterId(String canisterId) {
		this.canisterId = canisterId;
	}

	/**
	 * @return the delete
	 */
	public boolean isDelete() {
		return delete;
	}

	/**
	 * @param delete the delete to set
	 */
	public void setDelete(boolean delete) {
		this.delete = delete;
	}

	/**
	 * @return the mode
	 */
	public Mode getMode() {
		return mode;
	}

	/**
	 * @param mode the mode to set
	 */
	public void setMode(Mode mode) {
		this.mode = mode;
	}

	/**
	 * @return the withCycles
	 */
	public BigInteger getWithCycles() {
		return withCycles;
	}

	/**
	 * @param withCycles the withCycles to set
	 */
	public void setWithCycles(BigInteger withCycles) {
		this.withCycles = withCycles;
	}

	/**
	 * @return the argument
	 */
	public String getArgument() {
		return argument;
	}

	/**
	 * @param argument the argument to set
	 */
	public void setArgument(String argument) {
		this.argument = argument;
	}

	/**
	 * @return the argumentType
	 */
	public IDLArgType getArgumentType() {
		return argumentType;
	}

	/**
	 * @param argumentType the argumentType to set
	 */
	public void setArgumentType(IDLArgType argumentType) {
		this.argumentType = argumentType;
	}	

}
