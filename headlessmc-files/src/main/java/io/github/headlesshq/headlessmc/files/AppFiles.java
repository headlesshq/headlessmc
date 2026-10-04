package io.github.headlesshq.headlessmc.files;

import java.nio.file.Path;

/**
 * Represents locations HeadlessMc places files into.
 */
public interface AppFiles {
    /**
     * @return the directory config.properties and profiles are placed into.
     */
    Path getConfigDir();

    /**
     * @return the directory data (accounts, JREs, game files) are placed into.
     */
    Path getDataDir();

    /**
     * @return the directory files to cache are placed into.
     */
    Path getCacheDir();

    /**
     * @return the directory state files, e.g. logs or history, are placed into.
     */
    Path getStateDir();

    /**
     * @return the directory to place log files in.
     */
    default Path getLogDir() {
        return getStateDir().resolve("logs");
    }

    /**
     * @return the directory to place authentication related files (accounts etc.) in.
     */
    default Path getAuthDir() {
        return getDataDir().resolve(".auth");
    }

    /**
     * @return the directory to place Java Runtimes in.
     */
    default Path getJavaDir() {
        return getDataDir().resolve("java");
    }

    /**
     * @return the directory to place plugin jars in.
     */
    default Path getPluginDir() {
        return getDataDir().resolve("plugins");
    }

    /**
     * @return the directory server game files for each server profile are placed in.
     */
    default Path getServerDir() {
        return getDataDir().resolve("server");
    }

    /**
     * @return the directory launching profiles are placed in.
     */
    default Path getProfilesDir() {
        return getConfigDir().resolve("profiles");
    }

    /**
     * @return the directory native files placed in.
     */
    default Path getNatives() {
        return getCacheDir().resolve("natives");
    }

}
