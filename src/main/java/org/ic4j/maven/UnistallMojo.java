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

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.ic4j.agent.AgentError;
import org.ic4j.management.CanisterStatusResponse;
import org.ic4j.management.ManagementService;
import org.ic4j.types.Principal;

/**
 * Uninstalls a specific canister . 
 *
 */

@Mojo(name = "uninstall", defaultPhase = LifecyclePhase.CLEAN)
public class UnistallMojo extends AbstractIC4JMojo {
	@Parameter(property = "canister")
	List<Canister> canisters;

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {

			this.getLog().info("ICP location " + this.network);

			if (this.canisters == null || this.canisters.isEmpty())
				throw new MojoExecutionException("Undefined canister deployments");

			ManagementService managementService = this.getManagementService();

			for (Canister deploymentCanister : this.canisters) {
				String canisterId = deploymentCanister.canisterId;

				if (canisterId == null)
					continue;

				Principal canister = Principal.fromString(canisterId);

				managementService.stopCanister(canister);

				CanisterStatusResponse canisterStatusResponse = managementService.canisterStatus(canister).get();

				this.getLog().info("Canister " + canister.toString() + " is " + canisterStatusResponse.status.name());

				managementService.uninstallCode(canister);

				if (deploymentCanister.delete) {
					managementService.deleteCanister(canister);
					this.getLog().info("Canister " + canister.toString() + " deleted");
				}
			}
		} catch (AgentError e) {
			this.getLog().error(e);
			throw new MojoExecutionException(e.getLocalizedMessage());
		} catch (Exception e) {
			this.getLog().error(e);
			throw new MojoExecutionException(e.getLocalizedMessage());
		}

	}

}
