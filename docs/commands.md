# Commands

You can list all commands available in HeadlessMc with the `help` command.
You can use any command with the `-h` or `--help` options to gain more information.

```
> help
Usage: headlessmc [-hV] [COMMAND]
test
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  config        Configures HeadlessMc
  account, auth Manage your account.
  java          Manage java installations.
  launch        Launches the game.
  version       Manage versions.
  profile       Manage launch profiles.
  server        Manage servers.
  mod           Manage mods.
  backup        Backs up a profile/server or its worlds.
  help          Display help information about the specified command.
  exit, quit    Exits the HeadlessMc shell
```

```
> launch -h
Usage: headlessmc launch [-hV] [--dry-run] [-eula] [-lwjgl] [--no-backup] [--offline] [-g=<gameArgs>] [-j=<jvmArgs>] [--mc=<mc>] [--packwiz=<packwiz>] [-res=<resolution>]
                         [-ret=<retries>] [--server=<server>] [-t=<target>] [--patchers=<patchers>[,<patchers>...]]... [profile...]
Launches the game.
      [profile...]           The profile to launch, or what to launch, e.g. 'fabric 1.21.1'.
      --dry-run              Installs and downloads everything needed to launch, e.g. the jar, libraries, assets and the Java runtime, but stops right before the game would be
                               started.
      -eula, --eula-accept   Automatically accepts the EULA if needed.
  -g, --game=<gameArgs>      Arguments for the started game, e.g. --game "--quickPlayRealms <realms-server>"
  -h, --help                 Show this help message and exit.
  -j, --jvm=<jvmArgs>        Arguments for the started JVM, e.g. --jvm "-Xmx2G -Dproperty=value"
      -lwjgl, --headless     Patches LWJGL to not render anything.
      --mc=<mc>              The Minecraft directory to launch from and to install missing versions into, if there are multiple (hmc.files.mc-dirs).
      --no-backup            Does not back up the profile before launching, see hmc.backup.auto.
      --offline              Launches with an offline account.
      --packwiz=<packwiz>    Path to a pack.toml packwiz modpack file, or a directory containing one, to launch. The same as '--target packwiz <path>'.
      --patchers=<patchers>[,<patchers>...]
                             Comma-separated list of patchers to use.
      -res, --resolution=<resolution>
                             Resolution to start the client with, <width>x<height>, e.g. 800x600
      -ret, --retries=<retries>
                             How many times to retry launching the process.
      --server=<server>      Joins a server immediately after launching the game
  -t, --target=<target>      The launch target that handles the arguments, e.g. version, server, version-file, jar, script, packwiz or profile. If it is not specified, it is
                               detected from the arguments.
  -V, --version              Print version information and exit.
```

