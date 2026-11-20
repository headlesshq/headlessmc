# Getting Started

## Installation

Select your platform and preferred installation method:

=== "Linux"

    === "Script"

        ``` sh
        curl -fsSL https://headlesshq.github.io/headlessmc/install.sh | sh
        ```
        
        This installs HeadlessMc to `~/.local/bin`, which you might have to add to your PATH.
        It also installs the [shell completions](#shell-completions) for bash, fish and zsh.
    
    === "Homebrew"
    
        You can install HeadlessMc through Homebrew by using our tap:
        
        ```shell
        brew tap headlesshq/tap
        brew trust headlesshq/tap
        brew install headlessmc
        ```
    
    === "APT"
    
        You can install HeadlessMc through APT by using our repository:
        
        ```shell
        sudo install -d -m 0755 /etc/apt/keyrings
        curl -fsSL https://headlesshq.github.io/apt/headlesshq.gpg | sudo tee /etc/apt/keyrings/headlesshq.gpg > /dev/null
        curl -fsSL https://headlesshq.github.io/apt/headlesshq.sources | sudo tee /etc/apt/sources.list.d/headlesshq.sources > /dev/null
        sudo apt update
        sudo apt install headlessmc
        ```

    === "Nix"

        Run it without installing, or install it into your profile:
        ``` sh
        nix run github:headlesshq/headlessmc
        nix profile install github:headlesshq/headlessmc
        ```
    
        You can also install without any experimental features:
        ``` sh
        nix-env -if https://github.com/headlesshq/headlessmc/archive/main.tar.gz
        ```

=== "MacOS"

    === "Script"

        ``` sh
        curl -fsSL https://headlesshq.github.io/headlessmc/install.sh | sh
        ```
        
        This installs HeadlessMc to `~/.local/bin`, which you might have to add to your PATH.
        It also installs the [shell completions](#shell-completions) for bash, fish and zsh.
    
    === "Homebrew"
    
        You can install HeadlessMc through Homebrew by using our tap:
        
        ```shell
        brew tap headlesshq/tap
        brew trust headlesshq/tap
        brew install headlessmc
        ```

=== "Windows"

    === "Script"

        Run in PowerShell:
        ```lang-powershell
        irm https://headlesshq.github.io/headlessmc/install.ps1 | iex
        ```
        This installs to `%LOCALAPPDATA%\headlessmc\bin` and adds it to your user PATH.

    === "Scoop"

        You can use our [Scoop](https://scoop.sh) bucket:
        ```powershell
        scoop bucket add headlesshq https://github.com/headlesshq/scoop-bucket
        scoop install headlesshq/headlessmc
        ```

=== "Java"

    HeadlessMc has been written in Java with the Quarkus Framework and runs on Java 25.
    You can also download the `headlessmc.jar` from GitHub, which will run on any operating system.
    ```shell
    java -jar headlessmc.jar
    ```

=== "Docker"

    A preconfigured [docker image](https://hub.docker.com/r/3arthqu4ke/headlessmc/) exists:
    ```shell
    docker pull 3arthqu4ke/headlessmc:latest
    docker run -it 3arthqu4ke/headlessmc:latest
    ```
    Inside the container you can use the `headlessmc`.

=== "Android"

    HeadlessMc can run inside Termux.

    - Download Termux from F-Droid, **NOT** from the PlayStore.
    - Install Java: `apt update && apt upgrade $ apt install openjdk-<version>`
    - Download the headlessmc.jar into Termux.
    - Disable JLine, as we could not get it to work on Termux for now,
      by adding `hmc.jline.enabled=false` to the user/.config/headlessmc/config.properties,
      or starting with `-Dhmc.jline.enabled=false`.
    - Now you can use HeadlessMc as you would on Desktop or Docker.

## Shell completions

You can start a HeadlessMc shell via by just running the `headlessmc` command without any subcommands.
This shell provides suggestions and tab-completions.
You can also install completions that run in your terminal.

If you installed one of the Linux packages, the completions for bash, zsh and fish
are already installed and work in every new shell, you do not have to set up anything:
bash needs the [bash-completion](https://github.com/scop/bash-completion) package
and zsh needs `compinit` to run, which most distributions and frameworks like Oh-My-Zsh do.

You may need to restart your terminal after installing HeadlessMc for completions to work.

Below are in-depth instructions for each type of shell:

=== "zsh"

    Add this to your `~/.zshrc`, after `compinit` (frameworks like Oh My Zsh run `compinit` for you):
    ```shell
    source <(headlessmc completion zsh)
    ```
    Alternatively, save the script once into a directory of your `fpath`,
    so that HeadlessMc does not have to run whenever a shell starts:
    ```shell
    headlessmc completion zsh -o ~/.zsh/completions/_headlessmc
    ```
    and add `fpath=(~/.zsh/completions $fpath)` before `compinit` runs.
    If your system runs `compinit` before your `~/.zshrc`, like Ubuntu does in `/etc/zsh/zshrc`,
    add it to your `~/.zshenv` instead.
    You might have to remove the completion cache with `rm ~/.zcompdump*` once.
    Running the command again overwrites the file, do so after updating HeadlessMc.

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
    With bash-completion you can also save the script once instead, it is then loaded when you first press tab:
    ```shell
    headlessmc completion bash -o ~/.local/share/bash-completion/completions/headlessmc
    ```
    Bash cannot show descriptions next to completions,
    so if there are multiple completions they are listed as `name  (description)`.

=== "fish"

    Save the script into your completions directory, fish loads it automatically:
    ```shell
    headlessmc completion fish -o ~/.config/fish/completions/headlessmc.fish
    ```
    Running the command again overwrites the file, do so after updating HeadlessMc.

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


&nbsp;  
&nbsp;  
&nbsp;  
Below are installation instructions for the platforms:
- Linux 
- MacOS
- Windows
- Java
- Docker
- Android

## Linux

### Linux - Script

``` sh
curl -fsSL https://headlesshq.github.io/headlessmc/install.sh | sh
```

This installs HeadlessMc to `~/.local/bin`, which you might have to add to your PATH.
It also installs the [shell completions](#shell-completions) for bash, fish and zsh.

### Linux - Homebrew

You can install HeadlessMc through Homebrew by using our tap:

```shell
brew tap headlesshq/tap
brew trust headlesshq/tap
brew install headlessmc
```

### Linux - APT

You can install HeadlessMc through APT by using our repository:

```shell
sudo install -d -m 0755 /etc/apt/keyrings
curl -fsSL https://headlesshq.github.io/apt/headlesshq.gpg | sudo tee /etc/apt/keyrings/headlesshq.gpg > /dev/null
curl -fsSL https://headlesshq.github.io/apt/headlesshq.sources | sudo tee /etc/apt/sources.list.d/headlesshq.sources > /dev/null
sudo apt update
sudo apt install headlessmc
```

### Linux - Nix

Run it without installing, or install it into your profile:
``` sh
nix run github:headlesshq/headlessmc
nix profile install github:headlesshq/headlessmc
```

You can also install without any experimental features:
``` sh
nix-env -if https://github.com/headlesshq/headlessmc/archive/main.tar.gz
```

## MacOS

### MacOS - Script

``` sh
curl -fsSL https://headlesshq.github.io/headlessmc/install.sh | sh
```

This installs HeadlessMc to `~/.local/bin`, which you might have to add to your PATH.
It also installs the [shell completions](#shell-completions) for bash, fish and zsh.
    
### MacOS - Homebrew
    
You can install HeadlessMc through Homebrew by using our tap:

```shell
brew tap headlesshq/tap
brew trust headlesshq/tap
brew install headlessmc
```

## Windows

### Windows - Script

Run in PowerShell:
```lang-powershell
irm https://headlesshq.github.io/headlessmc/install.ps1 | iex
```
This installs to `%LOCALAPPDATA%\headlessmc\bin` and adds it to your user PATH.

### Windows - Scoop

You can use our [Scoop](https://scoop.sh) bucket:
```powershell
scoop bucket add headlesshq https://github.com/headlesshq/scoop-bucket
scoop install headlesshq/headlessmc
```

## Java

HeadlessMc has been written in Java with the Quarkus Framework and runs on Java 25.
You can also download the `headlessmc.jar` from GitHub, which will run on any operating system.
```shell
java -jar headlessmc.jar
```

## Docker

A preconfigured [docker image](https://hub.docker.com/r/3arthqu4ke/headlessmc/) exists:
```shell
docker pull 3arthqu4ke/headlessmc:latest
docker run -it 3arthqu4ke/headlessmc:latest
```
Inside the container you can use the `headlessmc`.

## Android

HeadlessMc can run inside Termux.

- Download Termux from F-Droid, **NOT** from the PlayStore.
- Install Java: `apt update && apt upgrade $ apt install openjdk-<version>`
- Download the headlessmc.jar into Termux.
- Disable JLine, as we could not get it to work on Termux for now,
  by adding `hmc.jline.enabled=false` to the user/.config/headlessmc/config.properties,
  or starting with `-Dhmc.jline.enabled=false`.
- Now you can use HeadlessMc as you would on Desktop or Docker.

## musl, NixOS and older glibc

`headlessmc-launcher-linux-x64-musl` is a fully static executable, built against musl.
It does not need any library or dynamic loader of the system, so it runs on every x64 Linux, e.g.:

* musl based distributions, like Alpine Linux, Void Linux (musl) or Chimera Linux,
* NixOS and Guix, which have no `/lib64/ld-linux-x86-64.so.2` (Minecraft needs a Java of the system there, see below),
* distributions with a glibc older than 2.34, like RHEL 8, Debian 11 or Ubuntu 20.04.

### NixOS & Guix Java
To run Minecraft on NixOS and Guix, HeadlessMc does not support Java installations.
Either install a Java of your system, e.g. the `jdk` package of nixpkgs or `openjdk` of Guix.
HeadlessMc finds Java installed into the system profile or your user profile
(`/run/current-system/sw`, `/etc/profiles/per-user/$USER`, `~/.nix-profile`, `~/.guix-profile`, ...)
and the Java in `JAVA_HOME` or on the `PATH`, e.g. in `nix shell nixpkgs#jdk`, `nix-shell -p jdk`, a dev shell or with direnv.
Any other Java can be added via its Java home:
``` sh
headlessmc config set hmc.java.versions /path/to/jdk
```
Or enable [nix-ld](https://github.com/nix-community/nix-ld) with `programs.nix-ld.enable = true;` on NixOS,
which provides the loader, so that the downloaded Java runtimes start.

### gcompat for musl

On musl systems HeadlessMc downloads Java runtimes built against musl, e.g. Temurin for Alpine Linux.
Minecraft ships natives, e.g. of LWJGL, that are only built against glibc.
The headless client does not load the LWJGL natives, a Minecraft server needs none at all.
To run the client with a window, install [gcompat](https://git.adelielinux.org/adelie/gcompat),
a glibc compatibility layer for musl, e.g. with `apk add gcompat` on Alpine Linux.
HeadlessMc warns if it launches the client with a window on musl without a glibc loader.
The jemalloc bundled with LWJGL 3.3.1 and newer (Minecraft 1.19 and newer) crashes on load with gcompat,
so HeadlessMc launches the client with `-Dorg.lwjgl.system.allocator=system` on musl.

## Web

!!! warning "HeadlessMc v2"

    This is a legacy HeadlessMc v2 feature that will eventually come back.
    Maybe even better as it could be possible to compile HeadlessMc to webassembly with GraalVM.
    However, it is currently not supported.

HeadlessMc can run inside the browser, kinda.
First, there is [CheerpJ](https://cheerpj.com/), a WebAssembly JVM,
but it does not support all features we need to launch the game.
Secondly, there is [headlessmc-wasm](https://github.com/headlesshq/headlessmc-wasm),
which compiles HeadlessMc to WebAssembly using GraalVM.
In this mode, however, HeadlessMc has no way to launch the game.
Maybe this can be achieved via trufl.
Thirdly, there is [container2wasm](https://github.com/headlesshq/hmc-container2wasm),
which can translate the HeadlessMc Docker container
to WebAssembly and the run it inside the browser, but this is extremely slow.
Lastly, you can use [headlessmc-gui](https://github.com/headlesshq/headlessmc-gui)
to give HeadlessMc a GUI in your browser.
