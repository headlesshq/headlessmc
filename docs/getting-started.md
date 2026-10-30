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

### Shell completions

HeadlessMc can complete its commands, options, installed versions, accounts, profiles and more
in bash, zsh, fish and PowerShell, with a description for every completion.
`headlessmc completion <shell>` prints a small script for your shell,
which asks the `headlessmc` executable on your `PATH` for completions every time you press tab.
Use the native executable for this, the `headlessmc.jar` takes too long to start.
If you named the executable differently, e.g. `headlessmc-launcher-linux-x64`,
rename it to `headlessmc` (or `hmc`), as the scripts only complete those names.

=== "zsh"

    Add this to your `~/.zshrc`, after `compinit` (frameworks like Oh My Zsh run `compinit` for you):
    ```shell
    source <(headlessmc completion zsh)
    ```
    Alternatively, save the script once into a directory of your `fpath`,
    so that HeadlessMc does not have to run whenever a shell starts:
    ```shell
    mkdir -p ~/.zsh/completions
    headlessmc completion zsh > ~/.zsh/completions/_headlessmc
    ```
    and add `fpath=(~/.zsh/completions $fpath)` before `compinit` runs.
    If your system runs `compinit` before your `~/.zshrc`, like Ubuntu does in `/etc/zsh/zshrc`,
    add it to your `~/.zshenv` instead.
    You might have to remove the completion cache with `rm ~/.zcompdump*` once.
    Remember to save the script again after updating HeadlessMc.

    Options are shown before you have typed a `-`, to only show them after a `-`:
    ```shell
    zstyle ':completion:*:*:headlessmc:*:options' prefix-needed true
    ```

=== "bash"

    Add this to your `~/.bashrc`:
    ```shell
    source <(headlessmc completion bash)
    ```
    The script works best with the
    [bash-completion](https://github.com/scop/bash-completion) package installed,
    which most Linux distributions do by default.
    Bash cannot show descriptions next to completions,
    so if there are multiple completions they are listed as `name  (description)`.

=== "fish"

    Save the script into your completions directory, fish loads it automatically:
    ```shell
    headlessmc completion fish > ~/.config/fish/completions/headlessmc.fish
    ```
    Remember to save the script again after updating HeadlessMc.

=== "PowerShell"

    Add this to your PowerShell profile, which you can open with `notepad $PROFILE`:
    ```powershell
    headlessmc completion powershell | Out-String | Invoke-Expression
    ```
    Pressing tab cycles through the completions,
    to see all completions with their descriptions use `Ctrl+Space`, or enable a menu for tab:
    ```powershell
    Set-PSReadLineKeyHandler -Key Tab -Function MenuComplete
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

