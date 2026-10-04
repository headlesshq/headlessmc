package io.github.headlesshq.headlessmc.net.parallel;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for the bounded executor, {@link Retry}, {@link RateLimiter}
 * and {@link CircuitBreaker} used by {@link ParallelTaskService}.
 *
 * <p>All tasks of one service are expected to hit the same API, so the rate
 * limit and circuit breaker are shared between every task. The defaults are
 * tuned for many small downloads (Minecraft assets and libraries, usually a
 * few kilo- to megabytes each): retries back off quickly and give up after a
 * few attempts instead of stalling the whole download for a long time.
 *
 * <p>May also be used nested under another prefix, e.g. {@code hmc.assets.parallel}.
 */
@ConfigMapping(prefix = "hmc.parallel")
public interface ParallelConfig extends DynamicConfig {
    /** Maximum number of tasks that may be in progress at once, {@code 0} means one worker per task. */
    @WithDefault("6")
    int parallelism();

    /** Total number of attempts per task, including the initial attempt. */
    @WithDefault("4")
    int retryMaxAttempts();

    /** Base delay in milliseconds before the first retry. */
    @WithDefault("250")
    long retryInitialIntervalMs();

    /** Multiplier applied to the retry delay after each failed attempt. */
    @WithDefault("2.0")
    double retryBackOffFactor();

    /** Randomization factor ({@code 0.0}-{@code 1.0}) applied to each retry delay. */
    @WithDefault("0.5")
    double retryJitter();

    /** Upper bound in milliseconds for a single retry delay. */
    @WithDefault("2000")
    long retryMaxIntervalMs();

    /** Number of attempts allowed during each rate limit refresh period. */
    @WithDefault("200")
    int rateLimitPermits();

    /** Length of one rate limit refresh period in seconds. */
    @WithDefault("1")
    int rateLimitRefreshPeriodSeconds();

    /** Maximum time in seconds a worker may wait for a rate limit permit. */
    @WithDefault("30")
    int rateLimitTimeoutSeconds();

    /** Failure rate in percent at which the shared circuit breaker opens. */
    @WithDefault("50.0")
    float breakerFailureRateThreshold();

    /** Number of most recent calls used to calculate the failure rate. */
    @WithDefault("32")
    int breakerSlidingWindowSize();

    /** Minimum number of calls required before the failure rate is evaluated at all. */
    @WithDefault("8")
    int breakerMinimumCalls();

    /**
     * Time in milliseconds the circuit breaker stays open before probing the
     * API again. This is also how long a retry waits when it is rejected by
     * the open circuit breaker.
     */
    @WithDefault("5000")
    long breakerOpenWaitMs();

    /** Number of probe calls allowed while the circuit breaker is half-open. */
    @WithDefault("4")
    int breakerHalfOpenCalls();

    /**
     * @throws IllegalArgumentException if any value is out of range.
     */
    default void validate() {
        check(parallelism() >= 0, "parallelism must be >= 0");
        check(retryMaxAttempts() >= 1, "retryMaxAttempts must be >= 1");
        check(retryInitialIntervalMs() >= 0, "retryInitialIntervalMs must be >= 0");
        check(retryBackOffFactor() >= 1.0, "retryBackOffFactor must be >= 1.0");
        check(retryJitter() >= 0.0 && retryJitter() <= 1.0, "retryJitter must be between 0.0 and 1.0");
        check(retryMaxIntervalMs() >= retryInitialIntervalMs(), "retryMaxIntervalMs must be >= retryInitialIntervalMs");
        check(rateLimitPermits() >= 1, "rateLimitPermits must be >= 1");
        check(rateLimitRefreshPeriodSeconds() >= 1, "rateLimitRefreshPeriodSeconds must be >= 1");
        check(rateLimitTimeoutSeconds() >= 0, "rateLimitTimeoutSeconds must be >= 0");
        check(breakerFailureRateThreshold() > 0.0f && breakerFailureRateThreshold() <= 100.0f,
            "breakerFailureRateThreshold must be > 0.0 and <= 100.0");
        check(breakerSlidingWindowSize() >= 1, "breakerSlidingWindowSize must be >= 1");
        check(breakerMinimumCalls() >= 1, "breakerMinimumCalls must be >= 1");
        check(breakerOpenWaitMs() >= 1, "breakerOpenWaitMs must be >= 1");
        check(breakerHalfOpenCalls() >= 1, "breakerHalfOpenCalls must be >= 1");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

}
