# Pontus Knox and Keycloak 


This is the project with the knox plugins required to talk to keycloak. 

The Apache license was chosen, as this is derived from an Apache project

# Here are instructions on how to set up the Pontus Knox provider and service:

## Create the symbolic links:
```
cd /usr/hdp/current/knox-server/lib
ln -s /opt/pontus/gateway-service-pontus-0.13.0.jar
ln -s /opt/pontus/gateway-provider-security-pontus-jwt-0.13.0.jar
ln -s /opt/pontus/pontus-redaction-common-0.0.1-SNAPSHOT.jar
```

## Create the service definition:
```
cd /usr/hdp/current/knox-server/data/services
mkdir -p pontus/0.0.1
cd /usr/hdp/current/knox-server/data/services/pontus/0.0.1

tee /usr/hdp/current/knox-server/data/services/pontus/0.0.1/service.xml <<'EOF'
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->
<service role="Pontus" name="pontus" version="0.0.1">
    <routes>
        <route path="/pontus/**"/>
    </routes>
    <dispatch classname="org.apache.hadoop.gateway.pontus.PontusDispatch" ha-classname="org.apache.hadoop.gateway.pontus.PontusHaDispatch"/>
</service>
EOF


tee /usr/hdp/current/knox-server/data/services/pontus/0.0.1/rewrite.xml <<'EOF'

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->
<rules>
    <rule dir="IN" name="Pontus/pontus/inbound" pattern="*://*:*/**/pontus">
        <rewrite template="{$serviceUrl[Pontus]}"/>
    </rule>
</rules>
EOF

```

## Create the topology configuration
``` 
cd /usr/hdp/current/knox-server/conf/topologies
tee /usr/hdp/current/knox-server/conf/topologies/nifi.xml <<'EOF'
<topology>
  <gateway>
      <provider>
          <role>authentication</role>
          <name>ShiroProvider</name>
          <enabled>false</enabled>
      </provider>

      <provider>
          <role>identity-assertion</role>
          <name>Default</name>
          <enabled>false</enabled>
      </provider>

      <provider>
          <role>authorization</role>
          <name>AclsAuthz</name>
          <enabled>false</enabled>
      </provider>
      <provider>
          <role>webappsec</role>
          <name>WebAppSec</name>
          <enabled>false</enabled>
      </provider>
      <provider>
          <role>hostmap</role>
          <name>static</name>
          <enabled>false</enabled>
      </provider>
      <provider>
          <role>pontus_jwt</role>
          <name>process</name>
          <enabled>true</enabled>
      </provider>
  </gateway>

  <service>
      <role>Pontus</role>
      <!-- url>https://localhost:22222/nash_query</url -->
      <url>http://localhost:22221/nash_query</url>
  </service>
</topology>
EOF

```

