package io.github.headlesshq.headlessmc.platform.paper;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Qualifier for implementations of the Paper platform.
 *
 * @see PaperPlatform
 * @see <a href=https://papermc.io/>https://papermc.io/</a>
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Paper {
    String PLATFORM_NAME = "paper";
    String CAPITALIZED_PLATFORM_NAME = "Paper";

}
