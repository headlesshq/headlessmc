package io.github.headlesshq.headlessmc.mods.curseforge;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.smallrye.config.ConfigMapping;

import java.util.Optional;

/**
 * Configuration of the {@link CurseForgeDistributionPlatform}.
 */
@ConfigMapping(prefix = CurseForgeConfig.PREFIX)
public interface CurseForgeConfig extends DynamicConfig {
    /**
     * The prefix of all properties of this config.
     */
    String PREFIX = "hmc.mods.curseforge";

    /**
     * The key sent as {@code x-api-key} header to the CurseForge API.
     * Corresponds to the {@code hmc.mods.curseforge.api-key} property.
     * The CurseForge API rejects every request without a key,
     * keys can be obtained from <a href=https://console.curseforge.com>https://console.curseforge.com</a>.
     *
     * @return the api key, or an empty {@link Optional} if none has been configured.
     */
    Optional<String> apiKey();

}
