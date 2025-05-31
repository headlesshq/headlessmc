package io.github.headlesshq.headlessmc.logging;

import io.github.headlesshq.headlessmc.logging.handlers.HmcStreamHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.logging.Level.*;
import static org.junit.jupiter.api.Assertions.*;

public class LoggingServiceTest {
    private static final Path ROOT = Paths.get("HeadlessMC");

    private LoggingService loggingService;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @BeforeAll
    public static void beforeAll() throws IOException {
        Thread daemon = new Thread(() -> {
            while (true) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(Integer.MAX_VALUE);
                } catch (InterruptedException ignored) {
                }
            }
        });

        daemon.setName("Daemon-To-Get-Better-Branch-Coverage-For-getThreadId-In-ThreadFormatter");
        daemon.setDaemon(true);
        daemon.start();

        if (Files.exists(ROOT)) {
            try (Stream<Path> pathStream = Files.walk(Paths.get("HeadlessMC"))) {
                //noinspection ResultOfMethodCallIgnored
                pathStream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    @BeforeEach
    public void setUp() {
        loggingService = new LoggingService();
        loggingService.setStreamFactory(() -> new PrintStream(outContent));
        loggingService.init();
    }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void testSetLevel() {
        assertTrue(loggingService.setLevel("INFO"));
        Logger rootLogger = Logger.getLogger("");
        assertEquals(INFO, Arrays.stream(rootLogger.getHandlers()).filter(h -> h instanceof HmcStreamHandler).findFirst().get().getLevel());

        assertTrue(loggingService.setLevel("DEBUG"));
        assertEquals(FINE, Arrays.stream(rootLogger.getHandlers()).filter(h -> h instanceof HmcStreamHandler).findFirst().get().getLevel());

        assertTrue(loggingService.setLevel("ERROR"));
        assertEquals(SEVERE, Arrays.stream(rootLogger.getHandlers()).filter(h -> h instanceof HmcStreamHandler).findFirst().get().getLevel());

        assertFalse(loggingService.setLevel("ThisLevelDoesNotExist!"));
    }

    @Test
    public void testSetFileHandlerLogLevel() {
        Logger rootLogger = Logger.getLogger("");
        // Assuming INFO is the level set in init
        loggingService.setFileHandlerLogLevel(SEVERE);
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof FileHandler) {
                assertEquals(SEVERE, handler.getLevel());
            }
        }
    }

    @Test
    public void testAddLoggingHandler() {
        Logger rootLogger = Logger.getLogger("");
        loggingService.addLoggingHandler();
        boolean handlerFound = false;
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof HmcStreamHandler) {
                handlerFound = true;
                break;
            }
        }
        assertTrue(handlerFound);
    }

    @Test
    public void testLogOutput() {
        Logger logger = Logger.getLogger(LoggingServiceTest.class.getName());
        loggingService.setLevel(ALL);
        logger.log(INFO, "Test Message", new Exception("Stacktrace"));

        String loggedOutput = outContent.toString();
        assertTrue(loggedOutput.contains("Test Message"));
        assertTrue(loggedOutput.contains("INFO"));
        assertTrue(loggedOutput.contains("at io.github.headlesshq.headlessmc.logging.LoggingServiceTest.testLogOutput(LoggingServiceTest"));

        logger.info((String) null);
        loggedOutput = outContent.toString();
        assertTrue(loggedOutput.contains("null"));

        loggingService.addFileHandler(ROOT.resolve("headlessmc.log").resolve("invalid-because-headlessmc.log-exists-and-is-not-a-dir"));
        loggedOutput = outContent.toString();
        assertTrue(loggedOutput.contains("Failed to create directories for path"));
        assertTrue(loggedOutput.contains(FileAlreadyExistsException.class.getName()));

        io.github.headlesshq.headlessmc.logging.Logger hmcLogger = new io.github.headlesshq.headlessmc.logging.Logger(logger);
        loggingService.setLevel(FINE);
        logger.setLevel(FINE);
        loggingService.setLevel(INFO, true);
        hmcLogger.debug("message1", new Exception("Stacktrace1"));
        hmcLogger.info(new Exception("Stacktrace2"));
        hmcLogger.error(new Exception("Stacktrace3"));
        hmcLogger.warn("message2", new Exception("Stacktrace4"));
        hmcLogger.warn(new Exception("Stacktrace5"));
        hmcLogger.debug("message3");
        hmcLogger.warn("message4");

        loggedOutput = outContent.toString();
        for (int i = 1; i <= 5; i++) {
            assertTrue(loggedOutput.contains("Stacktrace" + i), "Should contain Stacktrace" + i);
        }

        for (int i = 1; i <= 4; i++) {
            assertTrue(loggedOutput.contains("message" + i), "Should contain message" + i);
        }
    }

    @Test
    public void testFileHandlerOutput() throws Exception {
        Path testLogPath = Paths.get("HeadlessMC", "headlessmc_test.log");

        // Clean test log
        if (Files.exists(testLogPath)) {
            Files.delete(testLogPath);
        }

        loggingService.addFileHandler(testLogPath);

        Logger logger = Logger.getLogger(LoggingServiceTest.class.getName());
        logger.info("File Handler Test Message");

        assertTrue(Files.exists(testLogPath));
        String logContents = new String(Files.readAllBytes(testLogPath));
        assertTrue(logContents.contains("File Handler Test Message"));
    }

    @Test
    public void testFormatter() {
        ThreadFormatter formatter = new ThreadFormatter();
        LogRecord record = new LogRecord(INFO, "This is a formatted message");
        record.setThreadID((int) Thread.currentThread().getId());
        record.setLoggerName("TestLogger");

        String formatted = formatter.format(record);
        assertTrue(formatted.contains("This is a formatted message"));
        assertTrue(formatted.contains("TestLogger"));
    }

    @Test
    public void testGetLevel() {
        List<Level> levels = new ArrayList<>();
        loggingService.getLevels().forEach(levels::add);
        List<Level> expected = asList(OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL);
        assertEquals(expected, levels);
    }

}
