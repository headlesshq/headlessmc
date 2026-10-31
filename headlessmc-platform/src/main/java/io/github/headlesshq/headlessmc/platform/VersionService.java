package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;

/**
 * Each {@link Platform} has a VersionService.
 * A VersionService lists the {@link PlatformVersion}s
 * that are available for the Platform.
 * // TODO: examples
 *
 * @see PlatformVersion
 * @see VanillaVersionService
 */
// we should think about platforms coming in from plugins that do not use vanilla-version-args
//  e.g. some plugin for mineflayer or something else that is vanilla-version-agnostic
//  the easiest way for these platforms is probably to just accept any vanilla version and not care
//  VanillaVersion also supports VersionArg.LATEST and VersionArg.STABLE
//  Every platform could also supply its own VanillaVersionService
public interface VersionService {
    String getPlatformName();

    SequencedSet<? extends PlatformVersion> getVersions() throws HeadlessMcException;

    SequencedSet<? extends PlatformVersion> getBuilds(VanillaVersion version) throws HeadlessMcException;

    Optional<? extends PlatformVersion> getLatestBuild(VanillaVersion version) throws HeadlessMcException;

    /**
     * Returns the latest build for the given version that this platform considers stable.
     * What counts as stable depends on the platform, e.g. Fabric loaders marked as stable,
     * recommended Forge builds or builds in the stable channel of Paper.
     * Platforms without any notion of stability may return {@link #getLatestBuild(VanillaVersion)}.
     *
     * @param version the vanilla version to get the build for.
     * @return the latest stable build, or empty if there is no stable build for the version.
     */
    Optional<? extends PlatformVersion> getLatestStableBuild(VanillaVersion version) throws HeadlessMcException;

    Optional<? extends PlatformVersion> getBuild(VanillaVersion version, String build) throws HeadlessMcException;

    SequencedSet<PlatformVersion> sort(Collection<PlatformVersion> versions) throws HeadlessMcException, IllegalArgumentException;

    // TODO: test
    default boolean isOlderThan(PlatformVersion version, PlatformVersion newerVersion) throws HeadlessMcException, IllegalArgumentException {
        if (version.equals(newerVersion)) {
            return false;
        }

        return sort(List.of(version, newerVersion)).getFirst().equals(newerVersion);
    }

}
