package io.github.headlesshq.headlessmc.launcher.backup;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Creates and manages backups of everything a {@link BackupProvider} can back up.
 */
public interface BackupService {
    /**
     * Creates a new backup.
     * Automatic backups delete older automatic backups of the same target, see {@link BackupConfig#autoKeep()}.
     *
     * @param request what to back up.
     * @return the created backup.
     * @throws IllegalArgumentException if there is nothing to back up.
     * @throws BackupInUseException     if the files are in use and the request does not force the backup.
     */
    Backup create(BackupRequest request);

    /**
     * @return all backups, sorted from oldest to newest.
     */
    List<Backup> list();

    /**
     * @param provider the name of the provider, or {@code null} for all providers.
     * @param target   the target, or {@code null} for all targets.
     * @return the matching backups, sorted from oldest to newest.
     */
    List<Backup> list(@Nullable String provider, @Nullable String target);

    /**
     * @param id the id of the backup.
     * @return the backup with the given id.
     */
    Optional<Backup> get(String id);

    /**
     * Compares the given backup with the files currently on disk.
     *
     * @param backup   the backup to compare.
     * @param checksum whether to compare the contents of files whose size and modification time are unchanged.
     * @return the difference between the backup and the files on disk.
     */
    BackupDiff diff(Backup backup, boolean checksum);

    /**
     * Restores the given backup.
     * Afterwards the backed up {@link Backup#paths()} match the backup again:
     * removed and modified files are restored and, unless {@link RestoreOptions#keepNew()},
     * files added since the backup are deleted. Ignored files are kept.
     *
     * @param backup  the backup to restore.
     * @param options how to restore the backup.
     * @return the reverted changes and the backup created before restoring, if any.
     * @throws BackupInUseException if the files are in use and the options do not force the restore.
     */
    RestoreResult restore(Backup backup, RestoreOptions options);

    /**
     * Deletes the given backup.
     *
     * @param backup the backup to delete.
     */
    void remove(Backup backup);

    /**
     * Deletes the backups selected by the given request.
     *
     * @param request the backups to delete.
     * @return the deleted backups.
     */
    List<Backup> prune(PruneRequest request);

    /**
     * @param backup the backup.
     * @return the zip archive containing the files of the backup.
     */
    Path getArchive(Backup backup);

    /**
     * @return the directory backups are placed in.
     */
    Path getBackupDir();

}
