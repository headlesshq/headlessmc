package io.github.headlesshq.headlessmc.plugins;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.List;
import java.util.Optional;

/**
 * Configuration for loading plugins.
 */
@ConfigMapping(prefix = "hmc.plugins")
public interface PluginConfig extends DynamicConfig {
    /**
     * @return {@code false} to ignore the plugins directory.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * @return a {@code quarkus-app} directory or {@code headlessmc-mutable.zip} to re-augment with the plugins,
     * instead of downloading the one belonging to this version of HeadlessMc.
     * Mainly useful for development, see the {@code mutableJarZip} task of headlessmc-application.
     */
    Optional<String> mutableJar();

    /**
     * @return {@code true} to exit if a plugin cannot be loaded,
     * {@code false} to continue with the plugins that can be loaded.
     */
    @WithDefault("true")
    boolean failOnError();

    /**
     * @return how many builds for different sets of plugins to keep in the cache.
     */
    @WithDefault("3")
    int keepBuilds();

    /**
     * @return {@code true} to speed up the start of the JVM running HeadlessMc with plugins with an AOT cache
     * (JEP 483, Java 25+), which is created in the background after the first run of a build.
     * Defaults to {@code true}, unless the {@code CI} environment variable is set, see {@link PluginAotCache}.
     */
    Optional<Boolean> aotCache();

    /**
     * @return {@code false} to run HeadlessMc with plugins on the JVM, even if {@code plugin compile}
     * has compiled a native executable for the installed plugins.
     */
    @WithName("native")
    @WithDefault("true")
    boolean useNative();

    /**
     * @return additional JVM arguments for the JVM running HeadlessMc with the plugins.
     */
    Optional<List<String>> jvmArgs();

}
