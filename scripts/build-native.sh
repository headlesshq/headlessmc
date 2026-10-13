#!/bin/sh

set -e

# quarkusBuild instead of build: tests already run in the build job, and their JVMs
# compete with native-image for memory on small runners. Extra args are passed to Gradle.
# With SEPARATE_NATIVE_BUILD=true Gradle only generates the native-image inputs and is stopped
# before native-image runs, so the Gradle and Quarkus JVMs do not take memory away from it.
if [ "$SEPARATE_NATIVE_BUILD" = "true" ]; then
  set -- -Dquarkus.native.sources-only=true "$@"
fi

./gradlew :headlessmc-application:quarkusBuild -Dquarkus.native.enabled=true -Dquarkus.package.jar.enabled=false \
-Dquarkus.native.additional-build-args="--initialize-at-run-time=org.jline.nativ,--initialize-at-run-time=org.jline.terminal.impl.ffm,-H:+UnlockExperimentalVMOptions,-H:-ParseRuntimeOptions,-H:+SharedArenaSupport,-H:-UnlockExperimentalVMOptions" "$@"

if [ "$SEPARATE_NATIVE_BUILD" = "true" ]; then
  ./gradlew --stop
  cd headlessmc-application/build/native-sources
  # word splitting is intended, the args file is space separated
  # shellcheck disable=SC2046
  native-image $(cat native-image.args)
  mv headlessmc-application-*-runner ..
fi
