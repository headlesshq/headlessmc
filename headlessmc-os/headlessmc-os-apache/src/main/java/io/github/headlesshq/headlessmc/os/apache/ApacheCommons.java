package io.github.headlesshq.headlessmc.os.apache;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Qualifier for instances of {@link io.github.headlesshq.headlessmc.os.CPU}
 * and {@link io.github.headlesshq.headlessmc.os.OS} created by using
 * the Apache Commons library.
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ApacheCommons {

}
