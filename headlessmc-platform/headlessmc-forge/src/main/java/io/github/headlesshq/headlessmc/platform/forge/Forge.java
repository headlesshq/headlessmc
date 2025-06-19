package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.platform.Platform;
import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The Forge {@link Qualifier} identifies all {@link Platform}
 * related implementations that refer to the Forge platform.
 * @see ForgePlatform
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Forge {

}
