package io.github.headlesshq.headlessmc.java.launcher;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import io.github.headlesshq.headlessmc.java.Java;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface JavaProcess {
    String getId();

    Map<String, @Nullable String> getSystemProperties();

    List<String> getJvmArgs();

    List<String> getArgs();

    List<String> getClassPath();

    Optional<Integer> getJavaVersion();

    Optional<Java> getJava();

    Optional<String> getMainClass();

    Optional<Path> getDirectory();

    Optional<Path> getJar();

    Optional<Process> getProcess();

    int waitFor(@Nullable Duration duration);

    int waitFor(@Nullable ProcessHandler handler)
        throws HeadlessMcException, UncheckedInterruptedException;

    int waitFor(@Nullable ProcessHandler handler, @Nullable Duration duration)
        throws HeadlessMcException, UncheckedInterruptedException;

    void kill();

    void killForcibly();

}
