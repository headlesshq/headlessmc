package io.github.headlesshq.headlessmc.test;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static io.github.headlesshq.headlessmc.test.Actions.action;
import static io.github.headlesshq.headlessmc.test.TestCase.Action.Type.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestCaseTest {
    private final FakeProcess process = FakeProcess.withLines(true);

    @Test
    public void testCaseDefaults() {
        TestCase testCase = new TestCase(List.of(), "test", null, null, null);
        assertEquals(120L, testCase.getTimeout());
        assertTrue(testCase.getImplicitWaitForEnd());
        assertNull(testCase.getTotalTimeout());
    }

    @Test
    public void testCaseExplicitValues() {
        TestCase testCase = new TestCase(List.of(), "test", 5L, false, 10L);
        assertEquals(5L, testCase.getTimeout());
        assertFalse(testCase.getImplicitWaitForEnd());
        assertEquals(10L, testCase.getTotalTimeout());
    }

    @Test
    public void actionTimeoutFallsBackToTestCaseTimeout() {
        TestCase testCase = new TestCase(List.of(), "test", 42L, null, null);
        assertEquals(42L, action(CONTAINS, "x").getTimeout(testCase));
        assertEquals(3L, Actions.withTimeout(action(CONTAINS, "x"), 3L).getTimeout(testCase));
    }

    @Test
    public void actionIgnoreCaseDefaultsToFalse() {
        assertFalse(action(CONTAINS, "x").isIgnoreCase());
        assertTrue(ignoreCase(CONTAINS, "x").isIgnoreCase());
        assertFalse(new TestCase.Action(CONTAINS, false, null, "x", null, null, null).isIgnoreCase());
    }

    @Test
    public void conditionTypes() {
        assertTrue(ENDS_WITH.isCondition());
        assertTrue(REGEX.isCondition());
        assertTrue(CONTAINS.isCondition());
        assertTrue(MATCH.isCondition());
        assertTrue(PASS.isCondition());
        assertFalse(SEND.isCondition());
        assertFalse(WAIT.isCondition());
        assertFalse(WAIT_FOR_END.isCondition());
        assertFalse(SUCCESS.isCondition());
        assertFalse(FAIL.isCondition());
    }

    @Test
    public void endsWith() throws IOException {
        assertEquals(TestCase.Result.MATCH, eval(action(ENDS_WITH, "help\""), "For help, type \"help\""));
        assertEquals(TestCase.Result.PASS, eval(action(ENDS_WITH, "For help"), "For help, type \"help\""));
        assertEquals(TestCase.Result.PASS, eval(action(ENDS_WITH, "HELP\""), "For help, type \"help\""));
        assertEquals(TestCase.Result.MATCH, eval(ignoreCase(ENDS_WITH, "HELP\""), "For help, type \"help\""));
    }

    @Test
    public void contains() throws IOException {
        assertEquals(TestCase.Result.MATCH, eval(action(CONTAINS, "Done"), "[Server] Done (1.2s)!"));
        assertEquals(TestCase.Result.PASS, eval(action(CONTAINS, "done"), "[Server] Done (1.2s)!"));
        assertEquals(TestCase.Result.MATCH, eval(ignoreCase(CONTAINS, "done"), "[Server] Done (1.2s)!"));
        assertEquals(TestCase.Result.PASS, eval(ignoreCase(CONTAINS, "stopping"), "[Server] Done (1.2s)!"));
    }

    @Test
    public void regexMustMatchTheWholeMessage() throws IOException {
        assertEquals(TestCase.Result.MATCH, eval(action(REGEX, ".*Done \\(\\d+\\.\\d+s\\)!"), "[Server] Done (1.2s)!"));
        assertEquals(TestCase.Result.PASS, eval(action(REGEX, "Done"), "[Server] Done (1.2s)!"));
        assertEquals(TestCase.Result.PASS, eval(action(REGEX, ".*done.*"), "[Server] Done (1.2s)!"));
        assertEquals(TestCase.Result.MATCH, eval(ignoreCase(REGEX, ".*done.*"), "[Server] Done (1.2s)!"));
    }

    @Test
    public void conditionsRequireAMessage() {
        for (TestCase.Action.Type type : List.of(ENDS_WITH, REGEX, CONTAINS)) {
            assertThrows(NullPointerException.class, () -> eval(action(type, "x"), null), type.name());
            assertThrows(NullPointerException.class, () -> eval(action(type, null), "x"), type.name());
        }
    }

    @Test
    public void matchAndPassIgnoreTheMessage() throws IOException {
        assertEquals(TestCase.Result.MATCH, eval(action(MATCH, null), "anything"));
        assertEquals(TestCase.Result.MATCH, eval(action(MATCH, null), null));
        assertEquals(TestCase.Result.PASS, eval(action(PASS, null), "anything"));
        assertEquals(TestCase.Result.PASS, eval(action(PASS, null), null));
    }

    @Test
    public void successAndFailEndTheTest() throws IOException {
        assertEquals(TestCase.Result.END_SUCCESS, eval(action(SUCCESS, null), null));
        assertEquals(TestCase.Result.END_FAIL, eval(action(FAIL, null), null));
    }

    @Test
    public void sendWritesTheMessageAsALine() throws IOException {
        FakeProcess process = FakeProcess.withLines(false);
        assertEquals(TestCase.Result.MATCH, SEND.getFunction().evaluate(process, action(SEND, "stop"), null));
        assertEquals(TestCase.Result.MATCH, SEND.getFunction().evaluate(process, action(SEND, "help"), null));
        assertEquals("stop" + System.lineSeparator() + "help" + System.lineSeparator(), process.getWrittenInput());
    }

    @Test
    public void sendRequiresAMessage() {
        assertThrows(NullPointerException.class, () -> eval(action(SEND, null), null));
    }

    @Test
    public void waitSleepsAndMatches() throws IOException {
        assertEquals(TestCase.Result.MATCH, eval(Actions.withTimeout(action(WAIT, null), 0L), null));
        assertThrows(NullPointerException.class, () -> eval(action(WAIT, null), null));
    }

    @Test
    public void waitThrowsTestExceptionWhenInterrupted() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(TestException.class, () -> eval(Actions.withTimeout(action(WAIT, null), 1L), null));
        } finally {
            //noinspection ResultOfMethodCallIgnored
            Thread.interrupted();
        }
    }

    @Test
    public void waitForEndDependsOnWhetherTheProcessExits() throws IOException {
        TestCase.Action waitForEnd = Actions.withTimeout(action(WAIT_FOR_END, null), 1L);
        assertEquals(TestCase.Result.END_SUCCESS,
                     WAIT_FOR_END.getFunction().evaluate(FakeProcess.withLines(true), waitForEnd, null));
        assertEquals(TestCase.Result.END_FAIL,
                     WAIT_FOR_END.getFunction().evaluate(FakeProcess.withLines(false), waitForEnd, null));
        assertThrows(NullPointerException.class, () -> eval(action(WAIT_FOR_END, null), null));
    }

    private TestCase.Result eval(TestCase.Action action, @Nullable String message) throws IOException {
        return action.getType().getFunction().evaluate(process, action, message);
    }

    private static TestCase.Action ignoreCase(TestCase.Action.Type type, String message) {
        return new TestCase.Action(type, true, null, message, null, null, null);
    }

}
