# IC4J Maven Plugin


The IC4J Maven Plugin allows for the installation and uninstallation of ICP canisters from Apache Maven scripts.

To install or uninstall an ICP canister, add the install Maven plugin to your Maven script.

run 
```mvn install ```
to install

or ```mvn clean``` to uninstall

```<build>
    <plugins>
       <plugin>
          <groupId>org.ic4j</groupId>
          <artifactId>ic4j-maven-plugin</artifactId>
          <version>0.7.0</version>
          <executions>
                <execution>
                  <id>install</id>
                   <goals>
                         <goal>install</goal>
                    </goals>
		             <configuration>
		                  <network>http://localhost:4943/</network>
		                  <identity>identity.pem</identity>
		                  <canisters>
		                   <canister>
		                      <mode>install</mode>
		                      <wasmFile>hello.wasm</wasmFile>
		                    <argument>("from Maven")</argument>
		                    </canister>                   	
		                 </canisters>
		              </configuration>
                  </execution>
                  <execution>
                    <id>uninstall</id>
                      <goals>
                          <goal>uninstall</goal>
                      </goals>
		              <configuration>
		                  <network>http://localhost:4943/</network>
		                  <identity>identity.pem</identity>
		                  <canisters>
		                   <canister>
		                    <canisterId>wzp7w-lyaaa-aaaaa-aaara-cai</canisterId>
		                    <delete>false</delete>
		                   </canister>                  
		                    </canisters>
		                </configuration>
                    </execution>                    
             </executions>
         </plugin>
     </plugins>
</build>


## Local replica CI

The GitHub Actions build validates the actual Maven lifecycle integration. It
builds the pinned Candid, Agent and Management sources into a fresh Maven cache,
builds the plugin on Java 21, then runs an independent POM's `mvn install` and
`mvn clean` against a disposable DFX 0.32.0 loopback replica. The test checks the
installed Wasm with the `peek` query and confirms that uninstall leaves the
canister stopped with no Wasm installed. No mainnet calls, existing identities
or registry deployments are performed.

Set plugin parameter `effectiveCanisterId` to a canister on the target subnet
when overriding the management call route for an isolated replica. Existing
configurations that omit it retain the previous default. Use a disposable
canister while testing these goals: `uninstall` removes code, and only deletes
the canister itself when `delete` is explicitly true.

This plugin installs and uninstalls Wasm; Java generation from Candid is handled
by the separate IC4J Codegen project.
