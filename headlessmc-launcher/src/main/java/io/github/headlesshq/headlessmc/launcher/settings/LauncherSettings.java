package io.github.headlesshq.headlessmc.launcher.settings;

import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.settings.NullableSettingKey;
import io.github.headlesshq.headlessmc.api.settings.SettingGroup;
import io.github.headlesshq.headlessmc.api.settings.SettingGroupInitializer;
import io.github.headlesshq.headlessmc.api.settings.SettingKey;
import io.github.headlesshq.headlessmc.launcher.files.MinecraftFinder;
import io.github.headlesshq.headlessmc.os.OSSettings;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.nio.file.Path;
import java.util.List;

@Getter
@ApplicationScoped
@Accessors(fluent = true)
public class LauncherSettings extends SettingGroupInitializer {
    private final OSSettings os;

    private final SettingGroup group = getRoot().group("hmc.launcher", "Launcher configuration.");

    private final SettingKey<Path> mcDir = group.setting(Path.class)
            .withName("hmc.launcher.mcDir")
            .withAlias("hmc.mcdir")
            .withDescription("Where to place assets, libraries and versions.")
            .withValue(config -> new MinecraftFinder().mkdir(os().getOS(config)))
            .build();

    private final NullableSettingKey<Path> gameDir = group.setting(Path.class)
            .withName("hmc.launcher.gameDir")
            .withAlias("hmc.gamedir")
            .withDescription("All game instances will be launched inside this directory if set. Generally not recommended to use.")
            .nullable();

    private final SettingKey<List<String>> jvmArgs = group.list(String.class)
            .withName("hmc.launcher.jvmArgs")
            .withAlias("hmc.jvmargs")
            .withDescription("A space seperated list of JVM arguments to apply when launching the game.")
            .build();

    private final SettingKey<List<String>> gameArgs = group.list(String.class)
            .withName("hmc.launcher.gameArgs")
            .withAlias("hmc.gameargs")
            .withDescription("A space seperated list of game arguments to add when launching the game.")
            .build();

    private final NullableSettingKey<String> customMainClass = group.setting(String.class)
            .withName("hmc.launcher.customMainClass")
            .withAlias("hmc.main.class")
            .withDescription("Launch a custom main class.")
            .nullable();

    private final NullableSettingKey<String> classPath = group.setting(String.class)
            .withName("hmc.launcher.additionalClassPath")
            .withAlias("hmc.additional.classpath")
            .withDescription("Add additional Jars to the game classpath (Only for developers).")
            .nullable();

    /* TODO: mainClass private final NullableSettingKey<String> mainClass = group.setting(String.class)
            .withName("hmc.launcher.additionalClassPath")
            .withAlias("hmc.additional.classpath")
            .withDescription("All game instances will be launched inside this directory if set. Generally not recommended.")
            .nullable();*/

    private final SettingKey<String> launcherName = group.setting(String.class)
            .withName("hmc.launcher.name")
            .withAlias("hmc.launchername")
            .withDescription("Name of the launcher to pass to the Minecraft client.")
            .withValue(HeadlessMc.NAME)
            .build();

    private final SettingKey<String> launcherVersion = group.setting(String.class)
            .withName("hmc.launcher.version")
            .withAlias("hmc.launcherversion")
            .withDescription("Version of the launcher to pass to HeadlessMc.")
            .withValue(HeadlessMc.VERSION)
            .build();

    private final NullableSettingKey<String> email = group.setting(String.class)
            .withName("hmc.launcher.email")
            .withAlias("hmc.email")
            .withDescription("Set hmc.email and hmc.password to login into an account automatically.")
            .nullable();

    private final NullableSettingKey<String> password = group.setting(String.class)
            .withName("hmc.launcher.password")
            .withAlias("hmc.password")
            .withDescription("Set hmc.email and hmc.password to login into an account automatically.")
            .nullable();

    private final SettingKey<Boolean> keepFiles = group.setting(Boolean.class)
            .withName("hmc.launcher.keepFiles")
            .withAlias("hmc.keepfiles")
            .withDescription("Set hmc.email and hmc.password to login into an account automatically.")
            .withValue(false)
            .build();

    private final SettingKey<Boolean> storeAccounts = group.setting(Boolean.class)
            .withName("hmc.launcher.storeAccounts")
            .withAlias("hmc.store.accounts")
            .withDescription("If HeadlessMc should store accounts.")
            .withValue(true)
            .build();

