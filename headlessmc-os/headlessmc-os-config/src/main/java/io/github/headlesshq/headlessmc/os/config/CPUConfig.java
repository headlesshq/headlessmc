package io.github.headlesshq.headlessmc.os.config;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.smallrye.config.ConfigMapping;

import java.util.Optional;

@ConfigMapping(prefix = "hmc.cpu")
public interface CPUConfig extends DynamicConfig {
    Optional<String> arch();

    Optional<Integer> bitness();

}
