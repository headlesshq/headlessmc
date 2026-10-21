package io.github.headlesshq.headlessmc.launcher;

import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcess;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessBuilder;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.launcher.process.McProcess;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.test.CommandTestService;
import io.github.headlesshq.headlessmc.test.TestConfig;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class ProcessLifecycleTest {
    private static final String TEST_FILE = """
        {
          "name": "Lifecycle Test",
          "implicitWaitForEnd": false,
          "steps": [
            { "type": "CONTAINS", "message": "ready" },
            { "type": "SEND", "message": "quit" },
            { "type": "SUCCESS" }
          ]
        }
        """;

    private static final String FAILING_TEST_FILE = """
        {
          "name": "Failing Test",
          "implicitWaitForEnd": false,
          "steps": [
            { "type": "CONTAINS", "message": "ready" },
            { "type": "FAIL" }
          ]
        }
        """;

    @TempDir
    Path root;

    private final RecordingConsole console = new RecordingConsole();

    /** Hands out the queued processes, one per launch. */
    private static final class QueueProcessLauncher implements ProcessLauncher {
        private final Deque<Supplier<McProcess>> processes = new ArrayDeque<>();
        private final List<Boolean> launches = new ArrayList<>();
        private final Path gameDir;

        QueueProcessLauncher(Path gameDir) {
            this.gameDir = gameDir;
        }

        QueueProcessLauncher then(Process process) {
            processes.add(() -> new McProcess("test", Optional.empty(), Optional.of(process)));
            return this;
        }

        QueueProcessLauncher then(Supplier<McProcess> process) {
            processes.add(process);
            return this;
        }

        @Override
        public McProcess launch(boolean pipeIO) throws HeadlessMcException {
            launches.add(pipeIO);
            Supplier<McProcess> process = processes.poll();
            assertNotNull(process, "Unexpected launch");
            return process.get();
        }

        @Override
        public Optional<JavaProcessBuilder> getJavaProcessBuilder() {
            return Optional.empty();
        }

        @Override
        public Optional<ProcessBuilder> getProcessBuilder() {
            return Optional.empty();
        }

        @Override
        public Path getGameDir() {
            return gameDir;
        }

        @Override
        public String getId() {
            return "test";
        }
    }

    private record Config(boolean crashReportWatcher) implements LauncherConfig {
        @Override
        public boolean autoInstall() {
            return false;
        }

        @Override
        public boolean free() {
            return true;
        }
    }

    private record Tests(boolean leave, Optional<String> file) implements TestConfig {
        static Tests none() {
            return new Tests(true, Optional.empty());
        }

        @Override
        public boolean server() {
            return false;
        }

        @Override
        public boolean noTimeout() {
            return false;
        }
    }

    private Path gameDir() {
        return root.resolve("game");
    }

    private ProcessLifecycle lifecycle(QueueProcessLauncher launcher, Config config, Tests tests, int retries) {
        ProcessLifecycle lifecycle = new ProcessLifecycle(
            launcher,
            new CommandTestService(() -> tests, new DefaultJacksonJsonService()),
            config,
            tests,
            console,
            launcher.getGameDir()
        );

        lifecycle.setRetries(retries);
        return lifecycle;
    }

    private ProcessLifecycle lifecycle(QueueProcessLauncher launcher, int retries) {
        return lifecycle(launcher, new Config(false), Tests.none(), retries);
    }

    private Tests testFile(String content, boolean leave) throws IOException {
        Path file = root.resolve("test.json");
        Files.writeString(file, content);
        return new Tests(leave, Optional.of(file.toString()));
    }

    @Test
    void launchWithoutProcessReturnsZero() {
        QueueProcessLauncher launcher = new QueueProcessLauncher(gameDir())
            .then(() -> new McProcess("script", Optional.empty(), Optional.empty()));

        assertEquals(0, lifecycle(launcher, 3).call());
        assertEquals(List.of(false), launcher.launches);
    }

    @Test
    void javaProcessWithoutProcessIsWaitedFor() {
        // e.g. an in-memory process, which runs in this JVM and has no java.lang.Process
        AtomicInteger waits = new AtomicInteger();
        JavaProcess javaProcess = (JavaProcess) Proxy.newProxyInstance(
            JavaProcess.class.getClassLoader(),
            new Class<?>[] {JavaProcess.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "waitFor" -> {
                    waits.incrementAndGet();
                    yield 3;
                }
                case "getProcess" -> Optional.empty();
                case "getId" -> "in-memory";
                default -> throw new UnsupportedOperationException(method.getName());
            }
        );

        QueueProcessLauncher launcher = new QueueProcessLauncher(gameDir())
            .then(() -> new McProcess("in-memory", Optional.of(javaProcess), Optional.empty()));

        assertEquals(3, lifecycle(launcher, 0).call());
        assertEquals(1, waits.get());
    }

    @Test
    void successfulProcessIsLaunchedOnce() {
        QueueProcessLauncher launcher = new QueueProcessLauncher(gameDir()).then(TestProcess.exiting(0));

        assertEquals(0, lifecycle(launcher, 3).call());
        assertEquals(List.of(false), launcher.launches);
    }

    @Test
    void failedProcessIsNotRetriedByDefault() {
        QueueProcessLauncher launcher = new QueueProcessLauncher(gameDir()).then(TestProcess.exiting(1));

        assertEquals(1, lifecycle(launcher, 0).call());
        assertEquals(1, launcher.launches.size());
    }

    @Test
    void failedProcessesAreRetried() {
        QueueProcessLauncher launcher = new QueueProcessLauncher(gameDir())
            .then(TestProcess.exiting(1))
            .then(TestProcess.exiting(2))
            .then(TestProcess.exiting(3));

        assertEquals(3, lifecycle(launcher, 2).call());
        assertEquals(3, launcher.launches.size());
    }

    @Test
    void retriesStopOnceTheProcessSucceeds() {
        QueueProcessLauncher launcher = new QueueProcessLauncher(gameDir())
            .then(TestProcess.exiting(1))
            .then(TestProcess.exiting(0));

        assertEquals(0, lifecycle(launcher, 5).call());
        assertEquals(2, launcher.launches.size());
    }

    @Test
    void launchExceptionsArePropagated() {
        QueueProcessLauncher launcher = new QueueProcessLauncher(gameDir())
            .then(() -> {
                throw new LaunchException("boom");
            });

        LaunchException e = assertThrows(LaunchException.class, () -> lifecycle(launcher, 3).call());
        assertEquals("boom", e.getMessage());
        assertEquals(1, launcher.launches.size());
    }

    @Test
    void retriesCanBeConfigured() {
        ProcessLifecycle lifecycle = lifecycle(new QueueProcessLauncher(gameDir()), 0);
        lifecycle.setRetries(4);

        assertEquals(4, lifecycle.getRetries());
        assertEquals(gameDir(), lifecycle.getGameDir());
    }

    @Test
    void commandTestsPipeIoAndWriteOutputToTheConsole() throws IOException {
        TestProcess process = TestProcess.exiting(0, "booting", "ready");
        QueueProcessLauncher launcher = new QueueProcessLauncher(gameDir()).then(process);

        assertEquals(0, lifecycle(launcher, new Config(false), testFile(TEST_FILE, false), 0).call());

        assertEquals(List.of(true), launcher.launches);
        assertEquals(List.of("booting", "ready"), console.lines());
        assertEquals("quit" + System.lineSeparator(), process.getWrittenInput());
        assertFalse(process.isDestroyed());
    }

    @Test
    void successfulCommandTestsWaitForTheProcessIfLeaveIsSet() throws IOException {
        // a process that does not exit on its own is killed after the test
        TestProcess process = TestProcess.running("ready");
        QueueProcessLauncher launcher = new QueueProcessLauncher(gameDir()).then(process);

        assertEquals(0, lifecycle(launcher, new Config(false), testFile(TEST_FILE, true), 0).call());

        assertTrue(process.isDestroyed());
    }

    @Test
    void failedCommandTestsKillTheProcessAndThrow() throws IOException {
        TestProcess process = TestProcess.running("ready");
        QueueProcessLauncher launcher = new QueueProcessLauncher(gameDir()).then(process);
        ProcessLifecycle lifecycle = lifecycle(launcher, new Config(false), testFile(FAILING_TEST_FILE, false), 3);

        LaunchException e = assertThrows(LaunchException.class, lifecycle::call);

        assertEquals("CommandTest failed", e.getMessage());
        assertTrue(process.isDestroyed());
        // failed tests are not retried
        assertEquals(1, launcher.launches.size());
    }

    @Test
    void missingTestFilesFailTheLaunch() {
        Tests tests = new Tests(false, Optional.of(root.resolve("missing.json").toString()));
        QueueProcessLauncher launcher = new QueueProcessLauncher(gameDir()).then(TestProcess.exiting(0));

        LaunchException e = assertThrows(
            LaunchException.class,
            () -> lifecycle(launcher, new Config(false), tests, 0).call()
        );

        assertEquals("Failed to load test", e.getMessage());
        assertInstanceOf(IOException.class, e.getCause());
    }

    @Test
    void crashReportWatcherIsNotCreatedByDefault() {
        QueueProcessLauncher launcher = new QueueProcessLauncher(gameDir()).then(TestProcess.exiting(0));

        assertEquals(0, lifecycle(launcher, 0).call());
        assertFalse(Files.exists(gameDir().resolve("crash-reports")));
    }

    @Test
    void crashReportWatcherDoesNotInterfereWithNormalExits() {
        QueueProcessLauncher launcher = new QueueProcessLauncher(gameDir())
            .then(TestProcess.exiting(1))
            .then(TestProcess.exiting(0));

        assertEquals(0, lifecycle(launcher, new Config(true), Tests.none(), 1).call());
        assertTrue(Files.isDirectory(gameDir().resolve("crash-reports")));
    }

    @Test
    void crashReportsKillTheProcessAndFailTheLaunch() {
        Path crashReport = gameDir().resolve("crash-reports").resolve("crash-client.txt");
        // the game "crashes" once it runs, but does not exit until it gets killed
        TestProcess process = new TestProcess(0, false) {
            @Override
            public int waitFor() throws InterruptedException {
                try {
                    Files.writeString(crashReport, "crash");
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }

                return super.waitFor();
            }
        };

        QueueProcessLauncher launcher = new QueueProcessLauncher(gameDir()).then(process);
        ProcessLifecycle lifecycle = lifecycle(launcher, new Config(true), Tests.none(), 0);

        LaunchException e = assertThrows(LaunchException.class, lifecycle::call);

        assertTrue(process.isDestroyed());
        assertTrue(e.getMessage().startsWith("CrashReport detected"), e.getMessage());
        assertEquals("CrashReport detected " + crashReport.toAbsolutePath(), e.getMessage());
    }

    @Test
    void failingToCreateTheCrashReportWatcherFailsTheLaunch() throws IOException {
        // the game dir is a file, so crash-reports cannot be created
        Files.createDirectories(root);
        Files.writeString(gameDir(), "not a directory");
        QueueProcessLauncher launcher = new QueueProcessLauncher(gameDir()).then(TestProcess.exiting(0));

        LaunchException e = assertThrows(
            LaunchException.class,
            () -> lifecycle(launcher, new Config(true), Tests.none(), 0).call()
        );

        assertInstanceOf(IOException.class, e.getCause());
        assertEquals(List.of(), launcher.launches);
    }

}
