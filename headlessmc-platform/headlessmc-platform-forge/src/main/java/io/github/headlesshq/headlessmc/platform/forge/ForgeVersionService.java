package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.platform.BoundPlatformVersion;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.util.AbstractJsonVersionService;
import io.quarkus.runtime.annotations.RegisterResources;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.SequencedSet;
import java.util.Set;

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
            result.add(toVersion(meta));
        }

        return verify(value, result, previous);
    }

    @Override
    protected Set<BoundPlatformVersion> mapStable(PrismIndex value, @Nullable Set<BoundPlatformVersion> previous) {
        Set<BoundPlatformVersion> result = new HashSet<>();
        for (PrismIndex.Meta meta : value.versions()) {
            if (isStable(meta)) {
                result.add(toVersion(meta));
            }
        }

        return result;
    }

    protected BoundPlatformVersion toVersion(PrismIndex.Meta meta) {
        return createVersion(meta.version(), meta.getMcVersion());
    }

    /**
     * @param meta the entry in the prism index.
     * @return {@code true} if the Forge build is recommended.
     */
    protected boolean isStable(PrismIndex.Meta meta) {
        return meta.recommended();
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