    // TODO: decide about invert and always flags

    private final SettingKey<Boolean> jomlNoUnsafe = group.setting(Boolean.class)
            .withName("hmc.launcher.disableJomlUnsafe")
            .withAlias("hmc.joml.no.unsafe")
            .withDescription("Prevents crashes by preventing usage of Unsafe by joml when launching in headless mode.")
            .withValue(true)
            .build();

    private final SettingKey<String> xuid = group.setting(String.class)
            .withName("hmc.launcher.xuid")
            .withAlias("hmc.xuid")
            .withDescription("XUID to pass to the Minecraft client, is used for telemetry by Mojang.")
            .withValue("")
            .build();

    private final SettingKey<String> clientId = group.setting(String.class)
            .withName("hmc.launcher.clientId")
            .withAlias("hmc.clientId")
            .withDescription("ClientId to pass to the Minecraft client, is used for telemetry by Mojang.")
            .withValue("")
            .build();

    private final SettingKey<String> userProperties = group.setting(String.class)
            .withName("hmc.launcher.userProperties")
            .withAlias("hmc.userproperties")
            .withDescription("UserProperties to pass to the Minecraft client.")
            .withValue("{}")
            .build();

    private final SettingKey<String> profileProperties = group.setting(String.class)
            .withName("hmc.launcher.profileProperties")
            .withAlias("hmc.profileproperties")
            .withDescription("ProfileProperties to pass to the Minecraft client.")
            .withValue("{}")
            .build();

    // TODO: build fabric URL into FabricCOmmand
    // TODO: generally generic way for settings to replace command option values!

    private final SettingKey<Boolean> rethrowLaunchExceptions = group.setting(Boolean.class)
            .withName("hmc.launcher.rethrowLaunchExceptions")
            .withAlias("hmc.rethrow.launch.exceptions")
            .withDescription("Crashes on failure to launch the game (For CI).")
            .withValue(false)
            .build();

    private final SettingKey<Boolean> autoDownload = group.setting(Boolean.class)
            .withName("hmc.launcher.autoDownload")
            .withAlias("hmc.auto.download.versions")
            .withDescription("Automatically downloads the version.json files required to launch the game.")
            .withValue(true)
            .build();

    private final SettingKey<Boolean> setLibraryDir = group.setting(Boolean.class)
            .withName("hmc.launcher.setLibraryDir")
            .withAlias("hmc.set.library.dir")
            .withDescription("Adds the -DlibraryDirectory system property when launching.")
            .withValue(true)
            .build();

    private final SettingKey<Boolean> noAutoConfig = group.setting(Boolean.class)
            .withName("hmc.launcher.noAutoConfig")
            .withAlias("hmc.no.auto.config")
            .withDescription("Does not run the auto configuration step when launching the first time.")
            .withValue(false)
            .build();

    private final SettingKey<Boolean> inMemory = group.setting(Boolean.class)
            .withName("hmc.launcher.inMemory")
            .withAlias("hmc.in.memory")
            .withDescription("Always launches the game in-memory. Analog to the --in-memory flag.")
            .withValue(false)
            .build();

    private final SettingKey<Boolean> inMemoryRequireCorrectJava = group.setting(Boolean.class)
            .withName("hmc.launcher.inMemoryRequireCorrectJava")
            .withAlias("hmc.in.memory.require.correct.java")
            .withDescription("Requires the current Java version to be the correct version for in-memory launching.")
            .withValue(true)
            .build();

    private final NullableSettingKey<Integer> assumedJavaVersion = group.setting(Integer.class)
            .withName("hmc.launcher.assumedJavaVersion")
            .withAlias("hmc.assumed.java.version")
            .withDescription("Overrides the current Java version.")
            .nullable();

    private final SettingKey<Boolean> refreshOnLaunch = group.setting(Boolean.class)
            .withName("hmc.launcher.refreshAccountOnLaunch")
            .withAlias("hmc.account.refresh.on.launch")
            .withDescription("Refreshes your account token when launching HeadlessMc.")
            .withValue(false)
            .build();

    private final SettingKey<Boolean> refreshOnGameLaunch = group.setting(Boolean.class)
            .withName("hmc.launcher.refreshOnGameLaunch")
            .withAlias("hmc.account.refresh.on.game.launch")
            .withDescription("Refreshes your account token when launching the game.")
            .withValue(true)
            .build();

