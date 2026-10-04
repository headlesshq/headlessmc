package io.github.headlesshq.headlessmc.platform.fabric;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Qualifier for implementations of the Fabric platform.
 *
 * @see FabricPlatform
 * @see <a href=https://fabricmc.net/>https://fabricmc.net/</a>
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Fabric {
    String PLATFORM_NAME = "fabric";
    String CAPITALIZED_PLATFORM_NAME = "Fabric";

}
