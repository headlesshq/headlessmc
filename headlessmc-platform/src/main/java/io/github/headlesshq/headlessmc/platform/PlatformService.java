package io.github.headlesshq.headlessmc.platform;

import java.util.List;
import java.util.Optional;

/**
 * Manages all {@link Platform}s supported by HeadlessMc.
 *
 * @see Platform
 */
public interface PlatformService {
    /**
     * @return the list of all {@link Platform}s supported by HeadlessMc.
     */
    List<Platform> getPlatforms();

    /**
     * @return the list of {@link Platform}s supported by HeadlessMc,
     * without the {@link Vanilla} platform.
     */
    List<Platform> getPlatformsWithoutVanilla();

    /**
     * Finds a platform with the given name ({@link Platform#getName()}).
     * Ignores casing.
     *
     * @param name the name to search for.
     * @return the platform for the given name, or an empty optional.
     */
    Optional<Platform> getPlatform(String name);

    /**
     * @return the {@link Vanilla} platform.
     */
    VanillaPlatform getVanillaPlatform();

}
