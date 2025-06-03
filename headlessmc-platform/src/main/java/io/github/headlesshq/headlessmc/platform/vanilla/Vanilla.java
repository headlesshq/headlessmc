package io.github.headlesshq.headlessmc.platform.vanilla;

import io.github.headlesshq.headlessmc.platform.Platform;
import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The Vanilla {@link Qualifier} identifies all {@link Platform}
 * related implementations that refer to the vanilla platform.
 * @see VanillaPlatform
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Vanilla {

}
