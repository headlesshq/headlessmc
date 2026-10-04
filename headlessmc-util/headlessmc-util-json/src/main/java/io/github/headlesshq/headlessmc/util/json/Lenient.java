package io.github.headlesshq.headlessmc.util.json;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Qualifier for a {@link JsonService} that is very lenient.
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Lenient {

}
