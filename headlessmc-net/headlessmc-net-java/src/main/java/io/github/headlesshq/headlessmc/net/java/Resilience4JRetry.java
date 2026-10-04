package io.github.headlesshq.headlessmc.net.java;

import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import io.github.headlesshq.headlessmc.net.DownloadRetry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.Callable;

@RequiredArgsConstructor
final class Resilience4JRetry implements DownloadRetry {
    private final Retry retry;

    public Resilience4JRetry(int retries, int initialIntervalMs, double factor, double jitter) {
        this(
            Retry.of(
                "download",
                RetryConfig.custom()
                    .maxAttempts(retries)
                    .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(initialIntervalMs, factor, jitter))
                    .ignoreExceptions(InterruptedException.class, UncheckedInterruptedException.class)
                    .build()
            )
        );
    }

    @Override
    public <T> T run(Callable<T> task) throws Exception {
        return retry.executeCallable(task);
    }

}
