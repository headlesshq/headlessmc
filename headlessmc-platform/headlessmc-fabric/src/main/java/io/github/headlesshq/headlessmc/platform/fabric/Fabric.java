package io.github.headlesshq.headlessmc.platform.fabric;

import io.github.headlesshq.headlessmc.platform.Platform;
import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The Fabric {@link Qualifier} identifies all {@link Platform}
 * related implementations that refer to the Fabric platform.
 * @see FabricPlatform
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Fabric {
}
