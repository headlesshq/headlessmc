#!/bin/sh

set -e

./gradlew :headlessmc-application:build -Dquarkus.native.enabled=true -Dquarkus.package.jar.enabled=false \
-Dquarkus.native.additional-build-args="--initialize-at-run-time=org.jline.nativ,--initialize-at-run-time=org.jline.terminal.impl.ffm,-H:+UnlockExperimentalVMOptions,-H:-ParseRuntimeOptions,-H:+SharedArenaSupport,-H:-UnlockExperimentalVMOptions"
