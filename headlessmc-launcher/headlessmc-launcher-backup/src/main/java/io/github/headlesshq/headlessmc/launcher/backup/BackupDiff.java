package io.github.headlesshq.headlessmc.launcher.backup;

import java.util.List;

/**
 * The difference between a {@link Backup} and the files currently on disk.
 * All paths are relative to the {@link Backup#source()} and use {@code /} as separator.
 *
 * @param added    files that exist on disk, but not in the backup.
 * @param removed  files that exist in the backup, but not on disk anymore.
 * @param modified files that exist in both, but whose contents differ.
 */
public record BackupDiff(List<String> added, List<String> removed, List<String> modified) {
    /**
     * @return {@code true} if the files on disk are the same as in the backup.
     */
    public boolean isEmpty() {
        return added.isEmpty() && removed.isEmpty() && modified.isEmpty();
    }

}
