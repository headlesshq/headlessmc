package io.github.headlesshq.headlessmc.logging;

import io.github.headlesshq.headlessmc.config.Holder;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Test;

import java.util.logging.Formatter;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleFormatterTest {
    private static final String LINE = System.lineSeparator();

    private TestLogConfig config = new TestLogConfig();
    private final Holder<LogConfig> holder = () -> config;
    private final Formatter formatter = new ConsoleFormatter(holder, false);
    private final Formatter colored = new ConsoleFormatter(holder, true);

    private ExtLogRecord record(java.util.logging.Level level, String message, Throwable thrown) {
        ExtLogRecord record = new ExtLogRecord(level, message, getClass().getName());
        record.setThrown(thrown);
        return record;
    }

    @Test
    void printsNothingButTheMessage() {
        assertEquals("Hello" + LINE, formatter.format(record(Level.WARN, "Hello", null)));
    }

    @Test
    void formatsTheParametersOfTheMessage() {
        ExtLogRecord record = record(Level.WARN, "Failed to launch {0}", null);
        record.setParameters(new Object[]{"1.20.1"});

        assertEquals("Failed to launch 1.20.1" + LINE, formatter.format(record));
    }

    @Test
    void appendsTheExceptionMessageAndListsTheCausesBelowIt() {
        Throwable thrown = new RuntimeException(
            "Failed to process response", new RuntimeException("Api response has status code 400")
        );

        String formatted = formatter.format(record(Level.ERROR, "Failed to execute headlessmc java install", thrown));

        assertEquals(
            "Failed to execute headlessmc java install: Failed to process response" + LINE
                + "    - Api response has status code 400" + LINE,
            formatted
        );
    }

    @Test
    void listsEveryCauseInTheChainAtTheSameIndent() {
        Throwable thrown = new RuntimeException("one", new RuntimeException("two", new RuntimeException("three")));

        assertEquals(
            "failed: one" + LINE
                + "    - two" + LINE
                + "    - three" + LINE,
            formatter.format(record(Level.ERROR, "failed", thrown))
        );
    }

    @Test
    void printsTheFullStacktraceWhenStacktracesAreEnabled() {
        config = config.withStacktraces(true);
        Throwable thrown = new RuntimeException("Failed to download client.jar");

        String formatted = formatter.format(record(Level.ERROR, "Failed to launch 1.20.1", thrown));

        assertTrue(
            formatted.startsWith("Failed to launch 1.20.1: java.lang.RuntimeException: Failed to download client.jar"),
            formatted
        );
        assertTrue(formatted.contains(getClass().getName()), formatted);
    }

    @Test
    void fallsBackToTheClassNameForExceptionsWithoutAMessage() {
        String formatted = formatter.format(record(Level.ERROR, "failed", new IllegalStateException()));

        assertEquals("failed: IllegalStateException" + LINE, formatted);
    }

    @Test
    void stopsAtSelfReferencingCauses() {
        RuntimeException cause = new RuntimeException("cause");
        RuntimeException thrown = new RuntimeException("thrown", cause);
        cause.initCause(thrown);

        assertEquals(
            "failed: thrown" + LINE + "    - cause" + LINE,
            formatter.format(record(Level.ERROR, "failed", thrown))
        );
    }

    @Test
    void listsOnlyTheFirstFiveCausesAndCountsTheRest() {
        Throwable thrown = new RuntimeException("one");
        for (int i = 2; i <= 9; i++) {
            thrown = new RuntimeException(Integer.toString(i), thrown);
        }

        assertEquals(
            "failed: 9" + LINE
                + "    - 8" + LINE
                + "    - 7" + LINE
                + "    - 6" + LINE
                + "    - 5" + LINE
                + "    - 4" + LINE
                + "    - ... and 3 more" + LINE,
            formatter.format(record(Level.ERROR, "failed", thrown))
        );
    }

    @Test
    void listsExactlyFiveCausesWithoutCountingTheRest() {
        Throwable thrown = new RuntimeException("one");
        for (int i = 2; i <= 6; i++) {
            thrown = new RuntimeException(Integer.toString(i), thrown);
        }

        assertEquals(
            "failed: 6" + LINE
                + "    - 5" + LINE
                + "    - 4" + LINE
                + "    - 3" + LINE
                + "    - 2" + LINE
                + "    - one" + LINE,
            formatter.format(record(Level.ERROR, "failed", thrown))
        );
    }

    @Test
    void skipsCausesWhichRepeatAMessageThatWasAlreadyListed() {
        Throwable thrown = new RuntimeException(
            "one", new RuntimeException("one", new RuntimeException("two", new RuntimeException("one")))
        );

        assertEquals(
            "failed: one" + LINE + "    - two" + LINE,
            formatter.format(record(Level.ERROR, "failed", thrown))
        );
    }

    @Test
    void doesNotCountSkippedDuplicatesTowardsTheOmittedCauses() {
        // eight causes, of which the two repeating the message of "a" are skipped, leaving six to list
        Throwable thrown = new RuntimeException("g");
        for (String message : new String[]{"f", "e", "d", "c", "b", "a", "a", "a"}) {
            thrown = new RuntimeException(message, thrown);
        }

        assertEquals(
            "failed: a" + LINE
                + "    - b" + LINE
                + "    - c" + LINE
                + "    - d" + LINE
                + "    - e" + LINE
                + "    - f" + LINE
                + "    - ... and 1 more" + LINE,
            formatter.format(record(Level.ERROR, "failed", thrown))
        );
    }

    @Test
    void printsErrorsInRed() {
        assertEquals(
            ConsoleFormatter.RED + "boom" + ConsoleFormatter.RESET + LINE,
            colored.format(record(Level.ERROR, "boom", null))
        );
        assertEquals(
            ConsoleFormatter.RED + "boom" + ConsoleFormatter.RESET + LINE,
            colored.format(record(Level.FATAL, "boom", null))
        );
    }

    @Test
    void printsWarningsInYellow() {
        assertEquals(
            ConsoleFormatter.YELLOW + "careful" + ConsoleFormatter.RESET + LINE,
            colored.format(record(Level.WARN, "careful", null))
        );
    }

    @Test
    void leavesEverythingBelowWarningUncolored() {
        assertEquals("hello" + LINE, colored.format(record(Level.INFO, "hello", null)));
        assertEquals("hello" + LINE, colored.format(record(Level.DEBUG, "hello", null)));
    }

    @Test
    void colorsTheCausesAlongWithTheMessage() {
        Throwable thrown = new RuntimeException("one", new RuntimeException("two"));

        assertEquals(
            ConsoleFormatter.RED + "failed: one" + LINE
                + "    - two" + ConsoleFormatter.RESET + LINE,
            colored.format(record(Level.ERROR, "failed", thrown))
        );
    }

}
