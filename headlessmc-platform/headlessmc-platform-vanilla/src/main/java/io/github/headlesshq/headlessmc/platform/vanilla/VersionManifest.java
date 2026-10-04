package io.github.headlesshq.headlessmc.platform.vanilla;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Qualifier for the URL to use to get the Mc version manifest.
 * @see VanillaManifest
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface VersionManifest {

}
