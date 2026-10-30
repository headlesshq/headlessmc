package io.github.headlesshq.headlessmc.platform.neoforge;

import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.platform.BoundPlatformVersion;
import io.github.headlesshq.headlessmc.platform.VersionService;
import io.github.headlesshq.headlessmc.platform.forge.ForgeVersionService;
import io.github.headlesshq.headlessmc.platform.forge.PrismIndex;
import io.quarkus.runtime.annotations.RegisterResources;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.SequencedSet;

@RegisterResources(globs = "neoforge/neoforge-versions.json")
public class NeoForgeVersionService extends ForgeVersionService implements VersionService {
    public NeoForgeVersionService(String platformName, Cache<PrismIndex> cache) {
        super(platformName, cache);
    }

    @Override
    protected SequencedSet<BoundPlatformVersion> map(
        PrismIndex value,
        @Nullable SequencedSet<BoundPlatformVersion> previous
    ) {
        SequencedSet<BoundPlatformVersion> result = new LinkedHashSet<>();
        for (PrismIndex.Meta meta : value.versions()) {
            result.add(createVersion(getBuildName(meta.version(), meta.getMcVersion()), meta.getMcVersion()));
        }

        return verify(value, result, previous);
    }

    /**
     * In the prism index NeoForge versions look like this: {@code 21.10.62-beta},
     * but we would like to only have the {@code 62-beta} part as the build name for {@code 1.21.10}.
     * Versions that do not start with their mc version, e.g. {@code 47.1.104} for {@code 1.20.1}, are kept as is.
     *
     * @param neoForgeVersion the NeoForge version, e.g. {@code 21.10.62-beta}.
     * @param mcVersion       the mc version of the NeoForge version, e.g. {@code 1.21.10}.
     * @return the build name, e.g. {@code 62-beta}.
     */
    static String getBuildName(String neoForgeVersion, String mcVersion) {
        if (neoForgeVersion.startsWith(mcVersion + ".")) {
            return neoForgeVersion.substring(mcVersion.length() + 1);
        } else if (("1." + neoForgeVersion).startsWith(mcVersion + ".")) {
            return neoForgeVersion.substring(mcVersion.length() - 1);
        }

        return neoForgeVersion;
    }

}