    // TODO: get rid of this?
    private final NullableSettingKey<String> extractedFileCacheUUID = group.setting(String.class)
            .withName("hmc.launcher.extractedFileCacheUUID")
            .withAlias("hmc.extracted.file.cache.uuid")
            .withDescription("Control where HeadlessMc places the extracted files for the game.")
            .nullable();

    private final SettingKey<Boolean> httpUserAgentEnabled = group.setting(Boolean.class)
            .withName("hmc.launcher.httpUserAgentEnabled")
            .withAlias("hmc.http.user.agent.enabled")
            .withDescription("Adds a user agent to http requests when downloading files.")
            .withValue(true)
            .build();

    private final SettingKey<String> httpUserAgent = group.setting(String.class)
            .withName("hmc.launcher.httpUserAgent")
            .withAlias("hmc.http.user.agent")
            .withDescription("Name of the user agent to add to http requests.")
            .withValue("Mozilla/5.0")
            .build();

    private final SettingKey<Boolean> gameDirForEachVersion = group.setting(Boolean.class)
            .withName("hmc.launcher.gameDirForEachVersion")
            .withAlias("hmc.game.dir.for.each.version")
            .withDescription("Creates a separate game directory for each version.")
            .withValue(false)
            .build();

    private final SettingKey<Boolean> installLogging = group.setting(Boolean.class)
            .withName("hmc.launcher.installLogging")
            .withAlias("hmc.install.mc.logging")
            .withDescription("Respects the logging configuration in mc version.json files, this might break console logging, but could add a layer of Log4J protection.")
            .withValue(false)
            .build();

    private final SettingKey<Boolean> checkXvfb = group.setting(Boolean.class)
            .withName("hmc.launcher.checkXvfb")
            .withAlias("hmc.check.xvfb")
            .withDescription("Allows to launch the game in non-headless mode when offline, but only when xvfb is running.")
            .withValue(false)
            .build();

    private final SettingKey<Boolean> crashReportWatcher = group.setting(Boolean.class)
            .withName("hmc.launcher.crashReportWatcher")
            .withAlias("hmc.crash.report.watcher")
            .withDescription("For CI/CD testing: kills the game process if a crash-report is created. Some modloaders display a crash-report window indefinitely when crashed, causing the process to not exit.")
            .withValue(false)
            .build();

    private final SettingKey<Boolean> crashReportWatcherExit = group.setting(Boolean.class)
            .withName("hmc.launcher.crashReportWatcherExit")
            .withAlias("hmc.crash.report.watcher.exit")
            .withDescription("Exits with code -1 if the crash report watcher detects a crash.")
            .withValue(true)
            .build();

    private final NullableSettingKey<Path> testFile = group.setting(Path.class)
            .withName("hmc.launcher.testFile")
            .withAlias("hmc.test.filename")
            .withDescription("Path to a test file that contains a test in the HeadlessMc test json schema.")
            .nullable();

    private final SettingKey<Boolean> leaveAfterTest = group.setting(Boolean.class)
            .withName("hmc.launcher.leaveAfterTest")
            .withAlias("hmc.test.leave.after")
            .withDescription("Exits after a successful test (with this set to false, you can also use tests to configure a game process with commands).")
            .withValue(true)
            .build();

    private final SettingKey<Boolean> noTestTimeout = group.setting(Boolean.class)
            .withName("hmc.launcher.noTestTimeout")
            .withAlias("hmc.test.no.timeout")
            .withDescription("Does not timeout tests after their configured timeout has run out.")
            .withValue(false)
            .build();

    private final SettingKey<Boolean> autoDownloadSpecifics = group.setting(Boolean.class)
            .withName("hmc.launcher.autoDownloadSpecifics")
            .withAlias("hmc.auto.download.specifics")
            .withDescription("Automatically downloads the hmc-specifics for the version that is being launched.")
            .withValue(false)
            .build();

    private final SettingKey<Boolean> fixArmLibraries = group.setting(Boolean.class)
            .withName("hmc.launcher.fixArmLibraries")
            .withAlias("hmc.arm.fix.libraries")
            .withDescription("Mojang does not provide ARM support for linux. This attempts to download the missing ARM libraries from maven central.")
            .withValue(true)
            .build();

    @Inject
    public LauncherSettings(SettingGroup group, OSSettings os) {
        super(group);
        this.os = os;
    }

}
