package io.github.headlesshq.headlessmc.launcher;

import io.github.headlesshq.headlessmc.api.Application;
import io.github.headlesshq.headlessmc.java.download.JavaDownloaderManager;
import io.github.headlesshq.headlessmc.launcher.auth.AccountManager;
import io.github.headlesshq.headlessmc.launcher.command.download.VersionInfo;
import io.github.headlesshq.headlessmc.launcher.command.download.VersionInfoCache;
import io.github.headlesshq.headlessmc.launcher.download.ChecksumService;
import io.github.headlesshq.headlessmc.launcher.download.DownloadService;
import io.github.headlesshq.headlessmc.launcher.files.ConfigService;
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

public interface ILauncher extends Application {
    /**
     * Used for finding and managing Minecraft {@link Version}s.
     */
    VersionService getVersionService();

    /**
     * The directories the HeadlessMc launcher will work in.
     */
    LauncherConfig getLauncherConfig();

    /**
     * A service for checking SHA1 hashes.
     */
    ChecksumService getChecksumService();

    /**
     * A service for making GET requests.
     */
    DownloadService getDownloadService();

    /**
     * Actual Launching happens here.
     */
    ProcessFactory getProcessFactory();

    /**
     * Manages the HeadlessMc configuration.
     */
    ConfigService getConfigService();

    /**
     * Manages the Java versions we use for launching.
     */
    JavaService getJavaService();

    /**
     * Manages accounts and authentication.
     */
    AccountManager getAccountManager();

    /**
     * Manages and downloads version specific mods like the HeadlessMc-Specifics.
     */
    VersionSpecificModManager getVersionSpecificModManager();

    /**
     * Manages Plugins for the HeadlessMc launcher.
     */
    PluginManager getPluginManager();

    /**
     * Manages the downloading of Java.
     */
    JavaDownloaderManager getJavaDownloaderManager();

    /**
     * Manages Servers.
     */
    ServerManager getServerManager();

    /**
     * Cache for vanilla {@link VersionInfo}s.
     */
    VersionInfoCache getVersionInfoCache();

    /**
     * Manages {@link Mod}s.
     */
    ModManager getModManager();

}
