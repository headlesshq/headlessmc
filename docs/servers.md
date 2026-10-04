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

#### Mods and plugins

You can also manage the mods/plugins of your server.
Read about that [here](mods.md).
