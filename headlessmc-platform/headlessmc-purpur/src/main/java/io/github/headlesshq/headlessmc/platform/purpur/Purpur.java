package io.github.headlesshq.headlessmc.platform.purpur;

import io.github.headlesshq.headlessmc.platform.Platform;
import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The Purpur {@link Qualifier} identifies all {@link Platform}
 * related implementations that refer to the Purpur platform.
 * @see PurpurPlatform
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Purpur {
}
