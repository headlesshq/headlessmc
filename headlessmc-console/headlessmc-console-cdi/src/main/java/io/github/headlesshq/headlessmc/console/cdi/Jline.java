package io.github.headlesshq.headlessmc.console.cdi;

import io.github.headlesshq.headlessmc.console.jline.JlineConsoleProvider;
import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Qualifier for the {@link JlineConsoleProvider}.
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Jline {

}
