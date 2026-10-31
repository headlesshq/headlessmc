package io.github.headlesshq.headlessmc.quilt;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.platform.PlatformVersion;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.util.AbstractJsonVersionService;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;

/**
 * The Quilt loader versions. Like on Fabric, builds are loader versions,
 * which we assume can load every Minecraft version.
 * The latest build is the newest stable loader, Quilt publishes betas, too.
 */
final class QuiltVersionService extends AbstractJsonVersionService<PlatformVersion, List<QuiltBuildData>> {
    public QuiltVersionService(Cache<List<QuiltBuildData>> cache) {
        super(Quilt.PLATFORM_NAME, cache);
    }

    @Override
    public SequencedSet<PlatformVersion> getBuilds(VanillaVersion version) throws HeadlessMcException {
        return getVersions();
    }

    @Override
    public Optional<PlatformVersion> getLatestBuild(VanillaVersion version) throws HeadlessMcException {
        SequencedSet<PlatformVersion> builds = getBuilds(version);
        return builds.stream()
            .filter(build -> !build.getName().contains("-"))
            .findFirst()
            .or(() -> builds.stream().findFirst());
    }

    @Override
    protected SequencedSet<PlatformVersion> map(
        List<QuiltBuildData> value,
        @Nullable SequencedSet<PlatformVersion> previous
    ) {
        SequencedSet<PlatformVersion> result = new LinkedHashSet<>();
        for (QuiltBuildData buildData : value) {
            result.add(createVersion(buildData.version()));
        }

        return verify(value, result, previous);
    }

}
