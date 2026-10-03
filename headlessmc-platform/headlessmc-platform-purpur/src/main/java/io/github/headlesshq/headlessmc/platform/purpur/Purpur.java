package io.github.headlesshq.headlessmc.platform.purpur;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Qualifier for implementations of the Purpur platform.
 *
 * @see PurpurPlatform
 * @see <a href=https://purpurmc.org/>https://purpurmc.org/</a>
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Purpur {
    String PLATFORM_NAME = "purpur";
    String CAPITALIZED_PLATFORM_NAME = "Purpur";

}
