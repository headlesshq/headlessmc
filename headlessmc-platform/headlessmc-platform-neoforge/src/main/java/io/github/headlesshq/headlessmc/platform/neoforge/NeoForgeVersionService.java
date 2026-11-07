package io.github.headlesshq.headlessmc.platform.neoforge;

import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.platform.BoundPlatformVersion;
import io.github.headlesshq.headlessmc.platform.VersionService;
import io.github.headlesshq.headlessmc.platform.forge.ForgeVersionService;
import io.github.headlesshq.headlessmc.platform.forge.PrismIndex;
import io.quarkus.runtime.annotations.RegisterResources;

@RegisterResources(globs = "neoforge/neoforge-versions.json")
public class NeoForgeVersionService extends ForgeVersionService implements VersionService {
    public NeoForgeVersionService(String platformName, Cache<PrismIndex> cache) {
        super(platformName, cache);
    }

    @Override
    protected BoundPlatformVersion toVersion(PrismIndex.Meta meta) {
        String metaVersion = meta.version();
        // in the prism index NeoForge versions look like this: 21.10.62-beta
        // we would like to only have the 62-beta part
        if (metaVersion.startsWith(meta.getMcVersion())) {
            metaVersion = metaVersion.substring(meta.getMcVersion().length() + 1);
        } else if (("1." + metaVersion).startsWith(meta.getMcVersion())) {
            metaVersion = metaVersion.substring(meta.getMcVersion().length() - 1);
        }

        return createVersion(metaVersion, meta.getMcVersion());
    }

    /**
     * NeoForge does not recommend builds, but marks them as release or beta.
     * Some alpha builds for snapshots are marked as release, so we also check the name.
     *
     * @param meta the entry in the prism index.
     * @return {@code true} if the NeoForge build is a release.
     */
    @Override
    protected boolean isStable(PrismIndex.Meta meta) {
        return "release".equals(meta.type())
            && !meta.version().contains("alpha")
            && !meta.version().contains("beta");
    }

}
