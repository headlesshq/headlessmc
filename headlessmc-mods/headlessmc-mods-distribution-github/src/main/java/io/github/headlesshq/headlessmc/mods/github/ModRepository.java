package io.github.headlesshq.headlessmc.mods.github;

import io.github.headlesshq.headlessmc.mods.distribution.RemoteMod;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * A GitHub repository that distributes a mod via its releases.
 * Implementations are picked up by the {@link GitHubDistributionPlatform}.
 */
public interface ModRepository {
    /**
     * @return the name of the mod in HeadlessMc, which is used as the id of the {@link RemoteMod}.
     */
    String getName();

    /**
     * @return a description of what the mod does.
     */
    String getDescription();

    /**
     * @return the owner of the GitHub repository.
     */
    String getOwner();

    /**
     * @return the name of the GitHub repository.
     */
    String getRepository();

    /**
     * Checks whether this mod can generally run on the given version.
     * This does not check whether a release actually contains a file for the version,
     * see {@link #findAsset(GitHubRelease, VersionArg)} for that.
     *
     * @param version the version to check.
     * @return {@code true} if this mod might be available for the given version.
     */
    boolean supports(VersionArg version);

    /**
     * Gets the tag of the release that contains this mod for the given version.
     *
     * @param version    the version the mod should be for.
     * @param modVersion the version of the mod that has been requested, {@code null} for the latest version.
     * @return the tag of the release, or empty to use the latest release of the repository.
     */
    Optional<String> getReleaseTag(VersionArg version, @Nullable String modVersion);

    /**
     * Finds the file of the given release which contains this mod for the given version.
     *
     * @param release the release to find the file in.
     * @param version the version the mod should be for.
     * @return the file for the given version or empty if the release contains none.
     */
    Optional<GitHubRelease.Asset> findAsset(GitHubRelease release, VersionArg version);

    /**
     * @return the names ({@link #getName()}) of the mods that have to be downloaded alongside this mod.
     */
    default List<String> getDependencies() {
        return List.of();
    }

    /**
     * @return this repository as a {@link RemoteMod}.
     */
    default RemoteMod toMod() {
        return new RemoteMod(getName(), getName(), getDescription());
    }

}
