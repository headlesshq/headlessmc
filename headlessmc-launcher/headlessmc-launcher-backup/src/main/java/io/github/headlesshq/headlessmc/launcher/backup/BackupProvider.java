package io.github.headlesshq.headlessmc.launcher.backup;

import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Something that can be backed up, e.g. profiles and servers.
 * Implement this interface as a bean to make a new kind of backup available,
 * the backup command selects providers by their {@link #getName() name} with {@code --provider}.
 * Providers only decide what to back up, the {@link BackupService} archives, compares and restores the files.
 */
public interface BackupProvider {
    /**
     * @return the unique name of this provider, e.g. {@code profile}.
     */
    String getName();

    /**
     * @return a short description of what this provider backs up.
     */
    String getDescription();

    /**
     * @return the types of backups this provider supports, the first one is the default.
     */
    List<String> getTypes();

    /**
     * @return the targets that can be backed up, e.g. for completions.
     */
    List<String> getTargets();

    /**
     * Resolves what to back up for the given request.
     *
     * @param request the request to resolve.
     * @return what to back up.
     * @throws IllegalArgumentException if the request cannot be resolved, e.g. if the target does not exist.
     */
    BackupPlan plan(BackupRequest request);

    /**
     * Resolves what to back up to create a backup of the same files as the given backup, as they are now.
     * Used to back up the files on disk before restoring the given backup.
     *
     * @param backup the backup to back up the files of.
     * @return what to back up.
     */
    default BackupPlan replan(Backup backup) {
        return new BackupPlan(backup.target(), backup.type(), backup.profile(), backup.source(), backup.paths(), List.of());
    }

    /**
     * Checks if the given files are in use, e.g. by a running game, so they should not be backed up or restored.
     *
     * @param scope   the files to check.
     * @param profile the profile the files belong to, if any.
     * @return why the files are in use, or an empty {@link Optional} if they are not.
     */
    Optional<String> getUsage(BackupScope scope, @Nullable Profile profile);

    /**
     * @param backup the backup to compare with the files on disk.
     * @return the paths, relative to {@link Backup#source()}, to compare.
     */
    default List<String> getDiffPaths(Backup backup) {
        return backup.paths();
    }

    /**
     * Called after the files of the given backup have been restored.
     *
     * @param backup the restored backup.
     */
    default void afterRestore(Backup backup) {

    }

}
