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
  config                      Configures HeadlessMc
  account, auth, login        Manage your account.
  java                        Manage java installations.
  launch                      Launches the game.
  version, download, install  Manage versions.
  profile                     Manage launch profiles.
  server                      Manage servers.
  mod                         Manage mods.
  help                        Display help information about the specified command.
  exit, quit                  Exits the HeadlessMc shell
```

```
> launch -h
Usage: headlessmc launch [-hV] [-eula] [-lwjgl] [--offline] [-g=<gameArgs>] [-j=<jvmArgs>] [-res=<resolution>] [-ret=<retries>] [--server=<server>] [--patchers=<patchers>[,
                         <patchers>...]]... profile...
Launches the game.
      profile...             The profile/server/version to launch
      -eula, --eula-accept   Automatically accepts the EULA if needed.
  -g, --game=<gameArgs>      Arguments for the started game, e.g. --game "--quickPlayRealms <realms-server>"
  -h, --help                 Show this help message and exit.
  -j, --jvm=<jvmArgs>        Arguments for the started JVM, e.g. --jvm "-Xmx2G -Dproperty=value"
      -lwjgl, --headless     Patches LWJGL to not render anything.
      --offline              Launches with an offline account.
      --patchers=<patchers>[,<patchers>...]
                             Comma-separated list of patchers to use.
      -res, --resolution=<resolution>
                             Resolution to start the client with, <width>x<height>, e.g. 800x600
      -ret, --retries=<retries>
                             How many times to retry launching the process.
      --server=<server>      Joins a server immediately after launching the game
  -V, --version              Print version information and exit.
```
