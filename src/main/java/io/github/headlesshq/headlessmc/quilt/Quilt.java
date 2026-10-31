package io.github.headlesshq.headlessmc.quilt;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Qualifier for implementations of the Quilt platform.
 *
 * @see QuiltPlatform
 * @see <a href=https://quiltmc.org/>https://quiltmc.org/</a>
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Quilt {
    String PLATFORM_NAME = "quilt";
    String CAPITALIZED_PLATFORM_NAME = "Quilt";

}
