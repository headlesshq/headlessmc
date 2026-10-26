package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.version.Version;

import java.nio.file.Path;
import java.util.List;

/**
 * Describes what removing an installed {@link Version} entails, created by {@link VersionRemoveService#plan(Version)}.
 *
 * @param version           the version to remove.
 * @param dependentVersions installed versions that inherit from the version and would break if it is removed.
 * @param blockingProfiles  client profiles that can only be launched with the version.
 * @param files             the existing files and directories to delete, the version directory last.
 */
public record VersionRemoval(
    Version version,
    List<Version> dependentVersions,
    List<Profile> blockingProfiles,
    List<Path> files
) {
    /**
     * @return {@code true} if removing the version would break other versions or profiles.
     */
    public boolean isBlocked() {
        return !dependentVersions.isEmpty() || !blockingProfiles.isEmpty();
    }

}
