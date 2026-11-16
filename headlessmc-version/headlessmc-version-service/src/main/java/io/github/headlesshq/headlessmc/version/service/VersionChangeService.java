package io.github.headlesshq.headlessmc.version.service;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.version.Version;

import java.util.List;

/**
 * Observes which versions an action installs into a {@code .minecraft} directory.
 */
public interface VersionChangeService {
    /**
     * Runs the given action and reports the versions it added to the given {@code .minecraft} directory.
     * Only that directory is observed: a version with the same name that is already installed in another
     * directory does not hide the change, and changes in other directories are not reported.
     *
     * @param mcFiles the {@code .minecraft} directory the action installs into.
     * @param action  the action to run.
     * @return the versions that are installed in the directory after the action, but were not before.
     * @throws HeadlessMcException if the action fails, or the versions cannot be listed or read.
     */
    List<Version> observeChanges(McFiles mcFiles, ChangeAction action) throws HeadlessMcException;

    @FunctionalInterface
    interface ChangeAction {
        void run() throws HeadlessMcException;
    }

}
