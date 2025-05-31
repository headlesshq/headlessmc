package io.github.headlesshq.headlessmc.launcher.download;

import io.github.headlesshq.headlessmc.launcher.util.IOConsumer;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@CustomLog
@RequiredArgsConstructor
public class ParallelIOService {
    private final List<IOConsumer<String>> tasks = new ArrayList<>();
    private final long delay;
    private final long retries;
    private final boolean parallel;
    private final boolean backoff;

    @Setter
    private boolean shouldLog = true;

    public void addTask(IOConsumer<String> task) {
        tasks.add(task);
    }

    public void execute() throws IOException {
        long nanos = System.nanoTime();
        int total = tasks.size();
        AtomicInteger count = new AtomicInteger();
        AtomicReference<IOException> failed = new AtomicReference<>();
        Stream<IOConsumer<String>> stream = parallel ? tasks.parallelStream() : tasks.stream();
        //noinspection ResultOfMethodCallIgnored
        stream.anyMatch(task -> {
            run(task, total, count, failed);
            return failed.get() != null; // end stream early if an asset failed completely
        });

        nanos = System.nanoTime() - nanos;
        if (shouldLog) {
            log.info("Download took " + (nanos / 1_000_000.0) + "ms, parallel: " + parallel);
        }
        if (failed.get() != null) {
            throw failed.get();
        }
    }

    public String updateProgress(AtomicInteger count, int total, IOConsumer<String> task) {
        // TODO: https://github.com/ctongfei/progressbar
        int downloaded = count.incrementAndGet();
        String percentage = String.format("%d", (downloaded * 100 / total)) + "%";
        String progress =  percentage + " (" + downloaded + "/" + total + ")";
        if (shouldLog) {
            log.debug(progress + " Checking " + task);
        }

        return progress;
    }

    @SneakyThrows
    private void run(IOConsumer<String> task, int total, AtomicInteger count, AtomicReference<IOException> failed) {
        String progress = updateProgress(count, total, task);
        IOException exception = null;
        for (int i = 0; i < retries; i++) {
            try {
                long wait = this.delay;
                if (backoff) {
                    wait *= (i + 1); // increase wait time
                }

                if (wait > 0L) {
                    Thread.sleep(wait);
                }

                task.accept(progress);
                return; // downloaded successfully, return
            } catch (IOException e) {
                log.warn(progress + " Failed to download " + task + ", retrying...", e);
                exception = e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                exception = new IOException("Thread interrupted");
                break;
            } catch (Throwable throwable) {
                log.error("Failure in download thread for " + task, throwable);
                throw throwable;
            }
        }

        // exception is always != null at this point
        log.error("Failed to download asset " + task + " after " + retries + " tries!");
        failed.set(exception);
    }

}
