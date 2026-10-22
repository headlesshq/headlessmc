package io.github.headlesshq.headlessmc.auth.yggdrasil;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

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

    /**
     * The path the Yggdrasil API is served under, relative to {@link #url()}.
     * Corresponds to the {@code hmc.auth.yggdrasil.api-path} property and
     * defaults to {@value YggdrasilClientFactory#DEFAULT_API_PATH}.
     *
     * <p>The Yggdrasil specification does not mandate where the API lives, the
     * login endpoints are only relative to the API root. Deployments based on
     * Blessing Skin, such as LittleSkin, serve it under
     * {@code /api/yggdrasil}, which is why that is the default. Other servers
     * may use a different path.</p>
     *
     * @return the path of the API, always starting with a slash.
     */
    @WithDefault(YggdrasilClientFactory.DEFAULT_API_PATH)
    String apiPath();

}
