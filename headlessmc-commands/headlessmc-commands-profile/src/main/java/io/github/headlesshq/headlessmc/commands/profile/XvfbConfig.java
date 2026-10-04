package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "hmc.xvfb")
public interface XvfbConfig extends DynamicConfig {
    @WithDefault("false")
    boolean check();

}
