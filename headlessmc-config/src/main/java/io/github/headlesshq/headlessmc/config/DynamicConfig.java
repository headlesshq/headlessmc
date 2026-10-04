package io.github.headlesshq.headlessmc.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * {@link ConfigMapping} interfaces to be held
 * by a {@link Holder} need to implement this marker interface.
 * All Beans implementing this interface are discovered by
 * the {@link ConfigService} and added as mapping
 * ({@link SmallRyeConfigBuilder#withMapping(Class)}).
 *
 * @implNote The extending class should be an interface,
 * which is annotated with {@link ConfigMapping}.
 * @see Holder
 */
public interface DynamicConfig {

}
