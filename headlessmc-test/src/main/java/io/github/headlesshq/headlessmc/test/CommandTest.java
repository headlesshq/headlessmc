package io.github.headlesshq.headlessmc.test;

import io.quarkus.runtime.annotations.RegisterResources;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

// Legacy HeadlessMc code, rewrite candidate
@Slf4j
@RequiredArgsConstructor
@RegisterResources(globs = CommandTest.SERVER_TEST_RESOURCE)
public class CommandTest implements AutoCloseable {
    public static final String SERVER_TEST_RESOURCE = "test/hmc-server-test.json";

    private final AtomicReference<@Nullable String> message = new AtomicReference<>();
    private final AtomicBoolean success = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final Object lock = new Object();

    private final Consumer<String> output;
    private final TestCase testCase;
    private final Process process;
    private final boolean noTimeout;

    // TODO: fix nullability issues, e.g. with a state variable
    private volatile @Nullable TestCaseRunner testCaseRunner;
    private volatile @Nullable TimeoutHandler timeoutHandler;
    private volatile @Nullable Thread thread;

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
                    output.accept(line);
                    synchronized (lock) {
                        if (stopped.get()) {
                            log.info("CommandTest Thread stopped.");
                            return;
                        }

                        TestCase.Result result = testCaseRunner.runStep(process, line);
                        log.debug("Result: {}", result);
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
                log.error("Encountered an exception while reading process output", t);
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
            } else if (noTimeout) {
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

}


