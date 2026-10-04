package io.github.headlesshq.headlessmc.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

// Legacy HeadlessMc code, rewrite candidate
@Slf4j
public class TestCaseRunner {
    private final Deque<Frame> stack = new ArrayDeque<>();
    private final TimeoutHandler timeoutHandler;
    private final TestCase test;

    public TestCaseRunner(TestCase test, TimeoutHandler timeoutHandler) {
        this.timeoutHandler = timeoutHandler;
        this.test = test;
        stack.addFirst(new Frame(test.getSteps()));
        if (test.getImplicitWaitForEnd()) {
            List<TestCase.Action> waitForEnd = new ArrayList<>(1);
            waitForEnd.add(new TestCase.Action(
                    TestCase.Action.Type.WAIT_FOR_END,
                    false,
                    test.getTimeout(),
                    null,
                    null,
                    null,
                    null
            ));

            stack.addLast(new Frame(waitForEnd));
        }
    }

    public void updateTimeout() {
        Frame nextFrame = getFrame();
        if (nextFrame != null) {
            TestCase.Action nextAction = nextFrame.actions.get(nextFrame.index);
            timeoutHandler.setTimeout(nextAction.getTimeout(test));
        } else {
            timeoutHandler.removeTimeout();
        }
    }

    public TestCase.Result runStep(Process process, @Nullable String message) throws IOException {
        Frame frame = getFrame();
        if (frame == null) {
            timeoutHandler.removeTimeout();
            return TestCase.Result.END_SUCCESS;
        }

        TestCase.Action action = frame.actions.get(frame.index);
        log.debug("Evaluating {} with action {}", message, action);
        EvaluationResult evaluationResult = evaluate(process, action, message);
        if (evaluationResult.result == TestCase.Result.MATCH) {
            frame.index++;
            if (frame.index >= frame.actions.size() && action.getThen() != null) {
                evaluationResult.setThen(action);
            }

            // TODO: nullability? Potential Bug????
            if (evaluationResult.thenAction != null) {
                stack.addFirst(new Frame(evaluationResult.thenAction.getThen()));
            }

            updateTimeout();
            // check the next action,
            // otherwise if we want to send a command right after we would have to wait for next message...
            TestCase.Result nextResult = checkNextAction(process);
            if (nextResult != TestCase.Result.PASS && nextResult != TestCase.Result.MATCH) {
                return nextResult;
            }
        }

        return evaluationResult.result;
    }

    private TestCase.Result checkNextAction(Process process) throws IOException {
        Frame nextFrame = getFrame();
        if (nextFrame != null) {
            TestCase.Action action = nextFrame.actions.get(nextFrame.index);
            if (!action.getType().isCondition()) {
                return runStep(process, null);
            }
        }

        return TestCase.Result.PASS;
    }

    private @Nullable Frame getFrame() {
        Frame frame;
        for (;;) {
            frame = stack.peek();
            if (frame == null) {
                return null;
            }

            if (frame.index >= frame.actions.size()) {
                stack.removeFirst();
            } else {
                break;
            }
        }

        return frame;
    }

    private EvaluationResult evaluate(Process process, TestCase.Action action, @Nullable String message) throws IOException {
        if (!action.getType().isCondition() && (action.getAnd() != null || action.getOr() != null)) {
            throw new IllegalArgumentException("Action " + action + " is not a condition but has and/or conditions.");
        }

        EvaluationResult result = new EvaluationResult();
        TestCase.Result actionResult = action.getType().getFunction().evaluate(process, action, message);
        result.result = actionResult;
        if (actionResult == TestCase.Result.MATCH) {
            evaluateAnds(process, action, message, result);
        } else if (actionResult == TestCase.Result.PASS) {
            evaluateOrs(process, action, message, result);
        }

        return result;
    }

    private void evaluateAnds(Process process, TestCase.Action action, @Nullable String message, EvaluationResult result) throws IOException {
        if (action.getAnd() == null) {
            result.result = TestCase.Result.MATCH;
            return;
        }

        for (TestCase.Action and : action.getAnd()) {
            if (!and.getType().isCondition()) {
                throw new IllegalArgumentException(String.format("%s and in %s is not a condition", and, action));
            }

            if (and.getThen() != null) {
                throw new IllegalArgumentException(String.format("then block in and condition %s not allowed", and));
            }

            EvaluationResult andResult = evaluate(process, and, message);
            if (andResult.result == TestCase.Result.PASS) {
                result.result = TestCase.Result.PASS;
                return;
            }

            result.setThen(andResult.thenAction);
        }

        result.result = TestCase.Result.MATCH;
    }

    private void evaluateOrs(Process process, TestCase.Action action, @Nullable String message, EvaluationResult result) throws IOException {
        if (action.getOr() == null) {
            result.result = TestCase.Result.PASS;
            return;
        }

        for (TestCase.Action or : action.getOr()) {
            if (!or.getType().isCondition()) {
                throw new IllegalArgumentException(String.format("%s or in %s is not a condition", or, action));
            }

            EvaluationResult orResult = evaluate(process, or, message);
            if (orResult.result == TestCase.Result.MATCH) {
                result.setThen(orResult.thenAction);
                result.setThen(or);
                result.result = TestCase.Result.MATCH;
                return;
            }
        }

        result.result = TestCase.Result.PASS;
    }

    private static class EvaluationResult {
        private TestCase.Result result = TestCase.Result.PASS;
        private @Nullable TestCase.Action thenAction;

        public void setThen(@Nullable TestCase.Action thenAction) {
            if (thenAction != null && thenAction.getThen() != null) {
                if (this.thenAction == null) {
                    this.thenAction = thenAction;
                } else {
                    throw new IllegalArgumentException(
                            "Invalid Test, two then paths: " + this.thenAction + ", " + thenAction);
                }
            }
        }
    }

    @RequiredArgsConstructor
    private static class Frame {
        private final List<TestCase.Action> actions;
        private int index;
    }

}
