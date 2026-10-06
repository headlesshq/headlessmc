package io.github.headlesshq.headlessmc.test;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static io.github.headlesshq.headlessmc.test.Actions.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestCaseRunnerTest {
    private final RecordingTimeoutHandler timeoutHandler = new RecordingTimeoutHandler();
    private final FakeProcess process = FakeProcess.withLines(true);

    @Test
    public void conditionsAreMatchedInOrder() throws IOException {
        TestCaseRunner runner = runner(testCase(contains("first"), contains("second")));

        assertEquals(TestCase.Result.PASS, runner.runStep(process, "second"));
        assertEquals(TestCase.Result.PASS, runner.runStep(process, "unrelated"));
        assertEquals(TestCase.Result.MATCH, runner.runStep(process, "first"));
        assertEquals(TestCase.Result.PASS, runner.runStep(process, "first"));
        assertEquals(TestCase.Result.MATCH, runner.runStep(process, "second"));
        assertEquals(TestCase.Result.END_SUCCESS, runner.runStep(process, "anything"));
    }

    @Test
    public void emptyTestEndsSuccessfully() throws IOException {
        TestCaseRunner runner = runner(testCase());
        assertEquals(TestCase.Result.END_SUCCESS, runner.runStep(process, "anything"));
    }

    @Test
    public void actionsFollowingAMatchRunImmediately() throws IOException {
        TestCaseRunner runner = runner(testCase(contains("ready"), send("stop"), send("again"), contains("stopped")));

        assertEquals(TestCase.Result.MATCH, runner.runStep(process, "ready"));
        assertEquals("stop" + System.lineSeparator() + "again" + System.lineSeparator(), process.getWrittenInput());
    }

    @Test
    public void leadingActionsRunOnTheFirstMessage() throws IOException {
        TestCaseRunner runner = runner(testCase(send("hello"), contains("ready")));

        assertEquals(TestCase.Result.MATCH, runner.runStep(process, "not ready"));
        assertEquals("hello" + System.lineSeparator(), process.getWrittenInput());
        assertEquals(TestCase.Result.MATCH, runner.runStep(process, "ready"));
    }

    @Test
    public void successAfterMatchEndsTheTest() throws IOException {
        TestCaseRunner runner = runner(testCase(contains("ready"), success(), contains("never reached")));
        assertEquals(TestCase.Result.END_SUCCESS, runner.runStep(process, "ready"));
    }

    @Test
    public void failAfterMatchEndsTheTest() throws IOException {
        TestCaseRunner runner = runner(testCase(contains("error"), failure()));
        assertEquals(TestCase.Result.PASS, runner.runStep(process, "all good"));
        assertEquals(TestCase.Result.END_FAIL, runner.runStep(process, "an error occurred"));
    }

    @Test
    public void implicitWaitForEndSucceedsWhenTheProcessExits() throws IOException {
        TestCase testCase = new TestCase(List.of(contains("ready")), "test", 7L, null, null);
        TestCaseRunner runner = runner(testCase);

        assertEquals(TestCase.Result.END_SUCCESS, runner.runStep(FakeProcess.withLines(true), "ready"));
        assertEquals(List.of(7L), timeoutHandler.timeouts, "WAIT_FOR_END should use the test timeout");
    }

    @Test
    public void implicitWaitForEndFailsWhenTheProcessDoesNotExit() throws IOException {
        TestCase testCase = new TestCase(List.of(contains("ready")), "test", null, true, null);
        TestCaseRunner runner = runner(testCase);

        assertEquals(TestCase.Result.END_FAIL, runner.runStep(FakeProcess.withLines(false), "ready"));
    }

    @Test
    public void explicitEndSkipsTheImplicitWaitForEnd() throws IOException {
        TestCase testCase = new TestCase(List.of(contains("ready"), success()), "test", null, true, null);
        TestCaseRunner runner = runner(testCase);

        assertEquals(TestCase.Result.END_SUCCESS, runner.runStep(FakeProcess.withLines(false), "ready"));
    }

    @Test
    public void andRequiresAllConditionsToMatch() throws IOException {
        TestCaseRunner runner = runner(testCase(withAnd(contains("a"), contains("b"), contains("c")), success()));

        assertEquals(TestCase.Result.PASS, runner.runStep(process, "a b"));
        assertEquals(TestCase.Result.PASS, runner.runStep(process, "b c"));
        assertEquals(TestCase.Result.END_SUCCESS, runner.runStep(process, "a b c"));
    }

    @Test
    public void orRequiresAnyConditionToMatch() throws IOException {
        TestCaseRunner runner = runner(testCase(withOr(contains("a"), contains("b"), contains("c")), contains("next")));

        assertEquals(TestCase.Result.PASS, runner.runStep(process, "d"));
        assertEquals(TestCase.Result.MATCH, runner.runStep(process, "c"));
        assertEquals(TestCase.Result.PASS, runner.runStep(process, "a"));
        assertEquals(TestCase.Result.MATCH, runner.runStep(process, "next"));
    }

    @Test
    public void thenOfTheLastActionRuns() throws IOException {
        TestCaseRunner runner = runner(testCase(withThen(contains("ready"), send("stop"), success())));

        assertEquals(TestCase.Result.END_SUCCESS, runner.runStep(process, "ready"));
        assertEquals("stop" + System.lineSeparator(), process.getWrittenInput());
    }

    @Test
    public void thenBlockRunsBeforeTheRemainingSteps() throws IOException {
        TestCase.Action branch = withOr(contains("ok"), withThen(contains("error"), send("recover"), contains("recovered")));
        TestCaseRunner runner = runner(testCase(branch, send("continue")));

        assertEquals(TestCase.Result.MATCH, runner.runStep(process, "error"));
        assertEquals("recover" + System.lineSeparator(), process.getWrittenInput());

        assertEquals(TestCase.Result.MATCH, runner.runStep(process, "recovered"));
        assertEquals("recover" + System.lineSeparator() + "continue" + System.lineSeparator(),
                     process.getWrittenInput());
    }

    @Test
    public void orBranchWithoutThenContinuesNormally() throws IOException {
        TestCase.Action branch = withOr(contains("ok"), withThen(contains("error"), failure()));
        TestCaseRunner runner = runner(testCase(branch, success()));

        assertEquals(TestCase.Result.END_SUCCESS, runner.runStep(process, "ok"));
    }

    @Test
    public void orBranchThenCanFailTheTest() throws IOException {
        TestCase.Action branch = withOr(contains("ok"), withThen(contains("error"), failure()));
        TestCaseRunner runner = runner(testCase(branch, success()));

        assertEquals(TestCase.Result.END_FAIL, runner.runStep(process, "error"));
    }

    @Test
    @Disabled("Bug: TestCaseRunner only pushes an action's own then block if it is the last action of its frame")
    public void thenOfAnActionThatIsNotTheLastStepRuns() throws IOException {
        TestCaseRunner runner = runner(testCase(withThen(contains("ready"), send("stop")), contains("stopped")));

        assertEquals(TestCase.Result.MATCH, runner.runStep(process, "ready"));
        assertEquals("stop" + System.lineSeparator(), process.getWrittenInput());
    }

    @Test
    public void twoThenPathsAreRejected() {
        TestCase.Action inner = withThen(withOr(contains("x"), withThen(contains("a"), success())), failure());
        TestCaseRunner runner = runner(testCase(withOr(contains("y"), inner)));

        assertThrows(IllegalArgumentException.class, () -> runner.runStep(process, "a"));
    }

    @Test
    public void thenInsideAndIsRejected() {
        TestCaseRunner runner = runner(testCase(withAnd(contains("a"), withThen(contains("b"), success()))));
        assertThrows(IllegalArgumentException.class, () -> runner.runStep(process, "a b"));
    }

    @Test
    public void nonConditionInsideAndIsRejected() {
        TestCaseRunner runner = runner(testCase(withAnd(contains("a"), send("b"))));
        assertThrows(IllegalArgumentException.class, () -> runner.runStep(process, "a"));
    }

    @Test
    public void nonConditionInsideOrIsRejected() {
        TestCaseRunner runner = runner(testCase(withOr(contains("a"), send("b"))));
        assertThrows(IllegalArgumentException.class, () -> runner.runStep(process, "b"));
    }

    @Test
    public void nonConditionWithAndOrIsRejected() {
        assertThrows(IllegalArgumentException.class,
                     () -> runner(testCase(withAnd(send("a"), contains("b")))).runStep(process, "b"));
        assertThrows(IllegalArgumentException.class,
                     () -> runner(testCase(withOr(send("a"), contains("b")))).runStep(process, "b"));
    }

    @Test
    public void timeoutFollowsTheCurrentAction() throws IOException {
        TestCase testCase = new TestCase(
            List.of(withTimeout(contains("first"), 5L), contains("second")), "test", 30L, false, null);
        TestCaseRunner runner = runner(testCase);
        assertFalse(timeoutHandler.hasTimeout(), "constructor should not set a timeout");

        runner.updateTimeout();
        assertEquals(5L, timeoutHandler.current);

        runner.runStep(process, "unrelated");
        assertEquals(5L, timeoutHandler.current, "a PASS should not reset the timeout");
        assertEquals(1, timeoutHandler.timeouts.size());

        runner.runStep(process, "first");
        assertEquals(30L, timeoutHandler.current);

        runner.runStep(process, "second");
        assertFalse(timeoutHandler.hasTimeout(), "timeout should be removed once all steps ran");
    }

    @Test
    public void updateTimeoutOnEmptyTestRemovesTheTimeout() {
        timeoutHandler.setTimeout(10L);
        runner(testCase()).updateTimeout();
        assertFalse(timeoutHandler.hasTimeout());
    }

    private TestCaseRunner runner(TestCase testCase) {
        return new TestCaseRunner(testCase, timeoutHandler);
    }

}
