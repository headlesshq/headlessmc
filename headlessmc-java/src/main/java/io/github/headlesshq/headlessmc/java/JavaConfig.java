package io.github.headlesshq.headlessmc.java;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "hmc.java")
public interface JavaConfig extends DynamicConfig {
    Optional<List<Path>> versions();

    @WithDefault("false")
    boolean failOnParsingFailure();

    @WithDefault("true")
    boolean download();

    @WithDefault("6")
    int maxScanDepth();

}
