package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.platform.BoundPlatformVersion;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.util.AbstractJsonVersionService;
import io.quarkus.runtime.annotations.RegisterResources;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.SequencedSet;

@RegisterResources(globs = "forge/forge-versions.json")
public class ForgeVersionService extends AbstractJsonVersionService<BoundPlatformVersion, PrismIndex> {
    public ForgeVersionService(String platformName, Cache<PrismIndex> cache) {
        super(platformName, cache);
    }

    @Override
    protected SequencedSet<BoundPlatformVersion> map(
        PrismIndex value,
        @Nullable SequencedSet<BoundPlatformVersion> previous
    ) {
        SequencedSet<BoundPlatformVersion> result = new LinkedHashSet<>();
        for (PrismIndex.Meta meta : value.versions()) {
            result.add(createVersion(meta.version(), meta.getMcVersion()));
        }

        return verify(value, result, previous);
    }

    @Override
    public SequencedSet<BoundPlatformVersion> getBuilds(VanillaVersion version) throws HeadlessMcException {
        return filter(getVersions(), version);
    }

    @Override // expose to this package
    protected Cache<PrismIndex> getCache() {
        return super.getCache();
    }

}
