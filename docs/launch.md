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
