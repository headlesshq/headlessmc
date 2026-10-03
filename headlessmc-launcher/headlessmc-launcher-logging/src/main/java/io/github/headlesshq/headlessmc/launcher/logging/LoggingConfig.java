package io.github.headlesshq.headlessmc.launcher.logging;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "hmc.logging")
public interface LoggingConfig extends DynamicConfig {
    /**
     * @return whether to patch the mc logging.xml file output.
     */
    @WithDefault("true")
    boolean patch();

}
