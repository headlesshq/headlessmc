package io.github.headlesshq.headlessmc.mods.github;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

/**
 * Configuration for the requests the {@link GitHubDistributionPlatform} makes to the GitHub API.
 */
@ConfigMapping(prefix = "hmc.github")
public interface GitHubConfig extends DynamicConfig {
    /**
     * Unauthenticated requests to the GitHub API are limited to 60 per hour,
     * which shared IPs, e.g. of CI runners, can easily exceed.
     *
     * @return a GitHub token to authenticate requests with, which raises the rate limit.
     */
    Optional<String> token();

    /**
     * @return how many minutes fetched releases are cached for, {@code 0} disables the cache.
     */
    @WithDefault("60")
    int cacheMinutes();

}
