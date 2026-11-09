# Getting Started

The easiest way to install HeadlessMc is our install script.
It downloads the executable for your Operating System from the latest
[GitHub Release](https://github.com/headlesshq/headlessmc/releases) and adds it to your PATH as `headlessmc`.

=== "Linux & MacOS"

    ``` sh
    curl -fsSL https://headlesshq.github.io/headlessmc/install.sh | sh
    ```
    This installs to `~/.local/bin`, which you might have to add to your PATH, the script tells you how.
    It also installs the [shell completions](#shell-completions) for bash and fish,
    and for zsh if it can, otherwise it tells you the line to add to your `~/.zshrc`.
    Set `HEADLESSMC_NO_COMPLETIONS=1` to skip them.

=== "Windows"

    Run in PowerShell:
    ```lang-powershell
    irm https://headlesshq.github.io/headlessmc/install.ps1 | iex
    ```
    This installs to `%LOCALAPPDATA%\headlessmc\bin` and adds it to your user PATH.

Afterwards, run `headlessmc` in a new terminal.
To install a specific version or into another directory, set the `HEADLESSMC_VERSION`
(e.g. `3.0.0`) or `HEADLESSMC_INSTALL_DIR` environment variables.

### Manual download

You can also download the executable for your Operating System from the latest 
[GitHub Release](https://github.com/headlesshq/headlessmc/releases) yourself.

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
    These executables need glibc 2.34 or newer.
    On other systems use `headlessmc-launcher-linux-x64-musl`, see [musl, NixOS and older glibc](#musl-nixos-and-older-glibc).

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

### Linux packages

Every release also contains deb, rpm and archlinux packages of the Linux executables for x64 and ARM64.
They install HeadlessMc as `headlessmc`, replace $VERSION with the HeadlessMc version to download:

=== "Debian/Ubuntu"

    ``` sh
    curl -LO https://github.com/headlesshq/headlessmc/releases/download/$VERSION/headlessmc_${VERSION}_amd64.deb
    sudo apt install ./headlessmc_${VERSION}_amd64.deb
    ```
    Use `arm64` instead of `amd64` on ARM64.

=== "Fedora/RHEL/openSUSE"

    ``` sh
    sudo dnf install https://github.com/headlesshq/headlessmc/releases/download/$VERSION/headlessmc-$VERSION.x86_64.rpm
    ```
    Use `aarch64` instead of `x86_64` on ARM64, and `zypper install` on openSUSE.

=== "Arch Linux"

    ``` sh
    curl -LO https://github.com/headlesshq/headlessmc/releases/download/$VERSION/headlessmc-$VERSION-x86_64.pkg.tar.zst
    sudo pacman -U headlessmc-$VERSION-x86_64.pkg.tar.zst
    ```
    Use `aarch64` instead of `x86_64` on ARM64.

The packages require glibc 2.34 or newer (e.g. Debian 12, Ubuntu 22.04, RHEL 9).
To update HeadlessMc, install the package of the new release.

### musl, NixOS and older glibc

`headlessmc-launcher-linux-x64-musl` is a fully static executable, built against musl.
It does not need any library or dynamic loader of the system, so it runs on every x64 Linux, e.g.

* musl based distributions, like Alpine Linux, Void Linux (musl) or Chimera Linux,
* NixOS and Guix, which have no `/lib64/ld-linux-x86-64.so.2` (Minecraft needs a Java of the system there, see below),
* distributions with a glibc older than 2.34, like RHEL 8, Debian 11 or Ubuntu 20.04.

The install script picks it automatically on these systems, set `HEADLESSMC_MUSL=1` to always install it.
The updater keeps using the musl executable once it is installed.
There is no musl executable for ARM64 yet, use the `headlessmc.jar` there.

On NixOS and Guix the launcher itself runs, but Minecraft needs a Java that runs there, too.
NixOS and Guix use glibc, so HeadlessMc would download glibc Java runtimes,
and those expect the loader at `/lib64/ld-linux-x86-64.so.2`, which does not exist there.
Either install a Java of your system, e.g. the `jdk` package of nixpkgs or `openjdk` of Guix.
HeadlessMc finds Java installed into the system profile or your user profile
(`/run/current-system/sw`, `/etc/profiles/per-user/$USER`, `~/.nix-profile`, `~/.guix-profile`, ...)
and the Java in `JAVA_HOME` or on the `PATH`, e.g. in `nix shell nixpkgs#jdk`, `nix-shell -p jdk`, a dev shell or with direnv.
Any other Java can be added via its Java home:
``` sh
config set hmc.java.versions /path/to/jdk
```
Or enable [nix-ld](https://github.com/nix-community/nix-ld) with `programs.nix-ld.enable = true;` on NixOS,
which provides the loader, so that the downloaded Java runtimes start.
HeadlessMc warns before it downloads a Java runtime on a system without a glibc loader.

On musl systems HeadlessMc downloads Java runtimes built against musl, e.g. Temurin for Alpine Linux.
Tested on Alpine Linux 3.22 with the musl executable and Temurin 17 and 21 downloaded by `java install`:

| What                                       | Result on musl                                                         |
|--------------------------------------------|------------------------------------------------------------------------|
| Vanilla server 1.21.1                      | works, needs no natives                                                |
| Headless client, Fabric 1.21.4 and 1.20.1  | works, joins a world with mc-runtime-test                              |
| JNA and oshi (hardware info)               | load, oshi only warns that udev is missing                             |
| text2speech (narrator)                     | fails without `flite`, like on glibc, Minecraft just disables it       |
| Client with a window, without gcompat      | fails, the LWJGL natives need `ld-linux-x86-64.so.2`                   |
| Client with a window, with gcompat         | the LWJGL natives load, the jemalloc of LWJGL 3.3.1+ crashes, see below |

Minecraft ships natives, e.g. of LWJGL, that are only built against glibc.
The headless client does not load the LWJGL natives, a Minecraft server needs none at all.
To run the client with a window, install [gcompat](https://git.adelielinux.org/adelie/gcompat),
a glibc compatibility layer for musl, e.g. with `apk add gcompat` on Alpine Linux.
HeadlessMc warns if it launches the client with a window on musl without a glibc loader.
The jemalloc bundled with LWJGL 3.3.1 and newer (Minecraft 1.19 and newer) crashes on load with gcompat,
so HeadlessMc launches the client with `-Dorg.lwjgl.system.allocator=system` on musl.
Rendering with a window on musl has not been verified yet.

### Shell completions

HeadlessMc can complete its commands, options, installed versions, accounts, profiles and more
in bash, zsh, fish and PowerShell, with a description for every completion.

If you installed one of the [Linux packages](#linux-packages), the completions for bash, zsh and fish
are already installed and work in every new shell, you do not have to set up anything below:
bash needs the [bash-completion](https://github.com/scop/bash-completion) package
(on Arch Linux install it with `sudo pacman -S bash-completion`),
and zsh needs `compinit` to run, which most distributions and frameworks like Oh My Zsh do.
If zsh does not find them, remove its completion cache once with `rm ~/.zcompdump*`.
The package manager updates the completions together with HeadlessMc.

The install script for Linux and macOS writes the completions for bash and fish into
`~/.local/share/bash-completion/completions` and `~/.config/fish/completions`.
zsh has no such directory for users by default, so the script only installs the zsh completion
if it can write to a `site-functions` directory of zsh, e.g. when run as root,
and otherwise prints the line to add to your `~/.zshrc` as described below.

`headlessmc completion <shell>` prints a small script for your shell, or writes it to a file with `-o <file>`,
which asks the `headlessmc` executable on your `PATH` for completions every time you press tab.
Use the native executable for this, the `headlessmc.jar` takes too long to start.
If you named the executable differently, e.g. `headlessmc-launcher-linux-x64`,
rename it to `headlessmc`, as the scripts only complete that name.

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

To uninstall the completions, remove the line from your shell configuration or delete the saved file again.

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

