package io.github.headlesshq.headlessmc.java.launcher;

import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import io.github.headlesshq.headlessmc.java.Java;
import lombok.Data;
import org.jspecify.annotations.Nullable;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "ClassCanBeRecord"})
public class JavaProcessImpl implements JavaProcess {
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
    private final Optional<Process> process;

    @Override
    public int waitFor(@Nullable Duration duration) {
        Process process = this.process.orElse(null);
        if (process != null) {
            try {
                if (duration == null) {
                    return process.waitFor();
                } else {
                    if (process.waitFor(duration)) {
                        return process.exitValue();
                    } else {
                        throw new JavaProcessException(
                            "Timeout while waiting " + duration + " for process"
                        );
                    }
                }
            } catch (InterruptedException e) {
                throw new UncheckedInterruptedException(e);
            }
        }

        return 0;
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
                handler.handle(this.getId(), result);
            }

            return result;
        } catch (UncheckedInterruptedException e) {
            if (handler != null) {
                handler.interrupted(this);
            }

            throw e;
        }
    }

    @Override
    public void kill() {
        process.ifPresent(Process::destroy);
    }

    @Override
    public void killForcibly() {
        process.ifPresent(Process::destroyForcibly);
    }

}
