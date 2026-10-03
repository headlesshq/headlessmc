package io.github.headlesshq.headlessmc.net.parallel;

import io.github.headlesshq.headlessmc.config.ConfigService;
import io.github.headlesshq.headlessmc.exceptions.VerificationException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;


@QuarkusTest
class ParallelTaskServiceTest {
    @Inject
    ConfigService configService;

    @Test
    void runsTasksInParallel() {
        int tasks = 8;
        long sleepMs = 300;
        AtomicInteger completed = new AtomicInteger();

        Duration elapsed = timed(() -> {
            try (ParallelTaskService<ParallelTask> service = new ParallelTaskService<>(config(), sleepingTasks(tasks, sleepMs))) {
                service.run(task -> completed.incrementAndGet());
            }
        });

        assertEquals(tasks, completed.get());
        assertTrue(elapsed.toMillis() < tasks * sleepMs / 2,
            "expected parallel throughput, but took " + elapsed.toMillis() + "ms");
    }

    @Test
    void limitsParallelism() {
        int tasks = 4;
        long sleepMs = 100;
        ParallelConfig config = config(Map.of("hmc.parallel.parallelism", "1"));
        AtomicInteger completed = new AtomicInteger();

        Duration elapsed = timed(() -> {
            try (ParallelTaskService<ParallelTask> service = new ParallelTaskService<>(config, sleepingTasks(tasks, sleepMs))) {
                service.run(task -> completed.incrementAndGet());
            }
        });

        assertEquals(tasks, completed.get());
        assertTrue(elapsed.toMillis() >= tasks * sleepMs,
            "expected sequential throughput, but took only " + elapsed.toMillis() + "ms");
    }

    @Test
    void limitsThroughputToRateLimit() {
        ParallelConfig config = config(Map.of(
            "hmc.parallel.rate-limit-permits", "2",
            "hmc.parallel.rate-limit-refresh-period-seconds", "1"
        ));
        AtomicInteger completed = new AtomicInteger();

        Duration elapsed = timed(() -> {
            try (ParallelTaskService<ParallelTask> service = new ParallelTaskService<>(config, sleepingTasks(6, 0))) {
                service.run(_ -> completed.incrementAndGet());
            }
        });

        assertEquals(6, completed.get());
        assertTrue(elapsed.toMillis() >= 1800,
            "expected rate limiting to take ~2000ms, but took only " + elapsed.toMillis() + "ms");
    }

    @Test
    void retriesTransientFailures() {
        ParallelConfig config = config(Map.of("hmc.parallel.retry-max-attempts", "4"));
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger completed = new AtomicInteger();
        ParallelTask task = new ParallelTaskImpl(1L, () -> {
            if (attempts.incrementAndGet() < 3) {
                throw new IOException("simulated transient failure");
            }
        });

        try (ParallelTaskService<ParallelTask> service = new ParallelTaskService<>(config, List.of(task))) {
            service.run(t -> completed.incrementAndGet());
        }

        assertEquals(3, attempts.get());
        assertEquals(1, completed.get());
    }

    @Test
    void doesNotRetryPermanentFailures() {
        ParallelConfig config = config(Map.of("hmc.parallel.retry-max-attempts", "4"));
        AtomicInteger attempts = new AtomicInteger();
        List<Exception> failures = new CopyOnWriteArrayList<>();
        ParallelTask task = new ParallelTaskImpl(1L, () -> {
            attempts.incrementAndGet();
            throw new VerificationException("simulated corrupt download");
        });

        try (ParallelTaskService<ParallelTask> service = new ParallelTaskService<>(config, List.of(task))) {
            service.run(observing(failures));
        }

        assertEquals(1, attempts.get(), "permanent failures must not be retried");
        assertEquals(1, failures.size());
        assertInstanceOf(VerificationException.class, failures.getFirst());
    }

    @Test
    void opensCircuitBreakerWhenApiKeepsFailing() {
        // No retries, so breaker rejections surface immediately to the observer.
        ParallelConfig config = config(Map.of(
            "hmc.parallel.retry-max-attempts", "1",
            "hmc.parallel.parallelism", "1",
            "hmc.parallel.breaker-sliding-window-size", "2",
            "hmc.parallel.breaker-minimum-calls", "2"
        ));
        List<Exception> failures = new CopyOnWriteArrayList<>();

        List<ParallelTask> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(new ParallelTaskImpl(1L, () -> {
                throw new IOException("simulated API outage");
            }));
        }

        try (ParallelTaskService<ParallelTask> service = new ParallelTaskService<>(config, tasks)) {
            service.run(observing(failures));
        }

        assertEquals(tasks.size(), failures.size());
        assertTrue(failures.stream().anyMatch(e -> e instanceof IOException),
            "expected the first tasks to actually hit the failing API");
        assertTrue(failures.stream().anyMatch(e -> e instanceof CallNotPermittedException),
            "expected the circuit breaker to reject calls after repeated failures");
    }

    @Test
    void computesTotalSize() {
        List<ParallelTask> tasks = List.of(
            new ParallelTaskImpl(3L, () -> { }),
            new ParallelTaskImpl(4L, () -> { }),
            new ParallelTaskImpl(null, () -> { })
        );

        try (ParallelTaskService<ParallelTask> service = new ParallelTaskService<>(config(), tasks)) {
            assertEquals(7L, service.getTotalSize());
        }
    }

    private ParallelConfig config() {
        return config(Map.of());
    }

    private ParallelConfig config(Map<String, String> overrides) {
        ConfigService fork = configService.fork();
        // Keep the retry/breaker timings small so tests stay fast.
        fork.set("hmc.parallel.retry-initial-interval-ms", "10", false);
        fork.set("hmc.parallel.retry-max-interval-ms", "50", false);
        fork.set("hmc.parallel.breaker-open-wait-ms", "100", false);
        overrides.forEach((name, value) -> fork.set(name, value, false));
        return fork.getHolder(ParallelConfig.class).get();
    }

    private static List<ParallelTask> sleepingTasks(int count, long sleepMs) {
        List<ParallelTask> tasks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            tasks.add(new ParallelTaskImpl(sleepMs, () -> Thread.sleep(sleepMs)));
        }

        return tasks;
    }

    private static TaskObserver<ParallelTask> observing(List<Exception> failures) {
        return new TaskObserver<>() {
            @Override
            public void onTaskCompleted(ParallelTask task) {
            }

            @Override
            public void onTaskFailed(ParallelTask task, Exception exception) {
                failures.add(exception);
            }
        };
    }

    private static Duration timed(Runnable runnable) {
        long start = System.nanoTime();
        runnable.run();
        return Duration.ofNanos(System.nanoTime() - start);
    }

}
