package io.github.headlesshq.headlessmc.commands.config;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

@ConfigMapping(prefix = "hmc.test.commands")
public interface TestCommandConfig extends DynamicConfig {
    @WithDefault("6")
    int number();

    Optional<String> token();

}
