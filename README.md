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

