# Configuration

Besides the commands and their flags and options,
HeadlessMc creates a `config.properties` file in the `HeadlessMC` folder.
This file can be used to configure HeadlessMc further.
All properties that can be put in the config file
can also be specified as system properties on the command line:
```
java -Dhmc.some.property=value -jar headlessmc-launcher.jar
```

#### hmc.account.refresh.on.game.launch
Type: `#!java boolean` Default: `#!java true`

Enable/disable if the launcher should refresh your account when launching the game.

#### hmc.account.refresh.on.launch
Type: `#!java boolean` Default: `#!java false`

Enable/disable if the launcher should refresh your account when it is started.

#### hmc.additional.classpath
Type: `#!java String` Default: `#!java ""`

Additional files to add to the classpath of Minecraft when it is launched.

#### hmc.always.download.assets.index
:warning:{ title="Deprecated" } Type: `#!java boolean` Default: `#!java false`

Always download the asset index for a Minecraft version.
Needed for CheerpJ as it corrupts in the browser and needs a redownload.

#### hmc.always.in.memory
Type: `#!java boolean` Default: `#!java false`

Always launches the game [in-memory](in-memory.md) with `-inmemory`.

#### hmc.always.jndi.flag
Type: `#!java boolean` Default: `#!java true`

Always adds the `-jndi` flag when launching the game.

#### hmc.always.lookup.flag
Type: `#!java boolean` Default: `#!java true`

Always adds the `-lookup` flag when launching the game.

#### hmc.always.lwjgl.flag
Type: `#!java boolean` Default: `#!java false`

Always adds the `-lwjgl` flag when launching the game.

#### hmc.always.pauls.flag
Type: `#!java boolean` Default: `#!java false`

Always adds the `-paulscode` flag when launching the game.

#### hmc.always.quit.flag
Type: `#!java boolean` Default: `#!java false`

Always adds the `-quit` flag when launching the game.

#### hmc.arm.fix.libraries
Type: `#!java boolean` Default: `#!java true`

Minecraft does not provide the correct binary files for Linux on ARM64.
This downloads the correct lwjgl binaries from maven central.

#### hmc.assets.backoff
Type: `#!java boolean` Default: `#!java true`

Increases wait time on assets that have failed to download if `#!java true`.

#### hmc.assets.check.file.hash
Type: `#!java boolean` Default: `#!java false`

