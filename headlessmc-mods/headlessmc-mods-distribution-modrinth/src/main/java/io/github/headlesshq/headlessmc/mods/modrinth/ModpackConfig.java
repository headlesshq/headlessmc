package io.github.headlesshq.headlessmc.mods.modrinth;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.github.headlesshq.headlessmc.net.parallel.ParallelConfig;
import io.smallrye.config.ConfigMapping;

/**
 * Configuration of the {@link ModpackDownloader}.
 */
@ConfigMapping(prefix = ModpackConfig.PREFIX)
public interface ModpackConfig extends DynamicConfig {
    /**
     * The prefix of all properties of this config.
     */
    String PREFIX = "hmc.mods.modpacks";

    /** Parallel download config for modpack files, nested under {@code hmc.mods.modpacks.parallel}. */
    ParallelConfig parallel();

}
