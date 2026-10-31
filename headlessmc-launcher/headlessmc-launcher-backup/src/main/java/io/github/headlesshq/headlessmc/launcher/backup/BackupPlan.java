package io.github.headlesshq.headlessmc.launcher.backup;

import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * What a {@link BackupProvider} resolved a {@link BackupRequest} to.
 *
 * @param target         the resolved target, e.g. the name of a profile.
 * @param type           the resolved type of the backup.
 * @param profile        the profile that is backed up, or {@code null} if none.
 * @param source         the directory to back up.
 * @param paths          the directories, relative to {@code source}, to back up.
 *                       An empty string stands for the entire {@code source} directory.
 * @param defaultIgnored glob patterns of paths the provider leaves out by default.
 */
public record BackupPlan(
    String target,
    String type,
    @Nullable Profile profile,
    Path source,
    List<String> paths,
    List<String> defaultIgnored
) {

}
