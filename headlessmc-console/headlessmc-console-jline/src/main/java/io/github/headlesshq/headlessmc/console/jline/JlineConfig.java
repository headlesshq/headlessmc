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

    GraphemeClusterMode graphemeCluster();

    /**
     * Only used in game by hmc-specifics, declared here so that the config knows the property.
     */
    boolean dumb();

}
