package io.github.headlesshq.headlessmc.updater;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Duration;
import java.time.Instant;

/**
 * The latest release of HeadlessMc that has been fetched from GitHub, cached on disk,
 * so that GitHub does not have to be requested every time HeadlessMc starts.
 *
 * @param repository the repository the release has been fetched from, see {@link UpdaterConfig#repository()}.
 * @param checkedAt  the time the release has been fetched at, in milliseconds since the epoch.
 * @param release    the latest release.
 */
@RegisterForReflection
public record UpdateCache(String repository, long checkedAt, GitHubRelease release) implements ReflectionRegistered {
    /**
     * Checks if this entry is older than the given interval and should be fetched again.
     * Entries from the future, e.g. after the system clock has been changed, are stale, too.
     *
     * @param now      the current time.
     * @param interval the interval after which an entry becomes stale.
     * @return {@code true} if the release should be fetched again.
     */
    public boolean isStale(Instant now, Duration interval) {
        Instant checked = Instant.ofEpochMilli(checkedAt);
        return checked.isAfter(now) || !checked.plus(interval).isAfter(now);
    }

}
