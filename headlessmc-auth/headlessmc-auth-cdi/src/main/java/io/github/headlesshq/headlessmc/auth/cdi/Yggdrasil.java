package io.github.headlesshq.headlessmc.auth.cdi;

import io.github.headlesshq.headlessmc.auth.AuthProvider;
import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Identifies the Yggdrasil {@link AuthProvider}.
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Yggdrasil {

}