What `launch`, `profile add` and `server add` take as arguments is explained in [Launching](launch.md#what-can-be-launched).

#### Profiles

```
> profile -h
Usage: headlessmc profile [-hV] [COMMAND]
Manage launch profiles.
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  add               Adds a new profile.
  list, ls          Lists your profiles.
  remove, rm        Removes a profile.
  edit              Edits a profile
  move, mv, rename  Moves the game directory of a profile and/or renames the profile.
  update            Resolves what a profile launches again, e.g. to move it to the latest build.
  launch            Launches a profile.
```

```
> profile add -h
Usage: headlessmc profile add [-fhV] [-d=<customDir>] [--mc=<mc>] [-n=<name>] [-t=<target>] [-u=<customURL>] [TARGET...]
Adds a new profile.
      [TARGET...]         What the profile launches, e.g. 'fabric 1.21.1', the path of a jar, or with '--target profile' the name of a profile to inherit from.
  -d, --dir=<customDir>   The game directory of the profile, for servers the directory the server is installed in.
  -f, --force             Overwrites an existing profile and forces reinstallation even if the version is already installed.
  -h, --help              Show this help message and exit.
      --mc=<mc>           The Minecraft directory to install a client into, by default the primary one (hmc.files.mc).
  -n, --name=<name>       The name of the new profile. Only optional if a version is added, which the profile can be named after.
  -t, --target=<target>   The launch target that handles the arguments, e.g. version, server, version-file, jar, script, packwiz or profile. If it is not specified, it is detected
                            from the arguments.
  -u, --url=<customURL>   Uses a custom URL to download the installer.
  -V, --version           Print version information and exit.
```

```
> profile edit -h
Usage: headlessmc profile edit [-hV] [--[no-]move] [-t=<target>] profile [field] [value]
Edits a profile
      profile             The name of the profile to edit.
      [field]             The field of the profile to edit.
      [value]             The value to set the profile field to (optional).
  -h, --help              Show this help message and exit.
      --[no-]move         For the field 'path': whether to move the game directory to the new path, like 'profile move', or to only make the profile use the new path. If it is not
                            specified and there is a game directory to move and nothing at the new path yet, you are asked.
  -t, --target=<target>   For the field 'target': the launch target that handles the new value, e.g. version, jar or profile. If it is not specified, it is detected from the value.
  -V, --version           Print version information and exit.
```

```
> profile move -h
Usage: headlessmc profile move [-fhV] [--with-shared] [--mc=<mc>] [-n=<newName>] [profile] [directory]
Moves the game directory of a profile and/or renames the profile.
      [profile]          The name of the profile to move.
      [directory]        The new game directory, it must not exist or be empty.
  -f, --force            Overwrites a profile that already has the new name.
  -h, --help             Show this help message and exit.
      --mc=<mc>          Moves the profile to another Minecraft directory: its game directory to the default location for that directory, unless a directory is given, and what it
                           launches is installed there.
  -n, --name=<newName>   The new name.
  -V, --version          Print version information and exit.
      --with-shared      Also moves the game directory for the other profiles using it.
```

```
> profile update -h
Usage: headlessmc profile update [-hV] [--mc=<mc>] profile
Resolves what a profile launches again, e.g. to move it to the latest build.
      profile     The name of the profile to update.
  -h, --help      Show this help message and exit.
      --mc=<mc>   The Minecraft directory to launch the profile from, the version is installed into it if needed.
  -V, --version   Print version information and exit.
```

#### Servers

```
> server -h
Usage: headlessmc server [-hV] [COMMAND]
Manage servers.
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  add, install, download  Downloads and installs a new server.
  list, ls                Lists your servers.
  remove, rm              Removes a server.
  move, mv, rename        Moves the directory of a server and/or renames the server.
  launch                  Launches a server.
  eula                    Manage the EULA of a server.
```

```
> server move -h
Usage: headlessmc server move [-fhV] [--with-shared] [-n=<newName>] [SERVER] [directory]
Moves the directory of a server and/or renames the server.
      [SERVER]           The name of the server to move.
      [directory]        The new game directory, it must not exist or be empty.
  -f, --force            Overwrites a profile that already has the new name.
  -h, --help             Show this help message and exit.
  -n, --name=<newName>   The new name.
  -V, --version          Print version information and exit.
      --with-shared      Also moves the game directory for the other profiles using it.
```

#### Versions

```
> version install -h
Usage: headlessmc version install [-fhV] [-d=<customDir>] [--mc=<mc>] [-n=<name>] [-u=<customURL>] [VERSION...]
Downloads and installs a version.
      [VERSION...]        The version to install, e.g. 'fabric 1.21.1'.
  -d, --dir=<customDir>   Installs the version to a custom directory, for clients a Minecraft directory, for servers the directory of the server.
  -f, --force             Forces reinstallation even if the version is already installed.
  -h, --help              Show this help message and exit.
      --mc=<mc>           The Minecraft directory to install a client into, by default the primary one (hmc.files.mc).
  -n, --name=<name>       This name is given to the profile/server to install.
  -u, --url=<customURL>   Uses a custom URL to download the installer.
  -V, --version           Print version information and exit.
```

```
> version remove -h
Usage: headlessmc version remove [-afhV] [--dry-run] [--mc=<mc>] [VERSION...]
Deletes an installed version and the libraries and assets no other version needs.
      [VERSION...]   The version to delete, e.g. 'fabric 1.21.1 0.16.9', or the name of its version.json.
  -a, --all          Also deletes the versions inheriting from the version and the profiles that need it, requires --force.
      --dry-run      Only lists the files that would be deleted.
  -f, --force        Deletes the version even if profiles or other versions need it.
  -h, --help         Show this help message and exit.
      --mc=<mc>      The Minecraft directory to delete the version from, if it is installed in multiple directories.
  -V, --version      Print version information and exit.
```
