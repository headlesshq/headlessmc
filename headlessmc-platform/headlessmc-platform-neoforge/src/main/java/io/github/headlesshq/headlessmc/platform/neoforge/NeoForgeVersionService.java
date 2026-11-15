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
        return createVersion(getBuildName(meta), meta.getMcVersion());
    }

    /**
     * @param meta an entry in the prism index.
     * @return the name of the build of the entry: its version without the Minecraft version,
     * e.g. {@code 62-beta} for {@code 21.10.62-beta}.
     */
    public static String getBuildName(PrismIndex.Meta meta) {
        String metaVersion = meta.version();
        // in the prism index NeoForge versions look like this: 21.10.62-beta
        // we would like to only have the 62-beta part
        if (metaVersion.startsWith(meta.getMcVersion())) {
            metaVersion = metaVersion.substring(meta.getMcVersion().length() + 1);
        } else if (("1." + metaVersion).startsWith(meta.getMcVersion())) {
            metaVersion = metaVersion.substring(meta.getMcVersion().length() - 1);
        }

        return metaVersion;
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
