package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;

import java.util.Optional;
import java.util.SequencedSet;

public interface VanillaVersionService extends VersionService {
    VanillaVersion getLatest() throws HeadlessMcException;

    boolean hasVersion(String name) throws HeadlessMcException;

    Optional<VanillaVersion> getVersion(String name) throws HeadlessMcException;

    @Override
    SequencedSet<VanillaVersion> getVersions() throws HeadlessMcException;

    @Override
    Optional<VanillaVersion> getLatestBuild(VanillaVersion version) throws HeadlessMcException;

    @Override
    SequencedSet<VanillaVersion> getBuilds(VanillaVersion version) throws HeadlessMcException;

    @Override
    default String getPlatformName() {
        return Vanilla.PLATFORM_NAME;
    }

}
