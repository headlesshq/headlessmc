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

    @WithDefault("true")
    boolean checkForUpdates();

    /**
     * @return the distributions to try, in order, when jmods have to be downloaded for a Java version.
     * Since JEP 493 (Java 24) some distributions, like temurin, do not ship jmods anymore.
     */
    @WithDefault("zulu,corretto,temurin")
    List<String> jmodsDistributions();

}
