package io.github.headlesshq.headlessmc.net.parallel;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import io.github.headlesshq.headlessmc.exceptions.VerificationException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * Runs download-like tasks against a single API with bounded concurrency.
 *
 * <p>Every attempt of every task passes through three shared resilience
 * layers, from the outside in:
 * <ol>
 *     <li>{@link Retry}: retries transient failures with exponential
 *     random backoff, so a flaky response does not fail the whole run.</li>
 *     <li>{@link CircuitBreaker}: shared by all tasks. Since they all target
 *     the same API, a spike of failures usually means the API itself is in
 *     trouble; the breaker then rejects further attempts for a short cooldown
 *     instead of hammering the API. Rejected attempts are retried after the
 *     cooldown rather than failed immediately.</li>
 *     <li>{@link RateLimiter}: a fresh permit is required by every actual
 *     HTTP attempt, including retries. It sits innermost so that attempts
 *     rejected by the breaker do not consume permits.</li>
 * </ol>
 *
 * <p>{@link VerificationException} and {@link UnrecoverableException} are
 * considered permanent failures and are never retried, nor do they trip the
 * circuit breaker.
 */
@Slf4j
public class ParallelTaskService<T extends ParallelTask> implements AutoCloseable {
    private final ExecutorService executor;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final RateLimiter rateLimiter;
    private final List<T> tasks;

    public ParallelTaskService(ParallelConfig config, List<T> tasks) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(tasks, "tasks");
        config.validate();

        log.info(config.toString());

        this.tasks = order(tasks, config.parallelism());
        this.executor = Executors.newFixedThreadPool(
            Math.clamp(this.tasks.size(), 1, config.parallelism() == 0 ? Integer.MAX_VALUE : config.parallelism()),
            Thread.ofVirtual().name("parallel-download-", 0).factory()
        );

