package io.github.headlesshq.headlessmc.console.jline;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.smallrye.config.ConfigMapping;

import java.util.Optional;

@ConfigMapping(prefix = "hmc.jline")
public interface JlineConfig extends DynamicConfig {
    boolean enabled();

    boolean ansiColors();

    boolean persistentCompletions();

    Optional<String> tooManyCandidates();

}
