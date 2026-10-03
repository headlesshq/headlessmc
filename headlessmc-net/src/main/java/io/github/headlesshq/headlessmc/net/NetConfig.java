package io.github.headlesshq.headlessmc.net;

import io.github.headlesshq.headlessmc.HeadlessMc;
import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

/**
 * Configuration for network/download behaviour.
 * May also be used nested under another prefix, e.g. {@code hmc.assets.net}.
 */
@ConfigMapping(prefix = "hmc.net")
public interface NetConfig extends DynamicConfig {
    Optional<HttpVersion> httpVersion();

    @WithDefault("true")
    boolean cookies();

    @WithDefault(HeadlessMc.DEFAULT_USER_AGENT)
    String userAgent();

    @WithDefault("true")
    boolean deleteFailedFiles();

    @WithDefault("1")
    int retries();

}
