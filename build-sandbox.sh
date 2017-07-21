#!/bin/bash
git pull
mvn -T2.0C -Drat.numUnapprovedLicenses=100 -Dmaven.test.skip=true -DskipTests=true  clean install
scp -P 2222 gateway-service-pontus/target/gateway-service-pontus-0.9.0.jar gateway-provider-security-pontus-jwt/target/gateway-provider-security-pontus-jwt-0.9.0.jar localhost:/opt/pontus
