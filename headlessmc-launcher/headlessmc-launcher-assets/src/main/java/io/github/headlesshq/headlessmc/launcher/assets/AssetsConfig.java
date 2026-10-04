package io.github.headlesshq.headlessmc.launcher.assets;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.github.headlesshq.headlessmc.net.NetConfig;
import io.github.headlesshq.headlessmc.net.parallel.ParallelConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.net.URI;

@ConfigMapping(prefix = "hmc.assets")
public interface AssetsConfig extends DynamicConfig {
    @WithDefault("false")
    boolean dummy();

    @WithDefault("https://resources.download.minecraft.net")
    URI url();

    /** Network config for asset downloads, nested under {@code hmc.assets.net}. */
    NetConfig net();

    /** Parallel download config for assets, nested under {@code hmc.assets.parallel}. */
    ParallelConfig parallel();

}
