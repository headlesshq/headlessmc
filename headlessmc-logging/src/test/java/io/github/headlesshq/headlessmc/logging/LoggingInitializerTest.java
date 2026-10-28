package io.github.headlesshq.headlessmc.logging;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.console.output.SecretService;
import io.github.headlesshq.headlessmc.console.output.SecretServiceImpl;
import io.github.headlesshq.headlessmc.files.*;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.logmanager.handlers.DelayedHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class LoggingInitializerTest {
    private static final String SECRET = "super-secret-token";

    private final FileService fileService = new DefaultFileService(new DefaultFileSystemProvider());
    private final Logger logger = Logger.getLogger(getClass().getName() + "." + UUID.randomUUID());
    private TestLogConfig config = new TestLogConfig();
    private final Holder<LogConfig> holder = () -> config;
    private final SecretService secretService = new SecretServiceImpl(List.of(() -> List.of(SECRET)));

    LoggingInitializerTest() {
        logger.setUseParentHandlers(false);
    }

    @AfterEach
    void tearDown() {
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
    }

    private LoggingInitializer initializer(Path root) {
        return new LoggingInitializer(TestFiles.appFiles(root), fileService, holder, secretService);
    }

    private Path logFile(Path root) {
        return TestFiles.appFiles(root).getLogDir().resolve("headlessmc.log");
    }

    @Test
    void writesRecordsDownToTheFileLevelIntoTheLogFile(@TempDir Path root) throws Exception {
        LoggingInitializer initializer = initializer(root);

        initializer.initialize(logger);
        try {
            assertEquals(Level.DEBUG.intValue(), logger.getLevel().intValue());
            logger.log(Level.DEBUG, "a debug message");
            logger.log(Level.WARN, "a warning");
        } finally {
            initializer.close();
        }

        String content = Files.readString(logFile(root));
        assertTrue(content.contains("a debug message"), content);
        assertTrue(content.contains("a warning"), content);
    }

    @Test
    void keepsTheFullStacktraceInTheLogFileEvenIfTheConsoleDoesNot(@TempDir Path root) throws Exception {
        LoggingInitializer initializer = initializer(root);

        initializer.initialize(logger);
        try {
            assertFalse(config.stacktraces());
            logger.log(Level.WARN, "it broke", new IllegalStateException("the cause"));
        } finally {
            initializer.close();
        }

        String content = Files.readString(logFile(root));
        assertTrue(content.contains("java.lang.IllegalStateException: the cause"), content);
        assertTrue(content.contains(getClass().getName()), content);
    }

    @Test
    void writesNoLogFileWhenItIsDisabled(@TempDir Path root) {
        config = new TestLogConfig(false, Level.WARN, Level.DEBUG, false);
        LoggingInitializer initializer = initializer(root);

        initializer.initialize(logger);

        assertFalse(Files.exists(logFile(root)));
        assertEquals(Level.WARN.intValue(), logger.getLevel().intValue());
    }

    @Test
    void writesNoLogFileOnAVirtualFileSystem(@TempDir Path root) {
        FileService virtual = new DefaultFileService(new FileSystemProvider() {
            @Override
            public FileSystem getFileSystem() {
                return FileSystems.getDefault();
            }

            @Override
            public boolean isVirtual() {
                return true;
            }
        });

        new LoggingInitializer(TestFiles.appFiles(root), virtual, holder, secretService).initialize(logger);

        assertFalse(Files.exists(logFile(root)));
    }

    @Test
    void addsTheLogFileOnlyOnce(@TempDir Path root) {
        LoggingInitializer initializer = initializer(root);

        initializer.initialize(logger);
        initializer.initialize(logger);
        initializer.close();

        assertEquals(1, logger.getHandlers().length);
    }

    @Test
    void doesNotRaiseALevelWhichIsAlreadyMoreVerbose(@TempDir Path root) {
        logger.setLevel(Level.TRACE);
        LoggingInitializer initializer = initializer(root);

        initializer.initialize(logger);
        initializer.close();

        assertEquals(Level.TRACE.intValue(), logger.getLevel().intValue());
    }

    @Test
    void limitsConsoleHandlersToTheConsoleLevelAndCompactsTheirExceptions(@TempDir Path root) {
        ConsoleHandler console = consoleHandler();
        logger.addHandler(console);
        LoggingInitializer initializer = initializer(root);

        initializer.initialize(logger);
        initializer.close();

        assertEquals(Level.WARN.intValue(), console.getLevel().intValue());
        assertConsoleFormatter(console);
    }

    @Test
    void alsoReachesConsoleHandlersWrappedInAnotherHandler(@TempDir Path root) {
        ConsoleHandler console = consoleHandler();
        DelayedHandler delayed = new DelayedHandler();
        delayed.setHandlers(new Handler[]{console});
        logger.addHandler(delayed);
        LoggingInitializer initializer = initializer(root);

        initializer.initialize(logger);
        initializer.close();

        assertEquals(Level.WARN.intValue(), console.getLevel().intValue());
        assertConsoleFormatter(console);
    }

    @Test
    void doesNotWrapTheConsoleFormatterTwice(@TempDir Path root) {
        ConsoleHandler console = consoleHandler();
        logger.addHandler(console);
        LoggingInitializer initializer = initializer(root);

        initializer.initialize(logger);
        java.util.logging.Formatter formatter = console.getFormatter();
        initializer.initialize(logger);
        initializer.close();

        assertSame(formatter, console.getFormatter());
    }

    @Test
    void removesSecretsFromTheLogFile(@TempDir Path root) throws Exception {
        LoggingInitializer initializer = initializer(root);

        initializer.initialize(logger);
        try {
            logger.log(Level.WARN, "token " + SECRET, new IllegalStateException("failed with " + SECRET));
        } finally {
            initializer.close();
        }

        String content = Files.readString(logFile(root));
        assertFalse(content.contains(SECRET), content);
        assertTrue(content.contains("token " + SecretService.MASK), content);
        assertTrue(content.contains("failed with " + SecretService.MASK), content);
    }

    @Test
    void removesSecretsFromTheConsole(@TempDir Path root) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleHandler console = consoleHandler(output);
        logger.addHandler(console);
        LoggingInitializer initializer = initializer(root);

        initializer.initialize(logger);
        logger.log(Level.ERROR, "failed to use " + SECRET);
        console.flush();
        initializer.close();

        String content = output.toString();
        assertFalse(content.contains(SECRET), content);
        assertTrue(content.contains("failed to use " + SecretService.MASK), content);
    }

    private static void assertConsoleFormatter(Handler handler) {
        CensoringFormatter formatter = assertInstanceOf(CensoringFormatter.class, handler.getFormatter());
        assertInstanceOf(ConsoleFormatter.class, formatter.getFormatter());
    }

    private ConsoleHandler consoleHandler() {
        // do not write to the terminal running the tests
        return consoleHandler(new ByteArrayOutputStream());
    }

    private ConsoleHandler consoleHandler(ByteArrayOutputStream output) {
        ConsoleHandler handler = new ConsoleHandler(new PatternFormatter("%p %s%e%n"));
        handler.setOutputStream(output);
        return handler;
    }

}
