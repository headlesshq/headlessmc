package io.github.headlesshq.headlessmc.files;

import io.github.headlesshq.headlessmc.config.ConfigException;
import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@ConfigMapping(prefix = "hmc.files")
public interface FileConfig extends DynamicConfig {
    Optional<String> location();

    @WithDefault("true")
    boolean gameForEachVersion();

    @WithName("mc")
    Optional<String> mcDir();

    /**
     * Additional {@code .minecraft} directories, e.g. on other disks.
     * Versions installed in these directories can be listed and launched like versions in {@link #mcDir()},
     * which stays the default for new installations.
     *
     * @return the paths of additional {@code .minecraft} directories.
     */
    @WithName("mc-dirs")
    Optional<List<String>> mcDirs();

    @WithName("game")
    Optional<String> gameDir();

    default Optional<String> property(
        Optional<String> property,
        Optional<String> legacy,
        Consumer<String> warning,
        String legacyName,
        String name
    ) {
        if (legacy.isEmpty()) {
            return property;
        }

        if (property.isEmpty()) {
            warning.accept("Legacy configuration " + legacyName + " used, consider using " + name + " instead.");
            return legacy;
        }

        throw new ConfigException("Both " + name + " and deprecated configuration " + legacyName + " were used.");
    }

}
