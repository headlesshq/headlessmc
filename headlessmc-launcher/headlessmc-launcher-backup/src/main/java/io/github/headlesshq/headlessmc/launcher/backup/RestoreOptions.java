package io.github.headlesshq.headlessmc.launcher.backup;

/**
 * Options for {@link BackupService#restore(Backup, RestoreOptions)}.
 *
 * @param backupFirst whether to back up the files that are about to be changed before restoring.
 * @param keepNew     whether to keep files that were added since the backup, instead of deleting them.
 * @param force       whether to restore even if the files are in use, e.g. by a running game.
 */
public record RestoreOptions(boolean backupFirst, boolean keepNew, boolean force) {
    /**
     * Backs up the files first, deletes new files and refuses to restore files that are in use.
     */
    public static final RestoreOptions DEFAULT = new RestoreOptions(true, false, false);

}
