package io.github.headlesshq.headlessmc.logging;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.formatters.ColorPatternFormatter;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.logmanager.handlers.PeriodicSizeRotatingFileHandler;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Attaches a log file in {@link AppFiles#getLogDir()} to the root logger and
 * limits the console handlers Quarkus set up to {@link LogConfig#consoleLevel()},
 * formatting what they print with the {@link ConsoleFormatter}.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class LoggingInitializer {
    static final String FILE_FORMAT = "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n";
    static final String ROTATION_SUFFIX = ".yyyy-MM-dd";

    private final AppFiles appFiles;
    private final FileService fileService;
    private final Holder<LogConfig> config;

    private @Nullable Handler fileHandler;

    void onStart(@Observes StartupEvent event) {
        initialize();
    }

    void onShutdown(@Observes ShutdownEvent event) {
        close();
    }

    public void initialize() {
        Logger root = LogManager.getLogManager().getLogger("");
        if (root != null) {
            initialize(root);
        }
    }

    public synchronized void close() {
        if (fileHandler != null) {
            fileHandler.close();
            fileHandler = null;
        }
    }

    @VisibleForTesting
    synchronized void initialize(Logger root) {
        LogConfig logConfig = config.get();
        for (Handler handler : root.getHandlers()) {
            configureConsoleHandlers(handler, logConfig.consoleLevel());
        }

        if (logConfig.file() && fileHandler == null) {
            Handler created = createFileHandler(logConfig);
            if (created != null) {
                fileHandler = created;
                root.addHandler(created);
            }
        }

        Level level = logConfig.consoleLevel();
        if (fileHandler != null) {
            fileHandler.setLevel(logConfig.fileLevel());
            if (logConfig.fileLevel().intValue() < level.intValue()) {
                level = logConfig.fileLevel();
            }
        }

        Level current = root.getLevel();
        if (current == null || level.intValue() < current.intValue()) {
            root.setLevel(level);
        }
    }

    private void configureConsoleHandlers(Handler handler, Level level) {
        if (handler instanceof ConsoleHandler) {
            handler.setLevel(level);
            if (!(handler.getFormatter() instanceof ConsoleFormatter)) {
                handler.setFormatter(
                    new ConsoleFormatter(config, handler.getFormatter() instanceof ColorPatternFormatter)
                );
            }

            return;
        }

        if (handler instanceof ExtHandler extHandler) {
            for (Handler nested : extHandler.getHandlers()) {
                configureConsoleHandlers(nested, level);
            }
        }
    }

    private @Nullable Handler createFileHandler(LogConfig logConfig) {
        if (fileService.fs().isVirtual()) {
            log.debug("Not writing a log file, because a virtual file system is used");
            return null;
        }

        Path directory = appFiles.getLogDir();
        try {
            Files.createDirectories(directory);
            PeriodicSizeRotatingFileHandler handler = new PeriodicSizeRotatingFileHandler();
            handler.setSuffix(ROTATION_SUFFIX);
            handler.setRotateSize(logConfig.maxFileSize().asLongValue());
            handler.setMaxBackupIndex(logConfig.maxBackups());
            handler.setFormatter(new PatternFormatter(FILE_FORMAT));
            handler.setAutoFlush(true);
            handler.setAppend(true);
            handler.setFile(directory.resolve(logConfig.fileName()).toFile());
            return handler;
        } catch (IOException | RuntimeException e) {
            log.warn("Failed to open a log file in {}", directory, e);
            return null;
        }
    }

}
