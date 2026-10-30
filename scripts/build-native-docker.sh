#!/bin/sh

set -e

# the native-image arguments are configured in headlessmc-application/src/main/resources/application.properties
./gradlew :headlessmc-application:build -Dquarkus.native.enabled=true -Dquarkus.package.jar.enabled=false -Dquarkus.native.container-build=true -Dquarkus.native.container-runtime=docker