        this.retry = createRetry(config);
        this.circuitBreaker = createCircuitBreaker(config);
        this.rateLimiter = createRateLimiter(config);
    }

    /**
     * Runs all tasks and blocks until every task has either completed or has
     * been handled by the given observer's
     * {@link TaskObserver#onTaskFailed(ParallelTask, Exception)}.
     *
     * @param observer notified on each completion and failure.
     * @throws HeadlessMcException if a task failed and the observer rethrew.
     * @throws UncheckedInterruptedException if the calling thread was interrupted.
     */
    public void run(TaskObserver<T> observer) throws HeadlessMcException, UncheckedInterruptedException {
        Objects.requireNonNull(observer, "observer");
        List<Future<?>> futures = new ArrayList<>(tasks.size());
        for (T task : tasks) {
            futures.add(executor.submit(() -> runTask(task, observer)));
        }

        try {
            for (Future<?> future : futures) {
                await(future);
            }
        } catch (RuntimeException | Error e) {
            futures.forEach(future -> future.cancel(true));
            throw e;
        }
    }

    /**
     * @return the sum of {@link ParallelTask#getSize()} over all tasks.
     */
    public long getTotalSize() {
        long totalSize = 0L;
        for (T task : tasks) {
            totalSize += task.getSize();
        }

        return totalSize;
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private void runTask(T task, TaskObserver<T> observer) {
        try {
            retry.executeCallable(() -> circuitBreaker.executeCallable(() -> rateLimiter.executeCallable(() -> {
                try {
                    task.run();
                    observer.onTaskCompleted(task);
                } catch (Exception e) {
                    log.error("Task {} failed", task, e);
                    throw e;
                }
                return null;
            })));
        } catch (InterruptedException | UncheckedInterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        } catch (Exception e) {
            handleFailure(task, observer, e);
        }
    }

    /**
     * Gives the observer a chance to swallow the failure. If it rethrows,
     * the exception is propagated to {@link #run(TaskObserver)}.
     */
    private void handleFailure(T task, TaskObserver<T> observer, Exception failure) {
        try {
            observer.onTaskFailed(task, failure);
        } catch (InterruptedException | UncheckedInterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        } catch (HeadlessMcException e) {
            throw e;
        } catch (Exception e) {
            throw new ParallelTaskException(e);
        }
    }

    private void await(Future<?> future) {
        try {
            future.get();
        } catch (CancellationException e) {
            if (Thread.currentThread().isInterrupted()) {
                throw new UncheckedInterruptedException(e);
            }

            throw new ParallelTaskException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        } catch (ExecutionException e) {
            Throwable cause = Objects.requireNonNullElse(e.getCause(), e);
            switch (cause) {
                case UncheckedInterruptedException interrupted -> throw interrupted;
                case HeadlessMcException headless -> throw headless;
                case Error error -> throw error;
                default -> throw new ParallelTaskException(cause);
            }
        }
    }

    /**
     * Orders tasks by size: shortest-processing-time for a single worker,
     * longest-processing-time for parallel workers (better makespan).
     */
    private static <T extends ParallelTask> List<T> order(List<T> tasks, int parallelism) {
        Comparator<ParallelTask> bySize = Comparator.comparingLong(ParallelTask::getSize);
        List<T> ordered = new ArrayList<>(tasks);
        ordered.sort(parallelism == 1 ? bySize : bySize.reversed());
        return List.copyOf(ordered);
    }

    private static Retry createRetry(ParallelConfig config) {
        IntervalFunction backOff = IntervalFunction.ofExponentialRandomBackoff(
            Duration.ofMillis(config.retryInitialIntervalMs()),
            config.retryBackOffFactor(),
            config.retryJitter(),
            Duration.ofMillis(config.retryMaxIntervalMs())
        );

        return Retry.of("download", RetryConfig.custom()
            .maxAttempts(config.retryMaxAttempts())
            // When the breaker rejected the attempt, wait for its cooldown instead of the usual backoff.
            .intervalBiFunction((attempt, result) ->
                result.isLeft() && result.getLeft() instanceof CallNotPermittedException
                    ? config.breakerOpenWaitMs()
                    : backOff.apply(attempt))
            .retryExceptions(
                IOException.class,
                TimeoutException.class,
                HeadlessMcException.class,
                CallNotPermittedException.class,
                RequestNotPermitted.class
            )
            .ignoreExceptions(
                VerificationException.class,
                UnrecoverableException.class,
                InterruptedException.class,
                UncheckedInterruptedException.class
            )
            .build());
    }

    private static CircuitBreaker createCircuitBreaker(ParallelConfig config) {
        return CircuitBreaker.of("download", CircuitBreakerConfig.custom()
            .failureRateThreshold(config.breakerFailureRateThreshold())
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(config.breakerSlidingWindowSize())
            .minimumNumberOfCalls(config.breakerMinimumCalls())
            .waitDurationInOpenState(Duration.ofMillis(config.breakerOpenWaitMs()))
            .permittedNumberOfCallsInHalfOpenState(config.breakerHalfOpenCalls())
            // Half-open itself after the cooldown so waiting retries can proceed.
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(IOException.class, TimeoutException.class, HeadlessMcException.class)
            .ignoreExceptions(
                VerificationException.class,
                UnrecoverableException.class,
                InterruptedException.class,
                UncheckedInterruptedException.class,
                // Waiting too long for our own rate limiter says nothing about the API's health.
                RequestNotPermitted.class
            )
            .build());
    }

    private static RateLimiter createRateLimiter(ParallelConfig config) {
        return RateLimiter.of("download", RateLimiterConfig.custom()
            .limitForPeriod(config.rateLimitPermits())
            .limitRefreshPeriod(Duration.ofSeconds(config.rateLimitRefreshPeriodSeconds()))
            .timeoutDuration(Duration.ofSeconds(config.rateLimitTimeoutSeconds()))
            .build());
    }

}
