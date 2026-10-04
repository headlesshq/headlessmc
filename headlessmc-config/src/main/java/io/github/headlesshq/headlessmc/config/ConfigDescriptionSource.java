package io.github.headlesshq.headlessmc.config;

import java.util.Optional;

/**
 * SmallRye/Microprofile config provides no way to specify
 * descriptions for config properties.
 *
 * <p>{@link ConfigService} provides that functionality,
 * using implementations of this interface.
 *
 * <p>All discoverable Beans implementing this interface
 * will be discovered and used to look up descriptions
 * for setting keys.
 */
public interface ConfigDescriptionSource {
    /**
     * Provides the description for the setting with the given name
     * or an empty optional if this source has no description
     * for the given name.
     *
     * @param name the name of the setting to get a description for.
     * @return the description for the setting with the given name,
     * or an empty optional.
     */
    Optional<String> getDescription(String name);

}
