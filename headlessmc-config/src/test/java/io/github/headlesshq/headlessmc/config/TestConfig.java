package io.github.headlesshq.headlessmc.config;

import io.smallrye.config.ConfigMapping;

import java.util.Optional;

@ConfigMapping(prefix = "hmc.test.properties")
public interface TestConfig extends DynamicConfig {
    String testProperty();

    int testInt();

    Optional<String> testOptional();

}
