package io.github.headlesshq.headlessmc.logging;

import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.quarkus.runtime.configuration.MemorySize;
import io.quarkus.runtime.configuration.MemorySizeConverter;
import io.quarkus.runtime.logging.LevelConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

import java.util.logging.Level;

/**
 * Configuration for the log file HeadlessMc writes and
 * for how much of it ends up on the console.
 *
 * @implNote everything but {@link #stacktraces()} is read once,
 * when the log handlers are set up during startup,
 * changing it at runtime has no effect.
 */
@ConfigMapping(prefix = "hmc.log")
public interface LogConfig extends DynamicConfig {
    /**
     * @return whether HeadlessMc writes a log file at all.
     */
    @WithDefault("true")
    boolean file();

    /**
     * @return the name of the log file inside the log directory.
     */
    @WithDefault("headlessmc.log")
    String fileName();

    /**
     * @return the level records need to have to be printed to the console.
     */
    @WithDefault("WARN")
    @WithConverter(LevelConverter.class)
    Level consoleLevel();

    /**
     * @return the level records need to have to end up in the log file.
     * Levels below {@code quarkus.log.min-level} are never recorded.
     */
    @WithDefault("DEBUG")
    @WithConverter(LevelConverter.class)
    Level fileLevel();

    /**
     * @return whether the console prints full stacktraces.
     * If disabled, only the message of an exception and
     * the messages of its causes are printed.
     * The log file always contains the full stacktrace.
     */
    @WithDefault("false")
    boolean stacktraces();

    /**
     * @return the size the log file may reach before it is rotated.
     */
    @WithDefault("10M")
    @WithConverter(MemorySizeConverter.class)
    MemorySize maxFileSize();

    /**
     * @return how many rotated log files are kept.
     */
    @WithDefault("5")
    int maxBackups();

}
