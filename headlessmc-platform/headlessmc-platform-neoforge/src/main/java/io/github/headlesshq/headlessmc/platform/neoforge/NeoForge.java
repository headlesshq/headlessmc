package io.github.headlesshq.headlessmc.platform.neoforge;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Qualifier for implementations of the NeoForge platform.
 * @see NeoForgePlatform
 * @see <a href=https://neoforged.net/>https://neoforged.net/</a>
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface NeoForge {
    String PLATFORM_NAME = "neoforge";
    String CAPITALIZED_PLATFORM_NAME = "NeoForge";

}
