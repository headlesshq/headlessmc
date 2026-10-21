package io.github.headlesshq.headlessmc.java.inmemory;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcess;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessException;
import io.github.headlesshq.headlessmc.java.launcher.ProcessHandler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link JavaProcess} running in this JVM, started by the {@link InMemoryProcessBuilder}.
 * Like a real JVM, the process ends when its main thread and all non-daemon threads it started,
 * which all belong to the {@link ThreadGroup} of the process, have ended.
 */
@Slf4j
@Getter
@RequiredArgsConstructor
@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "ClassCanBeRecord"})
public class InMemoryJavaProcess implements JavaProcess {
    private final String id;
    private final Map<String, @Nullable String> systemProperties;
    private final List<String> classPath;
    private final List<String> jvmArgs;
    private final List<String> args;
    private final Optional<Java> java;
    private final Optional<Integer> javaVersion;
    private final Optional<String> mainClass;
    private final Optional<Path> directory;
    private final Optional<Path> jar;
    private final URLClassLoader classLoader;
    @Getter(lombok.AccessLevel.NONE)
    private final ThreadGroup threadGroup = new ThreadGroup("in-memory process");
    @Getter(lombok.AccessLevel.NONE)
    private volatile int exitCode;
    @Getter(lombok.AccessLevel.NONE)
    private volatile boolean closed;

    void start(String mainClass, String[] args) {
        Thread main = new Thread(threadGroup, () -> runMain(mainClass, args), id + " main");
        main.setContextClassLoader(classLoader);
        main.start();
    }

    private void runMain(String mainClass, String[] args) {
        try {
            Class<?> mainClassClass = Class.forName(mainClass, true, classLoader);
            Method main = mainClassClass.getMethod("main", String[].class);
            main.invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            log.error("Exception in {} main thread", id, e.getCause());
            exitCode = 1;
        } catch (ReflectiveOperationException | LinkageError e) {
            log.error("Failed to run main class {} of {}", mainClass, id, e);
            exitCode = 1;
        }
    }

    @Override
    public Optional<Process> getProcess() {
        return Optional.empty();
    }

    @Override
    public int waitFor(@Nullable Duration duration) {
        long deadline = duration == null ? Long.MAX_VALUE : System.nanoTime() + duration.toNanos();
        try {
            for (Thread thread = nextAliveThread(); thread != null; thread = nextAliveThread()) {
                if (duration == null) {
                    thread.join();
                } else {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0) {
                        throw new JavaProcessException("Timeout while waiting " + duration + " for process");
                    }

                    thread.join(Math.max(1L, remaining / 1_000_000L));
                }
            }
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }

        close();
        return exitCode;
    }

    @Override
    public int waitFor(@Nullable ProcessHandler handler) throws HeadlessMcException, UncheckedInterruptedException {
        return waitFor(handler, null);
    }

    @Override
    public int waitFor(
        @Nullable ProcessHandler handler,
        @Nullable Duration duration
    ) throws HeadlessMcException, UncheckedInterruptedException {
        try {
            int result = waitFor(duration);
            if (handler != null) {
                handler.handle(id, result);
            }

            return result;
        } catch (UncheckedInterruptedException e) {
            if (handler != null) {
                handler.interrupted(this);
            }

            throw e;
        }
    }

    /**
     * Interrupts all threads of the process. Threads cannot be stopped forcibly,
     * so a program that ignores interrupts keeps running.
     */
    @Override
    public void kill() {
        for (Thread thread : threads()) {
            thread.interrupt();
        }
    }

    @Override
    public void killForcibly() {
        kill();
    }

    private @Nullable Thread nextAliveThread() {
        for (Thread thread : threads()) {
            if (thread.isAlive() && !thread.isDaemon()) {
                return thread;
            }
        }

        return null;
    }

    private Thread[] threads() {
        Thread[] threads = new Thread[threadGroup.activeCount() + 16];
        int count = threadGroup.enumerate(threads, true);
        return Arrays.copyOf(threads, count);
    }

    private synchronized void close() {
        if (!closed) {
            closed = true;
            try {
                classLoader.close();
            } catch (IOException e) {
                log.warn("Failed to close classloader of {}", id, e);
            }
        }
    }

}
