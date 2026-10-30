#!/bin/sh

set -e

if [ "$SEPARATE_NATIVE_BUILD" = "true" ]; then
  set -- -Dquarkus.native.sources-only=true "$@"
fi

# the native-image arguments are configured in headlessmc-application/src/main/resources/application.properties
./gradlew :headlessmc-application:quarkusBuild -Dquarkus.native.enabled=true -Dquarkus.package.jar.enabled=false "$@"

if [ "$SEPARATE_NATIVE_BUILD" = "true" ]; then
  ./gradlew --stop
  cd headlessmc-application/build/native-sources
  # word splitting is intended, the args file is space separated
  # shellcheck disable=SC2046
  native-image $(cat native-image.args)
  mv headlessmc-application-*-runner ..
fi
