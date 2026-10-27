package io.github.headlesshq.headlessmc.mods.distribution;

import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Manages the {@link ModDistributionPlatform}s supported by HeadlessMc.
 */
public interface ModDistributionPlatformService {
    /**
     * @return the {@link ModDistributionPlatform#DEFAULT} platform, which combines the
     * {@link ModDistributionPlatform#DEFAULT_PLATFORMS}.
     */
    ModDistributionPlatform defaultPlatform();

    /**
     * @return a Stream of {@link ModDistributionPlatform}s supported by HeadlessMc,
     * without the combined {@link ModDistributionPlatform#DEFAULT} and {@link ModDistributionPlatform#ALL} platforms.
     */
    Stream<ModDistributionPlatform> platforms();

    /**
     * Finds a {@link ModDistributionPlatform} by name.
     * {@link ModDistributionPlatform#DEFAULT} and {@link ModDistributionPlatform#ALL} return combined platforms.
     *
     * @param name the name of the ModDistributionPlatform to find.
     * @return the ModDistributionPlatform with the given name, or an empty Optional.
     */
    Optional<ModDistributionPlatform> byName(String name);

    default ModDistributionPlatform getByArg(@Nullable String arg) {
        if (arg == null) {
            return defaultPlatform();
        }

        return byName(arg)
            .orElseThrow(() -> new IllegalArgumentException("Failed to find mod distribution platform " + arg));
    }

}
