package io.github.headlesshq.headlessmc.auth.yggdrasil;

import java.util.Optional;

/**
 * Configuration of the Yggdrasil auth provider.
 *
 * <p>The provider is only enabled when {@link #url()} is present, so that Yggdrasil
 * authentication has to be opted into explicitly.</p>
 */
public interface YggdrasilConfig {
    /**
     * The configured default server url, e.g. {@code https://littleskin.cn}.
     * Corresponds to the {@code hmc.auth.yggdrasil.url} property.
     *
     * @return the server url, or an empty {@link Optional} if Yggdrasil auth
     * is disabled.
     */
    Optional<String> url();

}
