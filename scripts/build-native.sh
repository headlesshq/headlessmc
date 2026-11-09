#!/bin/sh
# Builds the GraalVM native image of HeadlessMc.
#
# Environment variables:
#   SEPARATE_NATIVE_BUILD  if true, Gradle only generates the native sources and native-image runs afterwards
#   NATIVE_MUSL            if true, builds a fully static executable against musl instead of glibc,
#                          x64 Linux only, needs x86_64-linux-musl-gcc with zlib on the PATH

set -e

BUILD_ARGS="--initialize-at-run-time=org.jline.nativ,--initialize-at-run-time=org.jline.terminal.impl.ffm,-H:+UnlockExperimentalVMOptions,-H:-ParseRuntimeOptions,-H:+SharedArenaSupport,-H:-UnlockExperimentalVMOptions"
if [ "$NATIVE_MUSL" = "true" ]; then
  BUILD_ARGS="$BUILD_ARGS,--static,--libc=musl"
fi

if [ "$SEPARATE_NATIVE_BUILD" = "true" ]; then
  set -- -Dquarkus.native.sources-only=true "$@"
fi

./gradlew :headlessmc-application:quarkusBuild -Dquarkus.native.enabled=true -Dquarkus.package.jar.enabled=false \
-Dquarkus.native.additional-build-args="$BUILD_ARGS" "$@"

if [ "$SEPARATE_NATIVE_BUILD" = "true" ]; then
  ./gradlew --stop
  cd headlessmc-application/build/native-sources
  # word splitting is intended, the args file is space separated
  # shellcheck disable=SC2046
  native-image $(cat native-image.args)
  mv headlessmc-application-*-runner ..
fi
