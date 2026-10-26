package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.version.Version;

import java.nio.file.Path;
import java.util.List;

/**
 * Describes what removing an installed {@link Version} entails,
 * created by {@link VersionRemoveService#plan(Version, boolean)}.
 *
 * @param version           the version to remove.
 * @param dependentVersions installed versions that inherit from the version and would break if it is removed.
 * @param blockingProfiles  client profiles that can only be launched with the version or its dependent versions.
 * @param removedVersions   the versions that will be removed, the version and possibly its dependent versions.
 * @param removedProfiles   the profiles that will be removed.
 * @param files             the existing files and directories to delete, the version directories last.
 */
public record VersionRemoval(
    Version version,
    List<Version> dependentVersions,
    List<Profile> blockingProfiles,
    List<Version> removedVersions,
    List<Profile> removedProfiles,
    List<Path> files
) {
    /**
     * @return {@code true} if the removal would break versions or profiles that it does not remove.
     */
    public boolean isBlocked() {
        return !removedVersions.containsAll(dependentVersions) || !removedProfiles.containsAll(blockingProfiles);
    }

}
