package io.github.headlesshq.headlessmc.launcher.backup;

import io.github.headlesshq.headlessmc.launcher.profile.LegacyProfile;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * A {@link Backup} in format 0, see {@link Backup#BACKUP_VERSION}.
 * It differs from the current format only in the profile it contains,
 * which is a profile from before profiles had launch targets.
 * It is only read, to be converted with {@link #migrate()}.
 */
@RegisterForReflection
record LegacyBackup(
    String id,
    String provider,
    String target,
    String type,
    BackupOrigin origin,
    Instant created,
    @Nullable LegacyProfile profile,
    Path source,
    List<String> paths,
    List<String> ignored,
    String archive,
    int files,
    long size,
    int hmcVersion
) implements ReflectionRegistered {
    /**
     * @return this backup in the current format. Its format version is kept,
     * the files of a backup are never rewritten just because they have been read.
     */
    Backup migrate() {
        return new Backup(
            id, provider, target, type, origin, created, profile == null ? null : profile.migrate(),
            source, paths, ignored, archive, files, size, hmcVersion
        );
    }

}
