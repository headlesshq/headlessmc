package io.github.headlesshq.headlessmc.test;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

@ConfigMapping(prefix = "hmc.test")
public interface TestConfig extends DynamicConfig {
    @WithDefault("true")
    boolean leave();

    @WithDefault("false")
    boolean server();

    @WithDefault("false")
    boolean noTimeout();

    Optional<String> file();

}
