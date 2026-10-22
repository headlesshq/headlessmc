package io.github.headlesshq.headlessmc.auth.yggdrasil;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.smallrye.config.ConfigMapping;

import java.util.Optional;

/**
 * Configuration of the Yggdrasil auth provider.
 *
 * <p>The provider is only enabled when {@link #url()} is present, so that Yggdrasil
 * authentication has to be opted into explicitly.</p>
 */
@ConfigMapping(prefix = YggdrasilConfig.PREFIX)
public interface YggdrasilConfig extends DynamicConfig {
    /**
     * The prefix of all properties of this config.
     */
    String PREFIX = "hmc.auth.yggdrasil";

    /**
     * The configured default server url, e.g. {@code https://littleskin.cn}.
     * Corresponds to the {@code hmc.auth.yggdrasil.url} property.
     *
     * @return the server url, or an empty {@link Optional} if Yggdrasil auth
     * is disabled.
     */
    Optional<String> url();

}
