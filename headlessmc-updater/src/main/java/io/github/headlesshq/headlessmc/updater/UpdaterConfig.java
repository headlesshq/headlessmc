package io.github.headlesshq.headlessmc.updater;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for the check for new HeadlessMc releases.
 */
@ConfigMapping(prefix = "hmc.updater")
public interface UpdaterConfig extends DynamicConfig {
    @WithDefault("true")
    boolean enabled();

    @WithDefault("headlesshq/headlessmc")
    String repository();

    @WithDefault("3000")
    int timeoutMs();

    @WithDefault("24")
    int intervalHours();

}
