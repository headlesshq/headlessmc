<!--suppress HtmlDeprecatedAttribute -->
<div align="right">
  <a href="https://github.com/xiaofanforfabric/headlessmc">中文</a> | English
</div>

<h1 align="center" style="font-weight: normal;"><b>HeadlessMc</b></h1>
<p align="center">A command line launcher for Minecraft Java Edition.</p>
<p align="center"><img src="headlessmc-web/page/logo.png" alt="logo" style="width:200px;"></p>
<p align="center"><a href="https://github.com/headlesshq/mc-runtime-test">Mc-Runtime-Test</a> | HMC | <a href="https://github.com/headlesshq/hmc-specifics">HMC-Specifics</a> | <a href="https://github.com/headlesshq/hmc-optimizations">HMC-Optimizations</a></p>

<div align="center">

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/6a86b3e62d3b47909de670b09737f8fd)](https://app.codacy.com/gh/headlesshq/headlessmc/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![GitHub All Releases](https://img.shields.io/github/downloads/headlesshq/HeadlessMc/total.svg)](https://github.com/headlesshq/HeadlessMc/releases)
![](https://github.com/headlesshq/HeadlessMc/actions/workflows/gradle-publish.yml/badge.svg)
![GitHub](https://img.shields.io/github/license/headlesshq/HeadlessMc)
[![Docker Image Size](https://img.shields.io/docker/image-size/3arthqu4ke/headlessmc)](https://hub.docker.com/r/3arthqu4ke/headlessmc/)
![Github last-commit](https://img.shields.io/github/last-commit/headlesshq/HeadlessMc)

</div>

> [!NOTE]
> We are currently working on HeadlessMc 3.0, which will revamp it completly (With Picocli etc.)!
> Progress can be tracked on the [v3](https://github.com/headlesshq/headlessmc/tree/v3) branch.

> [!WARNING]
> NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
> 
> HeadlessMc will not allow you to play without having bought Minecraft! 
> Accounts will always be validated.
> Offline accounts can only be used to run the game headlessly in CI/CD pipelines.

HeadlessMc (HMC) allows you to launch Minecraft Java Edition from the command line.
It can manage clients, servers and mods.
It can run the client in headless mode, without a Screen, controlled by the command line.
This e.g. can allow you to test the game in your CI/CD pipeline with [mc-runtime-test](https://github.com/headlesshq/mc-runtime-test).

> [!TIP]  
> Read our new, beautiful documentation [here](https://headlesshq.github.io/headlessmc).

## Quickstart

1. Download the `headlessmc-launcher.jar` from the releases tab and install a Java version &geq; 8.
    - If you want additional features such as plugins and launching the game inside the same JVM, use the `headlessmc-launcher-wrapper.jar` instead.
    - You do not need to install java if you download one of the GraalVM executables instead.
2. Run the launcher with `java -jar headlessmc-launcher.jar` in your terminal.
    - Or e.g. `./headlessmc-launcher-linux` if you use a GraalVM executable.
3. HeadlessMc will generally not allow you to start the game without an account. 
Login to your Minecraft account by executing the `login` command and follow the instructions.
4. Launch the game with `launch <modloader>:<version>`, e.g. `launch fabric:1.21.4 -lwjgl`.
The `lwjgl` flag will make the game run in headless mode.

Read [more](https://headlesshq.github.io/headlessmc/getting-started/).

### HeadlessMc-Specifics

The [hmc-specifics](https://github.com/headlesshq/hmc-specifics) are mods
that you can place inside your .minecraft/mods folder.
Together with HeadlessMc they allow you to control the game via the command line, e.g.
by sending chat messages and commands with `msg "<message>"`,
visualizing the menus displayed by Minecraft via `gui` and clicking through menus via `click`.

Read [more](https://headlesshq.github.io/headlessmc/specifics/).

### Docker 

A preconfigured [docker image](https://hub.docker.com/r/3arthqu4ke/headlessmc/) exists,
which comes with Java 8, 17 and 21 installed.
Pull it via `docker pull 3arthqu4ke/headlessmc:latest`
and run it with `docker run -it 3arthqu4ke/headlessmc:latest`.
Inside the container you can use the `hmc` command anywhere.

### Android

HeadlessMc can run inside Termux.
* Download Termux from F-Droid, NOT from the PlayStore.
* Install Java: `apt update && apt upgrade $ apt install openjdk-<version>`
* Download the headlessmc-launcher-wrapper.jar into Termux.
* Disable JLine, as we could not get it to work on Termux for now,
  by adding `hmc.jline.enabled=false` to the HeadlessMC/config.properties.
* Now you can use HeadlessMc like you would on Desktop or Docker.

### Web

HeadlessMc can run inside the browser, kinda.
First, there is CheerpJ, a WebAssembly JVM,
but it does not support all features we need to launch the game.
The CheerpJ instance can be tried out [here](https://headlesshq.github.io/headlessmc/).
Secondly, there is [container2wasm](https://github.com/headlesshq/hmc-container2wasm),
which can translate the HeadlessMc Docker container
to WebAssembly and the run it inside the browser, but this is extremely slow.

### Servers

HeadlessMc also has support for Minecraft servers.
It can install and run Paper, Fabric, Vanilla, Purpur, Forge and Neoforge servers.
Instrumentation for servers is currently not supported.
Use the following commands:
```
> server add paper 1.21.5
Added paper server: paper-1.21.5-54.

> server list
id   type    version   name
0    paper   1.21.5    paper-1.21.5-54

> server eula paper-1.21.5-54 -accept
...

> server launch paper-1.21.5-54 --jvm "-Xmx10G -XX:+UseG1GC <...>"
...
```

### Testing

One primary goal of HeadlessMc is to enable testing for both production servers and clients.
For this purpose it is used in the [mc-runtime-test](https://github.com/headlesshq/mc-runtime-test).
It also has a built-in command testing framework.
It can send commands to a running process and check the output.
Tests can be specified in a json format.
As an example, the workflow to test if **any** Minecraft server boots successfully:
```json
{
  "name": "Server Test",
  "steps": [
    {
      "type": "ENDS_WITH",
      "message": "For help, type \"help\""
    },
    {
      "type": "SEND",
      "message": "stop",
      "timeout": 120
    }
  ]
}
```
It checks for a log message that ends with `For help, type "help"`, 
something all versions of the Minecraft server output upon successful launch.
It then stops the server by sending the stop command to it.
You can write your own test and even run it against the client instead of the server,
provided the client has command support, e.g. via the [hmc-specifics](https://github.com/headlesshq/hmc-specifics).
Just specify the location of your test file in the config with the key
`hmc.test.filename`.
An example CI workflow that tests if HeadlessMc can launch the game with the
hmc-specifics can be found [here](.github/workflows/hmc-specifics-test.yml).

### Optimizations 

HeadlessMc achieves headless mode by patching the LWJGL library: 
every of its functions is rewritten to do nothing, or to return stub values
(you can read more about this [here](headlessmc-lwjgl/README.md)).
This has the advantage of being independent of Minecraft versions,
but comes with some overhead.
A Minecraft version dependent approach are the [hmc-optimizations](https://github.com/headlesshq/hmc-optimizations),
another set of mods which patch Minecraft itself to skip all rendering code.
Additionally HeadlessMc also comes with the `hmc.assets.dummy` property,
which replaces all assets with small dummy textures and sounds,
which allows for a smaller memory footprint and much less downloads before launch.
You can also achieve headless mode without patching lwjgl by running headlessmc with a virtual framebuffer like
[Xvfb](https://www.x.org/releases/X11R7.6/doc/man/man1/Xvfb.1.xhtml).

### Configuring HeadlessMc
> [!NOTE]  
> All configuration options are listed [here](https://headlesshq.github.io/headlessmc/configuration/)

- HeadlessMc stores its configuration in `HeadlessMC/config.properties`.
- On Windows and Linux Java versions in certain folders get detected automatically
  and HeadlessMc can download missing Java distributions.
  But you can also specify which Java installations HeadlessMc can use to run the game.
  Open the file `HeadlessMC/config.properties` and add a key called `hmc.java.versions`,
  with a `;` seperated list of java versions HeadlessMc can use, like this:
    ```properties
    hmc.java.versions=C:/Program Files/Java/jre-<version>/bin/java;C:/Program Files/Java/jdk-<version>/bin/java
    ```
- Restart HeadlessMc or use `config -refresh` and then `java -refresh`, HeadlessMc should now know which Java versions to use.

Properties can also be passed as SystemProperties from the command line.
For available properties see the [HmcProperties](headlessmc-api/src/main/java/io/github/headlesshq/headlessmc/api/config/HmcProperties.java), the
[LauncherProperties](headlessmc-launcher/src/main/java/io/github/headlesshq/headlessmc/launcher/LauncherProperties.java), the
[JLineProperties](headlessmc-jline/src/main/java/io/github/headlesshq/headlessmc/jline/JLineProperties.java), the
[LoggingProperties](headlessmc-logging/src/main/java/io/github/headlesshq/headlessmc/logging/LoggingProperties.java), the
[RuntimeProperties](headlessmc-runtime/src/main/java/io/github/headlesshq/headlessmc/runtime/RuntimeProperties.java) or the
[LwjglProperties](headlessmc-lwjgl/src/main/java/io/github/headlesshq/headlessmc/lwjgl/LwjglProperties.java).

You can e.g. set `hmc.gamedir` to run the game inside another directory.

### In-Memory launching and GraalVM

With the `-inmemory` flag HeadlessMc can even launch the game inside the same
JVM that is running HeadlessMc itself.
Making it possible to really run Minecraft anywhere, where a JVM can run.

This is not possible on GraalVM.
Additionally, HeadlessMc's plugin system and instrumentation process
are also difficult to realize in GraalVM.

But we provide GraalVM images, that are basically a launcher for HeadlessMc itself:
They find/download a suitable Java distribution and run HeadlessMc on it,
without the user having to install Java.

### A Note on command arguments

Arguments passed to commands have to be separated using spaces. If you want to pass an Argument which contains spaces
you need to escape it using quotation marks, like this:
`"argument with spaces"`.
Quotation marks and backslashes can be escaped by using a backslash.
So `msg "A text with spaces"` will send the chat message `A text with spaces`,
while `msg "\"A text with spaces\"" additional space`
will send the chat message `"A text with spaces"` and the argument `additional space` will be dropped.

## Building, Developing and Contributing

Simply run `./gradlew build` or import the [build.gradle](build.gradle)
into an IDE of your choice, and you should be good to go.

In order to keep compatibility with older Java and Minecraft versions
HeadlessMc uses Java language level 8. It can be
built with any JDK &geq; 8, but language features > 8 can't be used. 
HeadlessMc uses [project lombok](https://github.com/projectlombok/lombok)
to eliminate Java boilerplate.

The (sparse) javadoc can be found [here](https://headlesshq.github.io/headlessmc/javadoc/).

Contributions are welcome!

### Plugins

You can also write Plugins for HeadlessMc.
Plugins can run through the `headlessmc-launcher-wrapper`,
which launches the `headlessmc-launcher` on another classloader.
You can find a small example [here](headlessmc-launcher-wrapper/src/testPlugin).

## License and Libraries

Some cool libraries we use:

*   [MinecraftAuth by RaphiMC](https://github.com/RaphiMC/MinecraftAuth)
*   [Deencapsulation by xxDark](https://github.com/xxDark/deencapsulation)
*   [Forge-CLI by TeamKun](https://github.com/TeamKun/ForgeCLI), which we [customized](https://github.com/3arthqu4ke/ForgeCLI).

HeadlessMc is licensed under the [MIT License](LICENSE).
