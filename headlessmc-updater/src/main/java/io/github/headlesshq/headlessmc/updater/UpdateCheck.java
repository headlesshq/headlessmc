package io.github.headlesshq.headlessmc.updater;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Duration;
import java.time.Instant;

/**
 * The result of a check for updates, cached by an {@link UpdateCheckCache},
 * so that remote sources do not have to be requested every time HeadlessMc starts.
 *
 * @param checkedAt the time of the check, in milliseconds since the epoch.
 * @param value     the result of the check.
 * @param <V>       the type of result.
 */
@RegisterForReflection
public record UpdateCheck<V>(long checkedAt, V value) implements ReflectionRegistered {
    /**
     * Checks if this result is older than the given interval and the check should be done again.
     * Results from the future, e.g. after the system clock has been changed, are stale, too.
     *
     * @param now      the current time.
     * @param interval the interval after which a result becomes stale.
     * @return {@code true} if the check should be done again.
     */
    public boolean isStale(Instant now, Duration interval) {
        Instant checked = Instant.ofEpochMilli(checkedAt);
        return checked.isAfter(now) || !checked.plus(interval).isAfter(now);
    }

}
