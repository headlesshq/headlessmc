package io.github.headlesshq.headlessmc.mods.github;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Qualifier for the {@link GitHubDistributionPlatform},
 * so that it can be injected directly, e.g. to download the headlesshq mods.
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface GitHub {
    String PLATFORM_NAME = "github";

}
