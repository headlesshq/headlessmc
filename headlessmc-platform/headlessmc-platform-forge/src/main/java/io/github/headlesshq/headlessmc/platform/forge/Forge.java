package io.github.headlesshq.headlessmc.platform.forge;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Qualifier for implementations of the Forge platform.
 * @see ForgePlatform
 * @see <a href=https://files.minecraftforge.net/net/minecraftforge/forge/>
 * https://files.minecraftforge.net/net/minecraftforge/forge/
 * </a>
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Forge {
    String PLATFORM_NAME = "forge";
    String CAPITALIZED_PLATFORM_NAME = "Forge";

}
