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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.ic4j.agent.AgentError;
import org.ic4j.candid.parser.IDLArgs;
import org.ic4j.management.CanisterStatusResponse;
import org.ic4j.management.ManagementService;
import org.ic4j.management.Mode;
import org.ic4j.types.Principal;

/**
 * Installs a specific canister from the WASM file. 
 *
 */

@Mojo(name = "install", defaultPhase = LifecyclePhase.INSTALL)
public class InstallMojo extends AbstractIC4JMojo {
	@Parameter(property = "canister")
	List<Canister> canisters;

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {

			this.getLog().info("ICP network " + this.network);
			
			if(this.canisters == null || this.canisters.isEmpty())
				throw new MojoExecutionException("Undefined canister deployments"); 
			
			ManagementService managementService = this.getManagementService();
			
			for(Canister deployment : this.canisters)
			{	
				String canisterId = deployment.canisterId;
				Principal canister;
				if(canisterId == null)
				{
					Optional<BigInteger> amount = Optional.ofNullable(deployment.withCycles);
					canister = managementService.provisionalCreateCanisterWithCycles(Optional.empty(), amount).get();
	
					this.getLog().info("Created canister with ID " + canister.toString());
					
					deployment.mode = Mode.install;
				}
				else
					canister = Principal.fromString(canisterId);
				
				Path path = Paths.get(deployment.wasmFile);
				
				byte [] wasmModule = Files.readAllBytes(path);	
				
				byte[] args = ArrayUtils.EMPTY_BYTE_ARRAY;
				
				if(deployment.argument != null)
				{
					this.getLog().info("Argument " + deployment.argument);
					switch(deployment.argumentType)
					{
					case raw:
						args = deployment.argument.getBytes();
						break;
					default:
						IDLArgs idlArgs = IDLArgs.fromIDL(deployment.argument);
						args = idlArgs.toBytes();
						break;					
					}
				}
				
				managementService.installCode(canister, deployment.mode, wasmModule, args);
				
				managementService.startCanister(canister);
				
				CanisterStatusResponse canisterStatusResponse = managementService.canisterStatus(canister).get();
				
				this.getLog().info("Canister " + canister.toString() + " is " + canisterStatusResponse.status.name());
			}
		}
		catch (AgentError e) {
			this.getLog().error(e);
			throw new MojoExecutionException(e.getLocalizedMessage());
		}		
		catch (Exception e) {
			this.getLog().error(e);
			throw new MojoExecutionException(e.getLocalizedMessage());
		}		

	}

}
