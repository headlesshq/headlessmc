package io.github.headlesshq.headlessmc.mods.github;

import io.github.headlesshq.headlessmc.files.cache.CacheBuilder;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionException;
import io.github.headlesshq.headlessmc.net.rest.ApiException;
import jakarta.ws.rs.core.Response;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GitHubReleaseServiceTest {
    private final List<@Nullable String> authorizations = new ArrayList<>();
    private boolean available = true;
    private int requests;
    private @Nullable String token;
    private int cacheMinutes = 60;
    private Instant now = Instant.parse("2026-10-27T10:00:00Z");

    private final GitHubAPI api = new GitHubAPI() {
        @Override
        public GitHubRelease getLatestRelease(String owner, String repo, @Nullable String authorization) {
            return request("latest-" + requests, authorization);
        }

        @Override
        public GitHubRelease getReleaseByTag(String owner, String repo, String tag, @Nullable String authorization) {
            return request(tag + "-" + requests, authorization);
        }

        private GitHubRelease request(String tag, @Nullable String authorization) {
            requests++;
            authorizations.add(authorization);
            if (!available) {
                throw new ApiException(Response.status(Response.Status.FORBIDDEN).build());
            }

            return new GitHubRelease(tag, List.of());
        }
    };

    private final GitHubConfig config = new GitHubConfig() {
        @Override
        public Optional<String> token() {
            return Optional.ofNullable(token);
        }

        @Override
        public int cacheMinutes() {
            return cacheMinutes;
        }
    };

    private final GitHubReleaseService service = new GitHubReleaseService(
        api,
        config,
        new GitHubReleaseCache(CacheBuilder.<GitHubReleaseCache.Releases>create().build()),
        new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now;
            }
        }
    );

    @Test
    void releasesAreCachedUntilTheyExpire() {
        assertEquals("latest-0", service.getRelease("headlesshq", "hmc-specifics", null).tagName());
        assertEquals("latest-0", service.getRelease("headlesshq", "hmc-specifics", null).tagName());
        assertEquals("1.21.1-latest-1", service.getRelease("headlesshq", "hmc-specifics", "1.21.1-latest").tagName());
        assertEquals(2, requests);

        now = now.plus(Duration.ofMinutes(60));
        assertEquals("latest-2", service.getRelease("headlesshq", "hmc-specifics", null).tagName());
    }

    @Test
    void cachedReleasesAreUsedIfGitHubCannotBeReached() {
        service.getRelease("headlesshq", "hmc-specifics", null);
        now = now.plus(Duration.ofDays(1));
        available = false;

        assertEquals("latest-0", service.getRelease("headlesshq", "hmc-specifics", null).tagName());
        assertEquals(2, requests);
        assertThrows(ModDistributionException.class, () -> service.getRelease("headlesshq", "hmc-specifics", "1.0-latest"));
    }

    @Test
    void cacheCanBeDisabled() {
        cacheMinutes = 0;

        service.getRelease("headlesshq", "hmc-specifics", null);
        service.getRelease("headlesshq", "hmc-specifics", null);

        assertEquals(2, requests);
    }

    @Test
    void requestsAreAuthenticatedWithTheToken() {
        cacheMinutes = 0;
        service.getRelease("headlesshq", "hmc-specifics", null);
        token = " ";
        service.getRelease("headlesshq", "hmc-specifics", null);
        token = "ghp_test";
        service.getRelease("headlesshq", "hmc-specifics", null);

        assertNull(authorizations.get(0));
        assertNull(authorizations.get(1));
        assertEquals("Bearer ghp_test", authorizations.get(2));
    }

}
