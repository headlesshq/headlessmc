# Launching

Once started, HeadlessMc will start a shell and wait for your command input.
You can try this out by typing `help` and pressing Enter,
to get a list of all available commands.
By default, [JLine](https://jline.org/) is enabled,
allowing you to get suggestions for commands and completing them by pressing TAB.

#### Logging in
Before you can launch the game, you first need to login to your Minecraft account.
For this use the `login` command:
```
> login
To login visit https://www.microsoft.com/link?otc=...
```
Open the link shown in your browser (can even be on another device),
and log into your Microsoft account.
Then return to HeadlessMc,
after a few seconds you should be logged in.

#### Launching

To launch the client, the `launch` command can be used.
```
launch 1.21.5
```
This launches the vanilla version `1.21.5`,
you can also specify a mod loader to use like this:
```
launch fabric 1.21.5
```
Currently HeadlessMc supports the client mod loaders `fabric`, `forge` and `neoforge`.

#### What can be launched

The arguments of `launch` are handled by a *launch target*.
HeadlessMc works out which one is meant, the one that can actually launch the arguments is used:

| Launch target  | Arguments                                         | Example                                         |
|----------------|---------------------------------------------------|-------------------------------------------------|
| `version`      | `[client] [platform] <mc-version> [build]`        | `launch fabric 1.21.5`                          |
| `server`       | `server [platform] <mc-version> [build]`          | `launch server paper 1.21.5`                    |
| `version-file` | the path of a version.json, or the id of an installed version | `launch ~/.minecraft/versions/pack/pack.json`, `launch pack` |
| `jar`          | `[client\|server]` and the path of a jar           | `launch ./tool.jar`                             |
| `script`       | `[client\|server]` and the path of a script        | `launch ./run.sh`                               |
| `packwiz`      | a `pack.toml` or its directory, `client\|server`, and the loader if the pack has several | `launch ./my-pack client` |
| `profile`      | the name of a profile, see [Profiles](#profiles)  | only with `--target profile`                    |

A single argument that is the name of a profile always launches that profile.
If arguments could be meant for more than one launch target, name it with `--target`, short `-t`:
```
launch --target version 1.21.5
```
A version that is installed can also be launched by its id, e.g. a version another launcher installed
as `my-pack`, or `fabric-loader-0.16.14-1.21.5`. If an id is also a version, like `1.21.5`,
it is launched as that version; `--target version-file 1.21.5` launches exactly the installed version.json.

A packwiz pack is launched from its directory or `pack.toml`, `--packwiz <path>` is short for
`--target packwiz <path>`:
```
launch ./my-pack client
launch --packwiz ./my-pack server fabric
```
The pack decides the Minecraft version and loader, and the game runs in the directory of the pack.
To keep settings for a pack, add a profile for it: `profile add ./my-pack client --name my-pack`.

#### Profiles

A profile stores what to launch together with how to launch it: the game directory, JVM and game arguments,
the Java version and so on.
`launch fabric 1.21.5` launches without storing anything.
To keep the settings, add a profile and launch it by its name:
```
> profile add fabric 1.21.5
Added profile fabric-1.21.5 for version fabric/1.21.5
> profile edit fabric-1.21.5 vm-args "-Xmx6G"
> launch fabric-1.21.5
```
`profile add` accepts everything `launch` does.
A profile for a version is named after the version, for everything else a name has to be given with `--name`:
```
profile add ./paper.jar --name lobby
```

A profile remembers what it has been resolved to.
A profile for `fabric 1.21.5 latest` keeps launching the build that was the latest when it was first launched,
it does not change when a new build is released. To move it to the build that is the latest now:
```
profile update fabric-1.21.5-latest
```

A profile can launch another profile and add its own settings on top of it.
JVM and game arguments are appended to those of the other profile, other settings replace them:
```
profile add --target profile fabric-1.21.5 --name fabric-debug
profile edit fabric-debug vm-args "-Ddebug=true"
```

The game directory of a profile is chosen when the profile is added.
`profile move` moves it somewhere else, and can also rename the profile:
```
profile move fabric-1.21.5 /mnt/ssd/games/fabric
profile move fabric-1.21.5 --name survival
```
`profile edit <profile> path <directory>` only makes the profile use another directory.
If the profile has a game directory and there is nothing at the new path yet, it asks whether to move
the game directory there instead; `--move` and `--no-move` answer that in advance.

#### Multiple Minecraft directories

Versions, libraries and assets are installed into the Minecraft directory, by default the `.minecraft` directory
of your system, or `hmc.files.mc`. If you have more of them, e.g. on another disk, list them in `hmc.files.mc-dirs`:
```
hmc.files.mc-dirs=/mnt/ssd/.minecraft,/mnt/hdd/.minecraft
```
HeadlessMc can also look for them itself: with `hmc.files.mc-detect=true` the other drives of your computer
are searched for `.minecraft` directories, on Windows every drive, elsewhere what is mounted in `/mnt`, `/media`,
`/run/media` and `/Volumes`. A drive is only searched a few directories deep, see `hmc.files.mc-detect-depth`,
so that `D:\.minecraft` or `D:\Games\HMCL\.minecraft` are found. `debug` lists the directories that are used.

The versions installed in these directories are listed by `version list` and can be launched.
A version is always launched with the libraries and assets of the directory it is installed in.
New versions are installed into the primary directory, unless another one is given with `--mc`:
```
install fabric 1.21.5 --mc /mnt/ssd/.minecraft
profile move fabric-1.21.5 --mc /mnt/ssd/.minecraft
```

#### Using the Minecraft directory of another launcher

Launchers do not agree on where the game directory of a version is, the directory with its worlds, mods and settings.
By default HeadlessMc uses a directory named after the version in the Minecraft directory, e.g. `.minecraft/fabric-1.21.5`.
HMCL and PCL2 with version isolation use the directory the version is installed in, `.minecraft/versions/<name>`.
To play the instances of such a launcher with their worlds and mods, set `hmc.files.game-dir-layout`:

| `hmc.files.game-dir-layout` | Game directory of a version                                                               |
|-----------------------------|-------------------------------------------------------------------------------------------|
| `headlessmc` (default)      | `.minecraft/<platform>-<version>`, or `.minecraft` if `hmc.files.game-for-each-version` is `false` |
| `versions`                  | `.minecraft/versions/<name>`, the directory the version is installed in                  |
| `versions-modded`           | like `versions` for modded versions, vanilla versions share `.minecraft` (the default of HMCL) |

```
hmc.files.mc=/path/to/the/.minecraft
hmc.files.game-dir-layout=versions
```
```
> version list
> launch "My Modpack"
> profile add "My Modpack" --name modpack
```
An instance is launched by the name it has in the other launcher, which is the id of its version.json.

The layout only decides the game directory of new profiles and of launches without a profile.
A profile keeps the game directory it has, `profile move` moves it.
In these layouts a version and a game share a directory, HeadlessMc keeps them apart:
`profile move` leaves the version installed, and `version remove` never deletes the files of a game.

#### Headless Launch
One of the main features of HeadlessMc is
that it can launch the client in headless mode,
without displaying a GUI.
This is achieved by patching the LWJGL library to not render anything
and allows you to run the client on servers
or in CI/CD pipelines without graphics devices.
In order to launch the game in headless mode, add the `--headless` flag:
```
launch --headless <version>
```

Generally there are two Minecraft settings that you might want to turn off for running the client headlessly.
They are not problematic,
but turning them off solves a lot of potential issues when debugging etc.
These are the accessibility screen, 
which is shown the first time you launch a fresh Minecraft instance and the hidden setting `pauseOnLostFocus`, 
which makes SinglePlayer worlds pause when you tab out.
Change the Minecraft `options.txt` to include these lines:
```
pauseOnLostFocus:false
onboardAccessibility:false
narrator:0
```

#### Managing Versions
You can get a list of all currently downloaded client versions with the `version list`
command:
```
> version list
name                        parent   type
26.1                        -        release
1.21.1                      -        release
fabric-loader-0.19.5-26.1   26.1     release
1.4.5                       -        release
1.6.4                       -        release
neoforge-21.1.248           1.21.1   release
26.3-snapshot-3             -        snapshot
1.12.2                      -        release
...
```

The launch command automatically downloads versions
if they are specified in the `<modloader> <version>` format.
You can also manually manage versions with the `download` command:
```
> download 1.12.2
Downloading 1.12.2...
Download successful!

> version ls
name                        parent   type
1.12.2                      -        release
...
```

#### Downloading without launching
Installing a version only writes its version.json.
The client jar, the libraries, the natives and the assets are downloaded when the game is launched.
To download all of them without starting the game, e.g. to prepare a CI cache or an offline machine,
launch with `--dry-run`:
```
launch --dry-run fabric 1.21.5
```
This does everything a launch does right up to starting the process.
It also resolves the Java runtime the game would be started with,
and downloads it if none is installed and `hmc.java.download` is enabled.
The command fails with a non-zero exit code if any download fails or no matching Java runtime is found.
A dry run does not need an account, and it does not create a backup.
A dry run does not quit the interactive HeadlessMc shell, so you can launch the game afterwards.
`profile launch` and `server launch` accept `--dry-run` too.

#### Quitting HeadlessMc

Simply type `exit` to exit HeadlessMc.
