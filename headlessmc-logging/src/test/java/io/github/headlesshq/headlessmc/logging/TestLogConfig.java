package io.github.headlesshq.headlessmc.logging;

import io.quarkus.runtime.configuration.MemorySize;
import org.jboss.logmanager.Level;

import java.math.BigInteger;

/**
 * A {@link LogConfig} which does not need a config to be built.
 */
record TestLogConfig(
    boolean file,
    java.util.logging.Level consoleLevel,
    java.util.logging.Level fileLevel,
    boolean stacktraces
) implements LogConfig {
    TestLogConfig() {
        this(true, Level.WARN, Level.DEBUG, false);
    }

    TestLogConfig withStacktraces(boolean stacktraces) {
        return new TestLogConfig(file, consoleLevel, fileLevel, stacktraces);
    }

    @Override
    public String fileName() {
        return "headlessmc.log";
    }

    @Override
    public MemorySize maxFileSize() {
        return new MemorySize(BigInteger.valueOf(10 * 1024 * 1024));
    }

    @Override
    public int maxBackups() {
        return 5;
    }

}
