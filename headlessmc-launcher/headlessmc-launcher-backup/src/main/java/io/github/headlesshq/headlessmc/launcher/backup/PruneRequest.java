package io.github.headlesshq.headlessmc.launcher.backup;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Set;

/**
 * Selects backups to delete with {@link BackupService#prune(PruneRequest)}.
 * Backups are grouped by provider and target. Within each group a backup is deleted
 * if it is not one of the newest {@code keep} backups and older than {@code olderThan}.
 *
 * @param provider  only prune backups of this provider, or {@code null} for all providers.
 * @param target    only prune backups of this target, or {@code null} for all targets.
 * @param origins   only prune backups with these origins, all origins if empty.
 * @param keep      how many of the newest backups of each group to keep, or {@code null} to keep none.
 * @param olderThan only prune backups older than this, or {@code null} to prune backups of any age.
 */
public record PruneRequest(
    @Nullable String provider,
    @Nullable String target,
    Set<BackupOrigin> origins,
    @Nullable Integer keep,
    @Nullable Duration olderThan
) {
    public PruneRequest {
        if (keep == null && olderThan == null) {
            throw new IllegalArgumentException("Please specify how many backups to keep or how old pruned backups are.");
        }

        if (keep != null && keep < 0) {
            throw new IllegalArgumentException("Cannot keep a negative amount of backups: " + keep);
        }
    }

}
