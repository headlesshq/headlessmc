package io.github.headlesshq.headlessmc.launcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// file system events can take a few seconds on platforms without native watch support (e.g. macOS)
@Timeout(60)
class CrashReportWatcherTest {
    private static final long EVENT_TIMEOUT = 30;

    @TempDir
    Path gameDir;

    private static Path poll(BlockingQueue<Path> queue) throws InterruptedException {
        Path result = queue.poll(EVENT_TIMEOUT, TimeUnit.SECONDS);
        assertNotNull(result, "No crash report event received");
        return result;
    }

    private Path report(String name) {
        return gameDir.resolve("crash-reports").resolve(name).toAbsolutePath();
    }

    @Test
    void reportedPathsAreAbsoluteForRelativeGameDirs() throws Exception {
        // a relative path to the temp dir, resolved against the working directory
        Path workingDir = Path.of("").toAbsolutePath();
        assumeTrue(workingDir.getRoot().equals(gameDir.getRoot()), "temp dir is on another drive");
        Path relativeGameDir = workingDir.relativize(gameDir);
        assertFalse(relativeGameDir.isAbsolute());
        BlockingQueue<Path> events = new LinkedBlockingQueue<>();
        try (CrashReportWatcher watcher = CrashReportWatcher.forGameDir(relativeGameDir)) {
            watcher.addListener(events::add);
            watcher.waitForStart();

            Files.writeString(gameDir.resolve("crash-reports").resolve("crash.txt"), "crash");

            Path reported = poll(events);
            assertTrue(reported.isAbsolute(), reported.toString());
            assertTrue(Files.isSameFile(report("crash.txt"), reported));
        }
    }

    @Test
    void forGameDirCreatesTheCrashReportsDirectory() throws IOException {
        try (CrashReportWatcher ignored = CrashReportWatcher.forGameDir(gameDir)) {
            assertTrue(Files.isDirectory(gameDir.resolve("crash-reports")));
        }
    }

    @Test
    void forGameDirAcceptsAnExistingCrashReportsDirectory() throws IOException {
        Files.createDirectories(gameDir.resolve("crash-reports"));
        Files.writeString(gameDir.resolve("crash-reports").resolve("old-crash.txt"), "old");

        try (CrashReportWatcher ignored = CrashReportWatcher.forGameDir(gameDir)) {
            assertTrue(Files.exists(gameDir.resolve("crash-reports").resolve("old-crash.txt")));
        }
    }

    @Test
    void forGameDirFailsIfTheCrashReportsDirectoryCannotBeCreated() throws IOException {
        Path file = gameDir.resolve("file");
        Files.writeString(file, "not a directory");

        assertThrows(IOException.class, () -> CrashReportWatcher.forGameDir(file));
    }

    @Test
    void watchingAMissingDirectoryFails() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            assertThrows(
                NoSuchFileException.class,
                () -> new CrashReportWatcher(gameDir.resolve("missing"), executor, false)
            );
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void listenersAreNotifiedAboutNewCrashReports() throws Exception {
        BlockingQueue<Path> first = new LinkedBlockingQueue<>();
        BlockingQueue<Path> second = new LinkedBlockingQueue<>();
        try (CrashReportWatcher watcher = CrashReportWatcher.forGameDir(gameDir)) {
            watcher.addListener(first::add);
            watcher.addListener(second::add);
            watcher.waitForStart();

            Files.writeString(gameDir.resolve("crash-reports").resolve("crash-2024-01-01.txt"), "crash");

            assertEquals(report("crash-2024-01-01.txt"), poll(first));
            assertEquals(report("crash-2024-01-01.txt"), poll(second));
        }
    }

    @Test
    void everyCrashReportIsReported() throws Exception {
        BlockingQueue<Path> events = new LinkedBlockingQueue<>();
        try (CrashReportWatcher watcher = CrashReportWatcher.forGameDir(gameDir)) {
            watcher.addListener(events::add);
            watcher.waitForStart();

            Path crashReports = gameDir.resolve("crash-reports");
            Files.writeString(crashReports.resolve("crash-1.txt"), "crash");
            assertEquals(report("crash-1.txt"), poll(events));

            // after the key has been reset, further events are received
            Files.writeString(crashReports.resolve("crash-2.txt"), "crash");
            assertEquals(report("crash-2.txt"), poll(events));
        }
    }

    @Test
    void filesOutsideOfTheCrashReportsDirectoryAreIgnored() throws Exception {
        BlockingQueue<Path> events = new LinkedBlockingQueue<>();
        try (CrashReportWatcher watcher = CrashReportWatcher.forGameDir(gameDir)) {
            watcher.addListener(events::add);
            watcher.waitForStart();

            Files.writeString(gameDir.resolve("latest.log"), "log");
            Files.writeString(gameDir.resolve("crash-reports").resolve("crash.txt"), "crash");

            assertEquals(report("crash.txt"), poll(events));
            assertTrue(events.isEmpty(), events.toString());
        }
    }

    @Test
    void waitForStartReturnsIfTheWatcherHasAlreadyStarted() throws Exception {
        try (CrashReportWatcher watcher = CrashReportWatcher.forGameDir(gameDir)) {
            watcher.waitForStart();
            watcher.waitForStart();
        }
    }

    @Test
    void closeShutsDownAnOwnedExecutor() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Files.createDirectories(gameDir.resolve("crash-reports"));
        CrashReportWatcher watcher = new CrashReportWatcher(gameDir.resolve("crash-reports"), executor, true);
        watcher.waitForStart();

        watcher.close();

        assertTrue(executor.isShutdown());
        assertTrue(executor.awaitTermination(EVENT_TIMEOUT, TimeUnit.SECONDS), "Watcher task did not stop");
    }

    @Test
    void closeKeepsAForeignExecutorRunning() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Files.createDirectories(gameDir.resolve("crash-reports"));
            CrashReportWatcher watcher = new CrashReportWatcher(gameDir.resolve("crash-reports"), executor, false);
            watcher.waitForStart();

            watcher.close();

            assertFalse(executor.isShutdown());
            // the watcher task finishes, so the executor can be used for other tasks
            assertEquals("free", executor.submit(() -> "free").get(EVENT_TIMEOUT, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void interruptingTheWatcherStopsIt() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Files.createDirectories(gameDir.resolve("crash-reports"));
        try (CrashReportWatcher watcher = new CrashReportWatcher(gameDir.resolve("crash-reports"), executor, false)) {
            watcher.waitForStart();

            List<Runnable> notStarted = executor.shutdownNow();

            assertEquals(List.of(), notStarted);
            assertTrue(executor.awaitTermination(EVENT_TIMEOUT, TimeUnit.SECONDS), "Watcher task did not stop");
        }
    }

    @Test
    void noEventsAreReportedAfterClose() throws Exception {
        BlockingQueue<Path> events = new LinkedBlockingQueue<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Path crashReports = Files.createDirectories(gameDir.resolve("crash-reports"));
        CrashReportWatcher watcher = new CrashReportWatcher(crashReports, executor, true);
        watcher.addListener(events::add);
        watcher.waitForStart();

        watcher.close();
        assertTrue(executor.awaitTermination(EVENT_TIMEOUT, TimeUnit.SECONDS), "Watcher task did not stop");
        Files.writeString(crashReports.resolve("crash.txt"), "crash");

        assertTrue(events.isEmpty(), events.toString());
    }

}
