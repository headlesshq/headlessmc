# Launching

Once started, HeadlessMc will wait for your command input.
You can try this out by typing `help` and pressing Enter,
to get a list of all available commands.
By default, [JLine](https://jline.org/) is enabled,
allowing you to get suggestions for commands and completing them by pressing TAB.

#### Logging in
Before you can launch the game, you first need to login to your Minecraft account.
For this use the `login` command:
```
> login
Starting login process 0, enter 'login -cancel 0' to cancel the login process.
Go to https://www.microsoft.com/link?otc=...
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
you can also specify a modloader to use like this:
```
launch fabric:1.21.5
```
Currently HeadlessMc supports the client modloaders `fabric`, `forge` and `neoforge`.

#### Headless Launch
One of the main features of HeadlessMc is,
that it can launch the client in headless mode,
without displaying a GUI.
This is achieved by patching the LWJGL library to not render anything
and allows you to run the client on servers
or in CI/CD pipelines without graphics devices.
In order to launch the game in headless mode, add the `-lwjgl` flag:
```
launch <version> -lwjgl
```

For a fresh headless instance, put these settings in `options.txt` inside the
Minecraft game directory (`hmc.gamedir`):
```
pauseOnLostFocus:false
onboardAccessibility:false
narrator:0
```
`pauseOnLostFocus` keeps singleplayer worlds ticking without window focus.
`onboardAccessibility` skips the first-run accessibility screen. That screen can
speak its introduction even when `narrator` is off: it uses Minecraft's
text-to-speech library directly. This also happens in 26.2; `-lwjgl` redirects
LWJGL rendering and does not mute that separate library.


#### Managing Versions
You can get a list of all currently downloaded client versions with the `versions`
command:
```
> versions
id   name                           parent
0    1.20.6                         
1    1.21.5                         
2    fabric-loader-0.16.14-1.21.5   1.21.5
```
This outputs a table of all client versions downloaded.
HeadlessMc makes use of such tables a lot.
You can use the id and name columns to specify versions.
``` title="Use id 1 to launch 1.21.5"
launch 1
```
``` title="Using the name of the version to launch"
launch fabric-loader-0.16.14-1.21.5
```

The launch command automatically downloads versions
if they are specified in the `<modloader>:<version>` format.
You can also manually manage versions with the `download` command:
```
> download 1.12.2
Downloading 1.12.2...
Download successful!

> versions
id   name                           parent
0    1.12.2                         
...
```
Modloaders for versions can be installed using the respective command:
```
> forge 1.12.2
Installing Forge 1.12.2-14.23.5.2860
...
Forge 1.12.2-14.23.5.2860 installed successfully!

> versions
id   name                           parent
0    1.12.2                         
1    1.12.2-forge-14.23.5.2860      1.12.2
...
```

#### Quitting HeadlessMc

Simply type `quit` to exit HeadlessMc.

#### Interactive Mode
By default, HeadlessMc will run in interactive mode.
It will continuously listen for commands,
until you end the process with quit.
You can also execute singular commands by specifying them at start with `--command`, like this:
```shell
java -jar headlessmc-launcher.jar --command <command>

./headlessmc-launcher-native --command <command>
```
