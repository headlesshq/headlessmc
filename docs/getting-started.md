# Getting Started

Download the executable for your Operating System from the latest 
[GitHub Release](https://github.com/headlesshq/headlessmc/releases).

=== "Linux"
    
    Get `headlessmc-launcher-linux-x64` or `headlessmc-launcher-linux-arm64`, 
    depending on if your CPU architecture is x64 or ARM64.
    Then you can run the file in your console.
    ``` sh
    chmod +x headlessmc-launcher-linux-x64
    ./headlessmc-launcher-linux-x64
    ```
    You can download the file e.g. via curl, replace $VERSION with the HeadlessMc version to download:
    ``` sh
    curl -L https://github.com/headlesshq/headlessmc/releases/download/$VERSION/headlessmc-launcher-linux-x64 -o headlessmc-launcher
    ```

=== "Windows"

    Get `headlessmc-launcher-windows-x64.exe`. Then you can run the file in your console.
    ```lang-powershell
    .\headlessmc-launcher-windows-x64.exe
    ```
    You can download the file e.g. via curl.exe in the Command prompt, replace $VERSION with the HeadlessMc version to download:
    ```lang-powershell
    curl.exe -L --output headlessmc-launcher.exe --url https://github.com/headlesshq/headlessmc/releases/download/$VERSION/headlessmc-launcher-windows-x64.exe
    ```

=== "MacOS"

    Get `headlessmc-launcher-macos-arm64` or `headlessmc-launcher-macos-x64`, 
    depending on if your CPU architecture is ARM64 or x64.
    Then you can run the file in your console.
    ``` sh
    chmod +x headlessmc-launcher-macos-arm64
    ./headlessmc-launcher-macos-arm64
    ```
    You can download the file e.g. via curl, replace $VERSION with the HeadlessMc version to download:
    ``` sh
    curl -L https://github.com/headlesshq/headlessmc/releases/download/$VERSION/headlessmc-launcher-macos-arm64 -o headlessmc-launcher
    ```

### Java

HeadlessMc has been written in Java with the Quarkus Framework and runs on Java 25.
You can also download the `headlessmc.jar` from GitHub, which will run on any operating system.
```shell
java -jar headlessmc.jar
```

### Docker

A preconfigured [docker image](https://hub.docker.com/r/3arthqu4ke/headlessmc/) exists:
```shell
docker pull 3arthqu4ke/headlessmc:latest
docker run -it 3arthqu4ke/headlessmc:latest
```
Inside the container you can use the `headlessmc`.

### Android

HeadlessMc can run inside Termux.

- Download Termux from F-Droid, **NOT** from the PlayStore.
- Install Java: `apt update && apt upgrade $ apt install openjdk-<version>`
- Download the headlessmc.jar into Termux.
- Disable JLine, as we could not get it to work on Termux for now,
  by adding `hmc.jline.enabled=false` to the user/.config/headlessmc/config.properties,
  or starting with `-Dhmc.jline.enabled=false`.
- Now you can use HeadlessMc as you would on Desktop or Docker.

### Web

!!! warning "HeadlessMc v2"

    This is a legacy HeadlessMc v2 feature that will eventually come back.
    Maybe even better as it could be possible to compile HeadlessMc to webassembly with GraalVM.
    However, it is currently not supported.

HeadlessMc can run inside the browser, kinda.
First, there is [CheerpJ](https://cheerpj.com/), a WebAssembly JVM,
but it does not support all features we need to launch the game.
The CheerpJ instance can be tried out [here](https://headlesshq.github.io/headlessmc/).
Secondly, there is [container2wasm](https://github.com/headlesshq/hmc-container2wasm),
which can translate the HeadlessMc Docker container
to WebAssembly and the run it inside the browser, but this is extremely slow.

