package io.github.headlesshq.headlessmc.updater;

import java.util.Optional;

/**
 * Checks for updates of something HeadlessMc manages, e.g. HeadlessMc itself or installed Java runtimes.
 * All {@code Updater} beans are run by the {@link UpdateNotifier}:
 * asynchronously when the shell starts and synchronously before a
 * {@link io.github.headlesshq.headlessmc.console.LongRunningCommand} that HeadlessMc has been started with.
 * Implementations should never throw, but log failures and return an empty message.
 */
public interface Updater {
    /**
     * @return a short, unique, lower case name for this updater, e.g. {@code java}, also used to name its cache file.
     */
    String getName();

    /**
     * @return {@code true} if this updater should run. All updaters can be disabled with {@link UpdaterConfig#enabled()}.
     */
    boolean isEnabled();

    /**
     * Checks remote sources for updates and caches the result.
     *
     * @return a message telling the user about available updates and how to install them, or empty if there are none.
     */
    Optional<String> checkForUpdates();

    /**
     * Like {@link #checkForUpdates()}, but only requests remote sources if the cached result is older than
     * {@link UpdaterConfig#intervalHours()}, otherwise the cached result is used.
     *
     * @return a message telling the user about available updates and how to install them, or empty if there are none.
     */
    Optional<String> checkForUpdatesIfStale();

}
