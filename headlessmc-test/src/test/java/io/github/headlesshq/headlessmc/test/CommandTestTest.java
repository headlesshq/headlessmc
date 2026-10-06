package io.github.headlesshq.headlessmc.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.github.headlesshq.headlessmc.test.Actions.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
public class CommandTestTest {
    private final List<String> output = new CopyOnWriteArrayList<>();
    private final List<AutoCloseable> closeables = new ArrayList<>();

    @AfterEach
    public void tearDown() throws Exception {
        for (AutoCloseable closeable : closeables) {
            closeable.close();
        }

        // a timeout interrupts the thread calling run(), make sure it does not leak into other tests
        //noinspection ResultOfMethodCallIgnored
        Thread.interrupted();
    }

    @Test
    public void successfulServerTest() {
        TestCase testCase = new TestCase(
            List.of(action(TestCase.Action.Type.ENDS_WITH, "For help, type \"help\""), send("stop")),
            "Server Test", null, null, null);
        FakeProcess process = FakeProcess.withLines(true, "Starting server", "Done (1.0s)! For help, type \"help\"", "Stopping");

        CommandTest commandTest = commandTest(testCase, process);
        commandTest.run();

        assertTrue(commandTest.wasSuccessful());
        assertNull(commandTest.getMessage());
        assertEquals("stop" + System.lineSeparator(), process.getWrittenInput());
        assertEquals(List.of("Starting server", "Done (1.0s)! For help, type \"help\""), output,
                     "output should be forwarded until the test ends");
    }

    @Test
    public void failingTest() {
        FakeProcess process = FakeProcess.withLines(true, "Starting", "Error!", "Done");
        CommandTest commandTest = commandTest(testCase(contains("Error"), failure()), process);
        commandTest.run();

        assertFalse(commandTest.wasSuccessful());
        assertEquals(List.of("Starting", "Error!"), output);
    }

    @Test
    public void outputEndingBeforeTheTestFinishedIsNotSuccessful() {
        FakeProcess process = FakeProcess.withLines(true, "Starting", "Crashed");
        CommandTest commandTest = commandTest(testCase(contains("Done")), process);
        commandTest.run();

        assertFalse(commandTest.wasSuccessful());
        assertNull(commandTest.getMessage());
        assertEquals(List.of("Starting", "Crashed"), output);
    }

    @Test
    public void stepTimeoutStopsTheTest() throws IOException {
        TestCase testCase = testCase(withTimeout(contains("never"), 1L));
        CommandTest commandTest = commandTest(testCase, new FakeProcess(silentStream(), true));

        long start = System.nanoTime();
        commandTest.run();
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000L;

        assertFalse(commandTest.wasSuccessful());
        assertEquals("Timed out!", commandTest.getMessage());
        assertTrue(elapsedMillis >= 900L, "returned before the timeout: " + elapsedMillis + "ms");
    }

    @Test
    public void totalTimeoutStopsWaiting() throws IOException {
        TestCase testCase = new TestCase(List.of(contains("never")), "test", 60L, false, 1L);
        CommandTest commandTest = commandTest(testCase, new FakeProcess(silentStream(), true));
        commandTest.run();

        assertFalse(commandTest.wasSuccessful());
        assertFalse(Thread.currentThread().isInterrupted());
    }

    @Test
    public void exceptionWhileReadingFailsTheTest() {
        InputStream throwing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }
        };

        CommandTest commandTest = commandTest(testCase(contains("Done")), new FakeProcess(throwing, true));
        commandTest.run();

        assertFalse(commandTest.wasSuccessful());
        assertEquals("boom", commandTest.getMessage());
    }

    @Test
    public void runningTwiceThrows() {
        CommandTest commandTest = commandTest(testCase(contains("Done"), success()), FakeProcess.withLines(true, "Done"));
        commandTest.run();
        assertTrue(commandTest.wasSuccessful());
        assertThrows(IllegalStateException.class, commandTest::run);
    }

    @Test
    public void awaitExitOrKillDoesNotKillExitedProcess() throws InterruptedException {
        FakeProcess process = FakeProcess.withLines(true);
        commandTest(testCase(), process).awaitExitOrKill();
        assertFalse(process.destroyed);
    }

    @Test
    public void awaitExitOrKillKillsRunningProcess() throws InterruptedException {
        FakeProcess process = FakeProcess.withLines(false);
        commandTest(testCase(), process).awaitExitOrKill();
        assertTrue(process.destroyed);
    }

    @Test
    public void closeBeforeRunDoesNotThrow() {
        assertDoesNotThrow(() -> new CommandTest(output::add, testCase(), FakeProcess.withLines(true), false).close());
    }

    private CommandTest commandTest(TestCase testCase, Process process) {
        CommandTest commandTest = new CommandTest(output::add, testCase, process, false);
        closeables.add(commandTest);
        return commandTest;
    }

    /**
     * @return a stream that blocks on read until the test finishes.
     */
    private InputStream silentStream() throws IOException {
        PipedOutputStream out = new PipedOutputStream();
        PipedInputStream in = new PipedInputStream(out);
        closeables.add(out);
        return in;
    }

}
