package io.github.headlesshq.headlessmc.launcher.backup;

import org.jspecify.annotations.Nullable;

/**
 * The result of {@link BackupService#restore(Backup, RestoreOptions)}.
 *
 * @param changes      the changes that were reverted, files that were added since the backup
 *                     have been deleted, removed and modified files have been restored.
 * @param safetyBackup the backup of the files on disk that was created before restoring,
 *                     or {@code null} if none was created.
 */
public record RestoreResult(BackupDiff changes, @Nullable Backup safetyBackup) {

}
