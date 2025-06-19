package io.github.headlesshq.headlessmc.platform.paper;

import io.github.headlesshq.headlessmc.platform.Platform;
import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The Paper {@link Qualifier} identifies all {@link Platform}
 * related implementations that refer to the Paper platform.
 * @see PaperPlatform
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Paper {
}
