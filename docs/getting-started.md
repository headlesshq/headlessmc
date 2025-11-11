# Getting Started

There are many ways to run the launcher.
Which one to choose depends on what you want to achieve and where you are running HeadlessMc.
If you simply want to use it to play the game,
the [native](#native) approach might be a good place to start.

### Native
No setup is required for the native HeadlessMc executables.
They will download a suitable version of Java and run HeadlessMc with it.

=== "Linux"
    
    Get `headlessmc-launcher-linux-x64` or `headlessmc-launcher-linux-arm64`, 
    depending on if your CPU architecture is x64 or ARM64.
    Then you can run the file in your console.
    ``` sh
    chmod +x headlessmc-launcher-linux-x64
    ./headlessmc-launcher-linux-x64
    ```
    You can download the file e.g. via curl:
    ``` sh
    curl -L https://github.com/headlesshq/headlessmc/releases/download/$VERSION/headlessmc-launcher-linux-x64 -o headlessmc-launcher
    ```

=== "Windows"

    Get `headlessmc-launcher-windows-x64.exe`. Then you can run the file in your console.
    ```lang-powershell
    .\headlessmc-launcher-windows-x64.exe
    ```
    You can download the file e.g. via curl.exe in the Command prompt:
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
    You can download the file e.g. via curl:
    ``` sh
    curl -L https://github.com/headlesshq/headlessmc/releases/download/$VERSION/headlessmc-launcher-macos-arm64 -o headlessmc-launcher
    ```

### Java

HeadlessMc has been written in Java and run on any version &geq; Java 8.
The `headlessmc-launcher-wrapper.jar` has slightly more overhead compared to the
normal launcher jar, but enables [plugins](plugins.md) and [in-memory launching](in-memory.md).

Simply run:
```shell
java -jar headlessmc-launcher.jar
```

### Docker

A preconfigured [docker image](https://hub.docker.com/r/3arthqu4ke/headlessmc/) exists,
which comes with Java 8, 17 and 21 installed.
```shell
docker pull 3arthqu4ke/headlessmc:latest
docker run -it 3arthqu4ke/headlessmc:latest
```
Inside the container you can use the `hmc` command anywhere,
or start the jar using `java -jar`.

### Android

HeadlessMc can run inside Termux.

- Download Termux from F-Droid, **NOT** from the PlayStore.
- Install Java: `apt update && apt upgrade $ apt install openjdk-<version>`
- Download the headlessmc-launcher-wrapper.jar into Termux.
- Disable JLine, as we could not get it to work on Termux for now,
  by adding `hmc.jline.enabled=false` to the HeadlessMC/config.properties.
- Now you can use HeadlessMc as you would on Desktop or Docker.

### Web

HeadlessMc can run inside the browser, kinda.
First, there is [CheerpJ](https://cheerpj.com/), a WebAssembly JVM,
but it does not support all features we need to launch the game.
The CheerpJ instance can be tried out [here](https://headlesshq.github.io/headlessmc/).
Secondly, there is [container2wasm](https://github.com/headlesshq/hmc-container2wasm),
which can translate the HeadlessMc Docker container
to WebAssembly and the run it inside the browser, but this is extremely slow.

