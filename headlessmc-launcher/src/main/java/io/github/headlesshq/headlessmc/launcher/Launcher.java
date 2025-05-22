package io.github.headlesshq.headlessmc.launcher;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.java.download.JavaDownloaderManager;
import io.github.headlesshq.headlessmc.launcher.auth.AccountManager;
import io.github.headlesshq.headlessmc.launcher.command.download.VersionInfo;
import io.github.headlesshq.headlessmc.launcher.command.download.VersionInfoCache;
import io.github.headlesshq.headlessmc.launcher.download.ChecksumService;
import io.github.headlesshq.headlessmc.launcher.download.DownloadService;
import io.github.headlesshq.headlessmc.launcher.files.ConfigService;
import io.github.headlesshq.headlessmc.launcher.files.FileManager;
import io.github.headlesshq.headlessmc.launcher.files.LauncherConfig;
import io.github.headlesshq.headlessmc.launcher.java.JavaService;
import io.github.headlesshq.headlessmc.launcher.launch.ProcessFactory;
import io.github.headlesshq.headlessmc.launcher.mods.Mod;
import io.github.headlesshq.headlessmc.launcher.mods.ModManager;
import io.github.headlesshq.headlessmc.launcher.plugin.PluginManager;
import io.github.headlesshq.headlessmc.launcher.server.ServerManager;
import io.github.headlesshq.headlessmc.launcher.specifics.VersionSpecificModManager;
import io.github.headlesshq.headlessmc.launcher.version.Version;
import io.github.headlesshq.headlessmc.launcher.version.VersionService;

/**
 * Represents the HeadlessMc launcher and everything that belongs to it.
 */
@Getter
@Setter
@AllArgsConstructor
public class Launcher implements HeadlessMc {
    /**
     * The current version of HeadlessMc.
     */
    public static final String VERSION = HeadlessMc.VERSION;

    /**
     * A HeadlessMc instance that this Launcher delegates to for CommandLine, ExitManager, etc..
     */
    @Delegate
    private HeadlessMc headlessMc;
    /**
     * Used for finding and managing Minecraft {@link Version}s.
     */
    private VersionService versionService;
    /**
     * The directories the HeadlessMc launcher will work in.
     */
    private final LauncherConfig launcherConfig;
    /**
     * A service for checking SHA1 hashes.
     */
    private ChecksumService sha1Service;
    /**
     * A service for making GET requests.
     */
    private DownloadService downloadService;
    /**
     * Actual Launching happens here.
     */
    private ProcessFactory processFactory;
    /**
     * Manages the HeadlessMc configuration.
     */
    private ConfigService configService;
    /**
     * Manages the Java versions we use for launching.
     */
    private JavaService javaService;
    /**
     * Manages accounts and authentication.
     */
    private AccountManager accountManager;
    /**
     * Manages and downloads version specific mods like the HeadlessMc-Specifics.
     */
    private VersionSpecificModManager versionSpecificModManager;
    /**
     * Manages Plugins for the HeadlessMc launcher.
     */
    private PluginManager pluginManager;
    /**
     * Manages the downloading of Java.
     */
    private JavaDownloaderManager javaDownloaderManager;
    /**
     * Manages Servers.
     */
    private ServerManager serverManager;
    /**
     * Cache for vanilla {@link VersionInfo}s.
     */
    private VersionInfoCache versionInfoCache;
    /**
     * Manages {@link Mod}s.
     */
    private ModManager modManager;

    /**
     * The FileManager managing the HeadlessMc config, log and other files.
     * This is a convenience method that calls {@link LauncherConfig} and by extension {@link ConfigService}.
     *
     * @return the FileManager managing the HeadlessMc config, log and other files.
     * @see LauncherConfig
     * @see ConfigService
     */
    public FileManager getFileManager() {
        return launcherConfig.getFileManager();
    }

    /**
     * The .minecraft directory in which we store assets, libraries and versions.
     * This is a convenience method that calls {@link LauncherConfig}.
     * 
     * @return the .minecraft directory in which we store assets, libraries and versions.
     * @see LauncherConfig
     */
    public FileManager getMcFiles() {
        return launcherConfig.getMcFiles();
    }

    /**
     * Returns the default game directory.
     * 
     * @return the default game directory.
     * @deprecated call {@link #getGameDir(Version)} or {@link LauncherConfig#getGameDir(String)} instead.
     */
    @Deprecated
    public FileManager getGameDir() {
        return launcherConfig.getGameDir();
    }

    /**
     * Gets the directory to run the game in for a specific version.
     * This is a convenience method that calls {@link LauncherConfig#getGameDir(Version)}.
     *
     * @param version the version to get a game directory for.
     * @return a FileManager managing the game dir for this version.
     * @see LauncherConfig#getGameDir(Version) 
     */
    public FileManager getGameDir(Version version) {
        return launcherConfig.getGameDir(version);
    }

}