Checks the integrity of all assets before launching.
Might clash with [hmc.assets.dummy](#hmcassetsdummy).

#### hmc.assets.check.hash
Type: `#!java boolean` Default: `#!java true`

Checks the integrity of assets when downloading them.

#### hmc.assets.check.size
Type: `#!java boolean` Default: `#!java true`

Checks the size of assets when downloading them.

#### hmc.assets.delay
Type: `#!java int` Default: `#!java 0`

Initial delay to wait for when an asset download failed in milliseconds.

#### hmc.assets.dummy
Type: `#!java boolean` Default: `#!java false`

Uses dummy assets (very small images and sound files),
to reduce the memory footprint of Minecraft.
Do not use when not running headlessly.

#### hmc.assets.parallel
Type: `#!java boolean` Default: `#!java true`

Downloads assets on multiple threads,
speeding up the download process.

#### hmc.assets.retries
Type: `#!java int` Default: `#!java 3` Min: `#!java 1`

How many times we want to retry to download an asset that failed to download
before failing.

#### hmc.assumed.java.version
Type: `#!java int` Default: `#!java 8`

If we cannot determine the current Java version HeadlessMc is running on,
this one will be taken.

#### hmc.auto.download.java
Type: `#!java boolean` Default: `#!java true`

Automatically downloads missing Java versions.

#### hmc.auto.download.java.rethrow.exception
Type: `#!java boolean` Default: `#!java true`

Fails hard on failed Java downloads.

#### hmc.auto.download.specifics
Type: `#!java boolean` Default: `#!java false`

Automatically downloads the hmc-specifics when launching.

#### hmc.auto.download.versions
:warning:{ title="Deprecated" } Type: `#!java boolean` Default: `#!java true`

Automatically downloads versions when specified in the `<modloader>:<version>`
format.

#### hmc.auto.java.distribution
Type: `#!java String` Default: `#!java "temurin"`

Java distribution to use when automatically downloading Java.

#### hmc.check.xvfb
Type: `#!java boolean` Default: `#!java false`

Checks if `xvfb` is running for offline `-lwjgl` checks.

#### hmc.clientId
Type: `#!java String` Default: `#!java ""`

Client id to fill in for the Minecraft client.
Is used for telemetry to Mojang.

#### hmc.crash.report.watcher
Type: `#!java boolean` Default: `#!java false`

Watches the game directory for crash-reports that are created,
and kills the process if it finds one.
This is for testing purposes.
Sometimes the game crashes,
but a modloader keeps a window open with the crash message,
waiting for user input.

#### hmc.crash.report.watcher.exit
Type: `#!java boolean` Default: `#!java true`

Exits with status code -1 if the process has already ended after
finding a crash report.

#### hmc.deencapsulate
Type: `#!java boolean` Default: `#!java true`

Enables the [deencapsulator](https://github.com/xxDark/deencapsulation) for Java 9+ versions.
This is needed for some reflection hacks.

#### hmc.dont.ask.for.quit
:warning:{ title="Deprecated" } Type: `#!java boolean` Default: `#!java false`

The runtime will ask for confirmation when using the `quit` command.
With this set to `true` not anymore.

#### hmc.email
Type: `#!java String` Default: `#!java null`

Specify [hmc.email](#hmcemail) and [hmc.password](#hmcpassword),
to log into an account automatically when the launcher starts.

#### hmc.enable.reflection
:warning:{ title="Deprecated" } Type: `#!java String` Default: `#!java false`

Enables hacky reflection commands in the runtime and in the hmc-specifics.

#### hmc.exit.on.failed.command
Type: `#!java boolean` Default: `#!java false`

For testing purposes.
Exits with status code -1 if a command fails.

#### hmc.extracted.file.cache.uuid
Type: `#!java UUID` Default: `#!java UUID.randomUUID()`

For each game launch a directory for the extracted files is created.
This command allows you to specify the name of that directory.

#### hmc.fabric.url
Type: `#!java URL` 
Default: `#!java "https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.3/fabric-installer-1.0.3.jar"`

The URL to download the fabric installer from.

#### hmc.filehandler.enabled
Type: `#!java boolean` Default: `#!java true`

If HeadlessMc should log to a file in the HeadlessMc folder.
For the runtime/hmc-specifics this is `#!java false`.

#### hmc.game.dir.for.each.version
Type: `#!java boolean` Default: `#!java false`

Creates a seperate game directory for each version.

#### hmc.gameargs
Type: `#!java String[]` Default: `#!java []`

Similar to the `--game-args` option in the launch command.
` ` delimitted list of arguments to launch the game with.

#### hmc.gamedir
Type: `#!java String` Default: `#!java ".minecraft"`

The directory to run the game in.
By default, this is the same as the `.minecraft` directory.

#### hmc.graal.java.version
Type: `#!java int` Default: `#!java 21`

The version to launch HeadlessMc with when running from a Graal Native image.

#### hmc.graal.distribution
Type: `#!java String` Default: `#!java "temurin"`

The Java distribution to use for launching HeadlessMc from a Graal Native image.

#### hmc.graal.force.download
Type: `#!java boolean` Default: `#!java false`

Always downloads a Java version when launching HeadlessMc from a Graal Native image.

#### hmc.graal.jdk
Type: `#!java boolean` Default: `#!java false`

Whether to use a JDK for launching HeadlessMc or, by default, a JRE.

#### hmc.http.user.agent
Type: `#!java String` Default: `#!java "Mozilla/5.0"`

The user agent to use for downloads.

#### hmc.http.user.agent.enabled
Type: `#!java boolean` Default: `#!java true`

If the [user agent](#hmchttpuseragent) should be added to requests.
Needed for web, 
as browser do not allow requests where the user agent has been set.

#### hmc.in.memory
Type: `#!java boolean` Default: `#!java false`

Set by the launcher when launching Minecraft [in-memory](in-memory.md).
Can be checked by a running instance.

#### hmc.in.memory.require.correct.java
Type: `#!java boolean` Default: `#!java true`

Requires the correct Java version for [in-memory](in-memory.md),
or fails hard.

#### hmc.install.mc.logging
Type: `#!java boolean` Default: `#!java false`

Installs the `logging.xml` files defined by Minecraft.
However, these inhibit logging.

#### hmc.invert.jndi.flag
:warning:{ title="Deprecated" } Type: `#!java boolean` Default: `#!java false`

Inverts the meaning of the `-jndi` flag.

#### hmc.invert.lookup.flag
:warning:{ title="Deprecated" } Type: `#!java boolean` Default: `#!java false`

Inverts the meaning of the `-lookup` flag.

#### hmc.invert.lwjgl.flag
:warning:{ title="Deprecated" } Type: `#!java boolean` Default: `#!java false`

Inverts the meaning of the `-lwjgl` flag.

#### hmc.invert.pauls.flag
:warning:{ title="Deprecated" } Type: `#!java boolean` Default: `#!java false`

Inverts the meaning of the `-paulscode` flag.

#### hmc.invert.quit.flag
:warning:{ title="Deprecated" } Type: `#!java boolean` Default: `#!java false`

Inverts the meaning of the `-quit` flag.

#### hmc.java.always.add.file.permissions
Type: `#!java boolean` Default: `#!java false`

`jspawnhelper` needs execution permissions on Linux,
so that we can launch newly installed java versions.

#### hmc.java.require.exact
Type: `#!java boolean` Default: `#!java false`

Requires the exact requires version of Java to launch a Java process.
If off, e.g. Java 21 can be launched even though Java 17 was requested.

#### hmc.java.use.current
Type: `#!java boolean` Default: `#!java true`

Makes HeadlessMc also consider the version of Java it is running with.

#### hmc.java.versions
Type: `#!java String[]` Default: `#!java []`

`;` delimited list of paths to `bin/java` of Java versions
HeadlessMc can use.
Not really needed anymore as HeadlessMc can
download Java on its own now.

#### hmc.jline.bracketed.paste
Type: `#!java boolean` Default: `#!java true`

Disables JLine bracketed paste.

#### hmc.jline.dumb
Type: `#!java boolean` Default: `#!java false`

Starts JLine with a dumb terminal.

#### hmc.jline.dumb.when.no.console
Type: `#!java boolean` Default: `#!java true`

Makes JLine terminal dumb if `#!java System.console() == null`.

#### hmc.jline.enable.progressbar
Type: `#!java boolean` Default: `#!java true`

Enables progress bar support for JLine.

#### hmc.jline.enabled
Type: `#!java boolean` Default: `#!java true`

Enables JLine.

#### hmc.jline.exec
Type: `#!java boolean` Default: `#!java false`

Enables JLine exec support.

#### hmc.jline.ffm
Type: `#!java boolean` Default: `#!java false`

Enables JLine ffm support.

#### hmc.jline.force.not.dumb
Type: `#!java boolean` Default: `#!java false`

Force JLine not to use a dumb terminal.

#### hmc.jline.in
Type: `#!java boolean` Default: `#!java false`

Makes JLine use `#!java System.in`.

#### hmc.jline.jansi
Type: `#!java boolean` Default: `#!java false`

Enables JLine jansi support.

#### hmc.jline.jna
Type: `#!java boolean` Default: `#!java true`

Enables JLine JNA support.
JNA is packaged with HeadlessMc and Minecraft and is well supported.

#### hmc.jline.jni
Type: `#!java boolean` Default: `#!java true`

Enables JLine JNI support.

#### hmc.jline.no.deprecation.warning
Type: `#!java boolean` Default: `#!java true`

Prevents JLine deprecation warnings.

#### hmc.jline.out
Type: `#!java boolean` Default: `#!java false`

Makes JLine use `#!java System.out`.

#### hmc.jline.progressbar.style
Type: `#!java String` Default: `#!java COLORFUL_UNICODE_BLOCK`

Which progressbar style to use.

#### hmc.jline.propagate.enabled
Type: `#!java boolean` Default: `#!java true`

Propagates `hmc.jline.enabled` to Minecraft,
good for hmc-specifics.

#### hmc.jline.providers
Type: `#!java String` Default: `#!java jni`

JLine providers.

#### hmc.jline.read.prefix
Type: `#!java String` Default: `#!java ""`

Read the command line with a prefix, like `>`.

#### hmc.jline.system
Type: `#!java boolean` Default: `#!java null`

Whether to use JLine system providers.

#### hmc.jline.type
Type: `#!java String` Default: `#!java null`

Type of JLine to use.

#### hmc.joml.no.unsafe
Type: `#!java boolean` Default: `#!java true`

Adds `-Djoml.nounsafe=true` when launching with `-lwjgl`.
Needed to not crash.

#### hmc.jvmargs
Type: `#!java String` Default: `#!java ""`

` ` delimited list of JVM args to use when launching Minecraft.

#### hmc.keepfiles
Type: `#!java boolean` Default: `#!java false`

Keeps extracted native files after launching Minecraft.

#### hmc.launchername
Type: `#!java String` Default: `#!java "HeadlessMc"`

Minecraft takes the name of the Launcher as an argument,
e.g. for telemetry?

#### hmc.launcherversion
Type: `#!java String` Default: `#!java "$VERSION"`

Minecraft takes the version of the Launcher as an argument,
e.g. for telemetry?

#### hmc.libraries.check.file.hash
Type: `#!java boolean` Default: `#!java false`

Check the file hashes of all libraries before launching the game.

#### hmc.libraries.check.hash
Type: `#!java boolean` Default: `#!java true`

Check the file hashes when downloading libraries.

#### hmc.libraries.check.size
Type: `#!java boolean` Default: `#!java true`

Check the file size when downloading libraries.

#### hmc.loglevel
Type: `#!java String` Default: `#!java "WARNING"`

The loglevel at which HeadlessMc logs.

#### hmc.lwjgl.update_sleep
Type: `#!java int` Default: `#!java 1`

Delay in milliseconds to sleep in Minecrafts main loop
on Display update.

#### hmc.lwjgl.gltextureinternalformat
Type: `#!java int` Default: `#!java 32856`

Internal texture format for lwjgl instrumentation.

#### hmc.lwjgl.texturesize
Type: `#!java int` Default: `#!java 1024`

Default texture size to return in lwjgl instrumentation.

#### hmc.lwjgl.fullscreen
Type: `#!java boolean` Default: `#!java true`

Fullscreen mode in lwjgl instrumentation.

#### hmc.lwjgl.screenwidth
Type: `#!java int` Default: `#!java 1920`

Screen width in lwjgl instrumentation.

#### hmc.lwjgl.screenheight
Type: `#!java int` Default: `#!java 1080`

Screen width in lwjgl instrumentation.

#### hmc.lwjgl.refreshrate
Type: `#!java int` Default: `#!java 100`

Refresh rate in lwjgl instrumentation.

#### hmc.lwjgl.bitsperpixel
Type: `#!java int` Default: `#!java 32`

Bits per pixel in lwjgl instrumentation.

#### hmc.lwjgl.nativejniversion
Type: `#!java int` Default: `#!java 24`

JNI version in lwjgl instrumentation.

#### hmc.lwjgl.no.awt
Type: `#!java boolean` Default: `#!java false`

If `java.awt` is not available, e.g. on Android, this is required.

#### hmc.main.class
Type: `#!java String` Default: `#!java "net.minecraft.client.main.Main"`

Allows you to specify another main class to use in the Minecraft jar to launch.

#### hmc.main_method
Type: `#!java String` Default: `#!java "net.minecraft.client.main.Main"`

The actual main class used.
This is e.g. needed when wrapping the game with the hmc-runtime.

#### hmc.mcdir
Type: `#!java String` Default: `#!java ".minecraft"`

The `.minecraft` directory.
This is where assets and libraries are stored.

#### hmc.no.auto.config
Type: `#!java boolean` Default: `#!java false`

Does not run the AutoConfiguration step to discover available Java versions when `#!java true`.

#### hmc.offline
Type: `#!java boolean` Default: `#!java false`

Enables offline mode.

#### hmc.offline.token
Type: `#!java String` Default: `#!java ""`

Token to use for the offline account.

#### hmc.offline.type
Type: `#!java String` Default: `#!java "msa"`

Type to use for the offline account.

#### hmc.offline.username
Type: `#!java String` Default: `#!java "Offline"`

Username to use for the offline account.

#### hmc.offline.uuid
Type: `#!java UUID` Default: `#!java "22689332a7fd41919600b0fe1135ee34"`

UUID to use for the offline account.

#### hmc.osarch
Type: `#!java boolean` Default: `#!java null`

HeadlessMc will detect your operating system properties.
With these properties you can override what HeadlessMc detects.
`true` if your operating system is 64-bit. `false` if 32.

#### hmc.osarchitecture
Type: `#!java String` Default: `#!java null`

HeadlessMc will detect your operating system properties.
With these properties you can override what HeadlessMc detects.
Allows you to override your OS architecture.

#### hmc.osname
Type: `#!java String` Default: `#!java null`

HeadlessMc will detect your operating system properties.
With these properties you can override what HeadlessMc detects.
Allows you to override your OS name.

#### hmc.ostype
Type: `#!java String` Default: `#!java null`

HeadlessMc will detect your operating system properties.
With these properties you can override what HeadlessMc detects.
Allows you to override your OS type (LINUX, WINDOWS, MACOS).

#### hmc.osversion
Type: `#!java String` Default: `#!java null`

HeadlessMc will detect your operating system properties.
With these properties you can override what HeadlessMc detects.
Allows you to override your OS version.

#### hmc.password
Type: `#!java String` Default: `#!java null`

Specify [hmc.email](#hmcemail) and [hmc.password](#hmcpassword),
to log into an account automatically when the launcher starts.

#### hmc.profileproperties
Type: `#!java Map` Default: `#!java "{}"`

`${profile_properties}` game argument for minecraft.

#### hmc.rethrow.launch.exceptions
Type: `#!java boolean` Default: `#!java false`

Throws all exceptions from launching further.
For testing.

#### hmc.server.accept.eula
Type: `#!java boolean` Default: `#!java false`

Automatically accepts Server EULAs.

#### hmc.server.args
Type: `#!java String` Default: `#!java "nogui"`

` ` delimited list of arguments to start the Minecraft server with.
Equivalent to `--game-args` in the `server launch` command.

#### hmc.server.launch.for.eula
Type: `#!java boolean` Default: `#!java true`

Automatically launches the Server to get the EULA file if it is not present.

#### hmc.server.test
Type: `#!java boolean` Default: `#!java false`

Runs the HeadlessMc server test when launching the server.

#### hmc.server.test.build
Type: `#!java String` Default: `#!java ""`

[hmc.server.test.dir](#hmcservertestdir) allows you to create a test server.
However, HeadlessMc cannot not know which server type is in this directory.
With these properties you can specify the information about the server type.

#### hmc.server.test.cache
Type: `#!java boolean` Default: `#!java false`

Enables the `server cache` command for caching downloaded servers.

#### hmc.server.test.cache.use.mc.dir
Type: `#!java boolean` Default: `#!java false`

Caches servers in `.minecraft`.

#### hmc.server.test.dir
Type: `#!java boolean` Default: `#!java false`

The directory to run the test server in.

#### hmc.server.test.dir
Type: `#!java boolean` Default: `#!java false`

The directory to run the test server in.
Usually which directories servers are stored in is handled internally by HeadlessMc.
This way you can specify the directory to store a server in.

#### hmc.server.test.name
Type: `#!java String` Default: `#!java ""`

[hmc.server.test.dir](#hmcservertestdir) allows you to create a test server.
However, HeadlessMc cannot not know which server type is in this directory.
With these properties you can specify the information about the server type.

#### hmc.server.test.type
Type: `#!java String` Default: `#!java ""`

[hmc.server.test.dir](#hmcservertestdir) allows you to create a test server.
However, HeadlessMc cannot not know which server type is in this directory.
With these properties you can specify the information about the server type.

#### hmc.server.test.version
Type: `#!java String` Default: `#!java ""`

[hmc.server.test.dir](#hmcservertestdir) allows you to create a test server.
However, HeadlessMc cannot not know which server type is in this directory.
With these properties you can specify the information about the server type.

#### hmc.set.library.dir
Type: `#!java boolean` Default: `#!java true`

Sets the system property `libraryDirectory` to `.minecraft/libraries`.
This is needed for a fabric (or forge? TODO)

#### hmc.store.accounts
Type: `#!java boolean` Default: `#!java true`

Stores accounts in `HeadlessMc/auth/.accounts.json`.

#### hmc.test.filename
Type: `#!java String` Default: `#!java null`

Path to a file containing a command test.
If specified the command test will run against server/client.

#### hmc.test.leave.after
Type: `#!java boolean` Default: `#!java true`

Quits HeadlessMc after running a command test.

#### hmc.test.no.timeout
Type: `#!java boolean` Default: `#!java false`

Disables the test timeout if `true`.

#### hmc.tweaker.main.class
Type: `#!java String` Default: `#!java "net.minecraft.client.main.Main"`

When using the headlessmc-lwjgl jar as a LaunchWrapper Tweaker.

#### hmc.userproperties
Type: `#!java Map` Default: `#!java "{}"`

`${user_properties}` game argument for minecraft.

#### hmc.wrapper.close.classloader
Type: `#!java boolean` Default: `#!java true`

Closes the Wrapper classloader after use.
For debugging purposes.

#### hmc.wrapper.debug.classes
Type: `#!java boolean` Default: `#!java false`

Outputs classes loaded by the HeadlessMcWrapper.

#### hmc.wrapper.garbage.collect.hmc
Type: `#!java boolean` Default: `#!java false`

Enables a hack that quits and garbage collects the launcher once the game has started,
but still listens to the running process.

#### hmc.wrapper.wrapped.main
Type: `#!java String` Default: `#!java "io.github.headlesshq.headlessmc.launcher.Main"`

The Main method to call from the wrapper.

#### hmc.xuid
Type: `#!java String` Default: `#!java ""`

`${auth_xuid}` game argument for minecraft.
Probably used by Mojang for telemetry.
