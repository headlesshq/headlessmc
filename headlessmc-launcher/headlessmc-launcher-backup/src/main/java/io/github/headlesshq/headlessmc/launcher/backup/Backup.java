package io.github.headlesshq.headlessmc.launcher.backup;

import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Metadata of a backup, stored as json next to the zip archive containing the backed up files.
 *
 * @param id         the unique id of the backup.
 * @param provider   the {@link BackupProvider#getName() name} of the provider that created the backup.
 * @param target     what was backed up, e.g. the name of a profile.
 * @param type       what the backup contains, one of the {@link BackupProvider#getTypes() types} of the provider.
 * @param origin     what created the backup.
 * @param created    when the backup was created.
 * @param profile    the profile the backup was created from, as it was when the backup was created,
 *                   or {@code null} if the backup does not belong to a profile.
 * @param source     the directory the files were backed up from.
 * @param paths      the directories, relative to {@code source}, that were backed up.
 *                   An empty string stands for the entire {@code source} directory.
 * @param ignored    glob patterns of paths, relative to {@code source}, that were left out of the backup,
 *                   see {@link BackupConfig#ignore()}.
 * @param archive    the file name of the zip archive, relative to this metadata file.
 * @param files      the amount of files in the backup.
 * @param size       the uncompressed size of all files in the backup in bytes.
 * @param hmcVersion the version of the backup format, see {@link #BACKUP_VERSION}.
 */
@RegisterForReflection
public record Backup(
    String id,
    String provider,
    String target,
    String type,
    BackupOrigin origin,
    Instant created,
    @Nullable Profile profile,
    Path source,
    List<String> paths,
    List<String> ignored,
    String archive,
    int files,
    long size,
    int hmcVersion
) implements ReflectionRegistered {
    public static final int BACKUP_VERSION = 0;

    /**
     * @return the files this backup was created from.
     */
    public BackupScope scope() {
        return new BackupScope(source, paths, ignored);
    }

}
