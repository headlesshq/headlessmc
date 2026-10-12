# Servers

HeadlessMc can also be used to manage paper, fabric, purpur, neoforge, forge and vanilla servers.
You can add a server with the `server add <type> <version> <name>` command.
If `<version>` is not specified,
the server for the latest Minecraft version will be used.

``` title="Adding a server"
> server add paper 1.21.5
Downloading Paper 100% │█████████████████████│ 51/51mb (0:00:07 / 0:00:00) 7.3mb/s
Installed server server-paper-1.21.5 for version server/paper/1.21.5
```

``` title="Listing servers"
> server list
name                  version               directory
server-paper-1.21.5   server/paper/1.21.5   /home/me/.local/share/headlessmc/server/server-paper-1.21.5
```

``` title="Accepting the EULA of a server"
> server eula accept server-paper-1.21.5 
Accepted the EULA of server-paper-1.21.5
```

``` title="Launching a server"
> server launch server-paper-1.21.5 
...
```

The argument `nogui`, to start the server without a GUI,
will be added automatically by HeadlessMc.
You can also specify JVM and other game arguments like this:

``` title="Launching a server with arguments"
> server launch paper-1.21.5-76 --jvm "-Xms10G -Xmx10G" --game-args "bonusChest eraseCache"
```

#### Forge and NeoForge server scripts

Newer Forge and NeoForge servers are started with a `run.sh` (or `run.bat` on Windows) script
instead of a server jar.
By default, HeadlessMc runs this script directly,
with the bin directory of the Java version for the server first on the `PATH`.
JVM arguments and system properties are passed to Java via the `JDK_JAVA_OPTIONS` environment variable,
game arguments are passed to the script.
On Windows the `pause` at the end of the `run.bat` is removed, by running a copy called `hmc_run_server.bat`.

Alternatively, you can enable `hmc.server.script.mock-java`:

```
> config set hmc.server.script.mock-java true
```

HeadlessMc will then run the script with a mock `java` command,
which only records the arguments the script passes to it.
HeadlessMc parses these arguments, including `@argument` files like `user_jvm_args.txt`,
and launches the server itself, just like it launches servers that use a jar.
If the script does not call `java` within `hmc.server.script.timeout` seconds, the launch fails.

#### Mods and plugins

You can also manage the mods/plugins of your server.
Read about that [here](mods.md).
