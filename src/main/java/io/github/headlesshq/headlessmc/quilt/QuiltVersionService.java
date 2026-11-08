package io.github.headlesshq.headlessmc.quilt;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.platform.PlatformVersion;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.util.AbstractJsonVersionService;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;

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
            .filter(QuiltVersionService::isStable)
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

    @Override
    protected Set<PlatformVersion> mapStable(List<QuiltBuildData> value, @Nullable Set<PlatformVersion> previous) {
        Set<PlatformVersion> result = new HashSet<>();
        for (QuiltBuildData buildData : value) {
            if (isStable(buildData.version())) {
                result.add(createVersion(buildData.version()));
            }
        }

        return result;
    }

    /**
     * The Quilt meta has no stable flag, betas and release candidates have a qualifier, e.g. {@code 0.31.0-beta.4}.
     */
    private static boolean isStable(PlatformVersion version) {
        return isStable(version.getName());
    }

    private static boolean isStable(String version) {
        return !version.contains("-");
    }

}
