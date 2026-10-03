package io.github.headlesshq.headlessmc.os.config;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.smallrye.config.ConfigMapping;

import java.util.Optional;

@ConfigMapping(prefix = "hmc.os")
public interface OSConfig extends DynamicConfig {
    Optional<String> name();

    Optional<String> type();

    Optional<String> version();

    Optional<String> actualType();

}
