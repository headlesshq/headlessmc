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
            String metaVersion = meta.version();
            // in the prism index NeoForge versions look like this: 21.10.62-beta
            // we would like to only have the 62-beta part
            if (metaVersion.startsWith(meta.getMcVersion())) {
                metaVersion = metaVersion.substring(meta.getMcVersion().length() + 1);
            } else if (("1." + metaVersion).startsWith(meta.getMcVersion())) {
                metaVersion = metaVersion.substring(meta.getMcVersion().length() - 1);
            }

            result.add(createVersion(metaVersion, meta.getMcVersion()));
        }

        return verify(value, result, previous);
    }

}
