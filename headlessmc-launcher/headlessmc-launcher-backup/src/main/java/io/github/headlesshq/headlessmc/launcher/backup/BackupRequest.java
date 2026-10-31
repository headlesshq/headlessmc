package io.github.headlesshq.headlessmc.launcher.backup;

import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A request to create a backup.
 *
 * @param provider      the {@link BackupProvider#getName() name} of the provider to back up with.
 * @param target        what to back up, e.g. the name of a profile.
 * @param profile       the already resolved profile to back up, or {@code null} to resolve it from the target.
 * @param type          what to back up, one of the {@link BackupProvider#getTypes() types} of the provider,
 *                      or {@code null} for its default.
 * @param world         the world to back up, for providers that support backing up single worlds.
 * @param ignore        additional glob patterns of paths to leave out of the backup.
 * @param defaultIgnore whether to leave out the configured and the provider's default ignored paths.
 * @param origin        what creates the backup.
 * @param force         whether to back up even if the target is in use, e.g. a running game.
 */
public record BackupRequest(
    String provider,
    String target,
    @Nullable Profile profile,
    @Nullable String type,
    @Nullable String world,
    List<String> ignore,
    boolean defaultIgnore,
    BackupOrigin origin,
    boolean force
) {

}
