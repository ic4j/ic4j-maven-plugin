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

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.ic4j.agent.Agent;
import org.ic4j.agent.AgentBuilder;
import org.ic4j.agent.AgentError;
import org.ic4j.agent.ReplicaTransport;
import org.ic4j.agent.http.ReplicaOkHttpTransport;
import org.ic4j.agent.identity.Identity;
import org.ic4j.agent.identity.Secp256k1Identity;
import org.ic4j.management.CanisterStatusResponse;
import org.ic4j.management.ManagementService;
import org.ic4j.management.Mode;
import org.ic4j.types.Principal;

public abstract class AbstractIC4JMojo extends AbstractMojo {
	
	static final Principal effectiveCanister = Principal.fromString("x5pps-pqaaa-aaaab-qadbq-cai");
	static final String DEFAULT_NETWORK = "http://localhost:4943/";
	
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;

    /**
     * The user identity file to run this task as.
     * 
     */
	@Parameter(property = "identity")
	String identity;	

    /**
     * Override the compute network to connect to. By default, the local network is used. A
     * valid URL (starting with `http:` or `https:`) can be used here, and a special ephemeral
     * network will be created specifically for this request. E.g. "http://localhost:12345/" is
     * a valid network name
     * 
     */	
	@Parameter(property = "network")
	String network = DEFAULT_NETWORK;
	
	
	public ManagementService getManagementService() throws MojoExecutionException, MojoFailureException {
		try {
			Security.addProvider(new BouncyCastleProvider());
			
			Reader sourceReader = new FileReader(this.identity);
			
			Identity identity = Secp256k1Identity.fromPEMFile(sourceReader);
	
	
			ReplicaTransport transport = ReplicaOkHttpTransport.create(network);
			Agent agent = new AgentBuilder().transport(transport)
					.identity(identity)
					.build();
			agent.fetchRootKey();

			ManagementService managementService = ManagementService.create(agent, Principal.managementCanister(),effectiveCanister);
			
			return managementService;
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
