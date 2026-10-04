# Mods

HeadlessMc can manage mods for both the client and server.
For this the `mod` and the `server mod` command are used.
Currently, HeadlessMc only supports [Modrinth](https://modrinth.com/) for finding mods.

``` title="Searching for mods"
> mod search fabric-api
id                                     name                                                          description
fabric-api                             Fabric API                                                    Lightweight and modular API providing common hooks and intercompatibility measures utilized by mods using the Fabric toolchain.
forgified-fabric-api                   Forgified Fabric API                                          Fabric API implemented on top of NeoForge
qsl                                    Quilted Fabric API (QFAPI) / Quilt Standard Libraries (QSL)   The standard libraries of the Quilt ecosystem. Essential for your modding experience on Quilt!
fabric-permissions-api                 fabric-permissions-api                                        A simple permissions API for Fabric.
legacy-fabric-api                      Legacy Fabric API                                             The legacy fabric version of the fabric api.
reforged-fabric-api                    Reforged Fabric API                                           Core API Library for Forge
fabric-key-binding-api-v1-for-1.14.2   Fabric Key Binding API for 1.14.2                             Ports the fabric-key-binding-api-v1 to 1.14.2 as it is missing in that version of the Fabric API.
legacy-fabric-api-fixes-btw            Legacy Fabric API fixes for BTW                               Make Legacy Fabric API usable with BetterThanWolves CE 3.0.
fake-fabric-api                        Fake Fabric API                                               Provides a fake fabric-api mod id
chunk-storage-api-fabric               Chunk Storage Api Fabric                                      Library mod for Fabric
...
```

Starting with HeadlessMc 3 you can also search for resourcepacks, datapacks, modpacks, etc.:
``` title="Searching for resourcepacks"
> mod search --type resourcepack faithful
id                                                    name                                                    description
faithful-64x                                          Faithful 64x                                            An even more detailed experience with quadruple-resolution textures!
faithful-32x                                          Faithful 32x                                            The original Minecraft texture feel, with double the resolution and double the fun!
...
```

The id column contains the identifier that is used for adding a modification.
To add a mod, use the `mod add` command and specify which version to
add the mod for, e.g. by using the versions id.
You also need to specify what type of modification you are adding, e.g. mod/resourcepack, etc.
`mod add <type> <mod-id> <version>`

``` title="Adding a mod"
> mod add mod fabric-api fabric 26.1
Downloaded /home/me/.minecraft/fabric-26.1/mods/fabric-api-0.155.3+26.1.2.jar successfully.
```

``` title="Adding a resourcepack"
> mod add resourcepack faithful-32x fabric 26.1
Downloading mod faithful-32x 100% │██████│ 12/12mb (0:00:01 / 0:00:00) 12.0mb/s
Downloaded /home/me/.minecraft/fabric-26.1/resourcepacks/Faithful 32x - 26.1.zip successfully.
```

``` title="Adding a shaderpack"
> mod add shader complementary-reimagined fabric 26.1
Downloaded /home/okfk/.minecraft/fabric-26.1/shaderpacks/ComplementaryReimagined_r5.9.3.zip successfully.
```

``` title="Uninstall mod"
> mod rm fabric-api fabric 26.1
Deleted mod successfully.
```

## Server Side
For servers like Paper mods are  called plugins:
``` title="Searching for server plugins"
> mod search --type plugin test server-paper-1.21.5 
id                           name                         description
stresstestbots               StressTestBots               A simple plugin that adds fake players to help stress test your server.
pulsetest                    PulseTest                    PulseTest is a lightweight Paper plugin that generates controlled CPU and RAM load to quickly stress-test and benchmark your Minecraft server.
```

## Datapacks

Datapacks require you to specify a world to install them on.
You can list the worlds of your version using the worlds command:
``` title="Listing worlds"
> mod worlds fabric 26.1
world       path
New World   /home/me/.minecraft/fabric-26.1/saves/New World
```

``` title="Searching for a datapack"
> mod search --type datapack veinminer fabric-26.1
id                         name                               description
veinminer                  VeinMiner                          Mine the whole vein on mining a single ore/block. Make the tedious mining experience to something satisfying and fun!
...
```

``` title="Adding a datapack"
> mod add --world "New World" datapack veinminer fabric 26.1
Downloaded /home/me/.minecraft/fabric-26.1/saves/New World/datapacks/veinminer-1.3.4.zip successfully.
```
