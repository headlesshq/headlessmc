@echo off

setlocal

call gradlew.bat :headlessmc-application:build -Dquarkus.native.enabled=true -Dquarkus.package.jar.enabled=false -Dquarkus.native.container-build=true -Dquarkus.native.container-runtime=docker ^
-Dquarkus.native.additional-build-args="--initialize-at-run-time=org.jline.nativ,--initialize-at-run-time=org.jline.terminal.impl.ffm,-H:+UnlockExperimentalVMOptions,-H:-ParseRuntimeOptions,-H:+SharedArenaSupport,-H:-UnlockExperimentalVMOptions"

endlocal
