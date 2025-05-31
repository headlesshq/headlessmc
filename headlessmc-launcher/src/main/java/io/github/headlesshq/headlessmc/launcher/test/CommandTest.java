package io.github.headlesshq.headlessmc.launcher.test;

import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@CustomLog
@RequiredArgsConstructor
public class CommandTest implements AutoCloseable {
    public static final String SERVER_TEST_RESOURCE = "test/hmc-server-test.json";

    private final AtomicReference<String> message = new AtomicReference<>();
    private final AtomicBoolean success = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final Object lock = new Object();

    private final Launcher launcher;
    private final TestCase testCase;
    private final Process process;

    private volatile TestCaseRunner testCaseRunner;
    private volatile TimeoutHandler timeoutHandler;
    private volatile Thread thread;

    public void run() {
        if (thread != null) {
            throw new IllegalStateException("CommandTest is already running");
        }

        Thread mainThread = Thread.currentThread();
        timeoutHandler = new TimeoutHandler(Executors.newSingleThreadScheduledExecutor(), () -> {
            stopped.set(true);
            success.set(false);
            message.set("Timed out!");
            mainThread.interrupt();
        });

        testCaseRunner = new TestCaseRunner(testCase, timeoutHandler);

        thread = new Thread(() -> {
            testCaseRunner.updateTimeout();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    launcher.log(line);
                    synchronized (lock) {
                        if (stopped.get()) {
                            log.info("CommandTest Thread stopped.");
                            return;
                        }

                        TestCase.Result result = testCaseRunner.runStep(process, line);
                        log.debug("Result: " + result);
                        switch (result) {
                            case MATCH:
                                log.info("Matched Line.");
                                break;
                            case END_SUCCESS:
                                success.set(true);
                                return;
                            case END_FAIL:
                                success.set(false);
                                return;
                            default:
                                break;
                        }
                    }
                }
            } catch (Throwable t) {
                log.error(t);
                message.set(t.getMessage());
                success.set(false);
            }
        });

        thread.setDaemon(true);
        thread.setName("CommandTest");
        thread.start();

        try {
            if (testCase.getTotalTimeout() != null) {
                thread.join(TimeUnit.SECONDS.toMillis(testCase.getTotalTimeout()));
            } else if (launcher.getConfig().get(LauncherProperties.NO_TEST_TIMEOUT, false)) {
                thread.join();
            } else {
                thread.join(TimeUnit.MINUTES.toMillis(5L));
            }
        } catch (InterruptedException e) {
            if (!stopped.get()) {
                throw new TestException("Unexpected Interrupt", e);
            }
        }
    }

    public boolean wasSuccessful() {
        return success.get();
    }

    public @Nullable String getMessage() {
        return message.get();
    }

    public void awaitExitOrKill() throws InterruptedException {
        if (!process.waitFor(2, TimeUnit.MINUTES)) {
            process.destroyForcibly();
        }
    }

    @Override
    public void close() {
        if (timeoutHandler != null) {
            timeoutHandler.close();
        }
    }

    public static @Nullable CommandTest create(Process process, Launcher launcher) throws IOException {
        TestCase test;
        if (launcher.getConfig().get(LauncherProperties.SERVER_TEST, false)) {
            try (InputStream is = CommandTest.class.getClassLoader().getResourceAsStream(SERVER_TEST_RESOURCE)) {
                test = TestCase.load(is);
            }
        } else {
            String fileName = launcher.getConfig().get(LauncherProperties.TEST_FILE, null);
            if (fileName == null) {
                return null;
            }

            try (InputStream is = Files.newInputStream(Paths.get(fileName))) {
                test = TestCase.load(is);
            }
        }

        if (process == null) {
            throw new IllegalArgumentException("Cannot create CommandTest for In-Memory process!");
        }

        return new CommandTest(launcher, test, process);
    }

}

