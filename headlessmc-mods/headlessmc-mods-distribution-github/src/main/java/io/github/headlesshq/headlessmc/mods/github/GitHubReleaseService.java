package io.github.headlesshq.headlessmc.mods.github;

import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionException;
import io.github.headlesshq.headlessmc.net.rest.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Fetches {@link GitHubRelease}s from the {@link GitHubAPI},
 * authenticated with the {@link GitHubConfig#token()} if one is configured.
 * Fetched releases are cached in the {@link GitHubReleaseCache} for {@link GitHubConfig#cacheMinutes()},
 * if a release cannot be fetched, a cached release is used regardless of its age.
 */
@Slf4j
@ApplicationScoped
public class GitHubReleaseService {
    private static final String LATEST = "latest";

    private final GitHubAPI api;
    private final GitHubConfig config;
    private final GitHubReleaseCache cache;
    private final Clock clock;

    @Inject
    public GitHubReleaseService(@RestClient GitHubAPI api, GitHubConfig config, GitHubReleaseCache cache) {
        this(api, config, cache, Clock.systemUTC());
    }

    GitHubReleaseService(GitHubAPI api, GitHubConfig config, GitHubReleaseCache cache, Clock clock) {
        this.api = api;
        this.config = config;
        this.cache = cache;
        this.clock = clock;
    }

    /**
     * Gets a release of a repository.
     *
     * @param owner the owner of the repository.
     * @param repo  the name of the repository.
     * @param tag   the tag of the release, or {@code null} for the latest release.
     * @return the release.
     * @throws ModDistributionException if the release could neither be fetched nor found in the cache.
     */
    public GitHubRelease getRelease(String owner, String repo, @Nullable String tag) throws ModDistributionException {
        String key = owner + "/" + repo + "@" + (tag == null ? LATEST : "tags/" + tag);
        Instant now = clock.instant();
        Optional<GitHubReleaseCache.CachedRelease> cached = cache.get(key);
        if (cached.isPresent() && isFresh(cached.get(), now)) {
            return cached.get().release();
        }

        String authorization = config.token().filter(token -> !token.isBlank()).map(token -> "Bearer " + token).orElse(null);
        try {
            GitHubRelease release = tag == null
                ? api.getLatestRelease(owner, repo, authorization)
                : api.getReleaseByTag(owner, repo, tag, authorization);
            if (config.cacheMinutes() > 0) {
                cache.set(key, new GitHubReleaseCache.CachedRelease(now.toEpochMilli(), release));
            }

            return release;
        } catch (ApiException | ProcessingException e) {
            if (cached.isPresent()) {
                log.warn("Failed to fetch release {} of {}/{}, using the cached release: {}",
                    tag == null ? LATEST : tag, owner, repo, e.getMessage());
                return cached.get().release();
            }

            throw new ModDistributionException(
                "Failed to get release %s of %s/%s".formatted(tag == null ? LATEST : tag, owner, repo), e
            );
        }
    }

    private boolean isFresh(GitHubReleaseCache.CachedRelease cached, Instant now) {
        Instant fetchedAt = Instant.ofEpochMilli(cached.fetchedAt());
        return !fetchedAt.isAfter(now) && fetchedAt.plus(Duration.ofMinutes(config.cacheMinutes())).isAfter(now);
    }

}
