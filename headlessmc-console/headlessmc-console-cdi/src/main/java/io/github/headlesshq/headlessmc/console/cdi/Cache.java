package io.github.headlesshq.headlessmc.console.cdi;

import io.github.headlesshq.headlessmc.console.cache.ConsoleCachingService;
import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Qualifier for the {@link ConsoleCachingService}.
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Cache {
}
