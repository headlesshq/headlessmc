package io.github.headlesshq.headlessmc.platform.fabric;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Identifies the URL where data about fabrics (build) versions is stored.
 *
 * @see FabricFactory#getFabricBuildData()
 * @see FabricVersionService
 * @see <a href=https://meta.fabricmc.net/v2/versions/loader>
 *     https://meta.fabricmc.net/v2/versions/loader
 *     </a>
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface FabricBuildData {

}
