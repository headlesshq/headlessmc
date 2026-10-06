package io.github.headlesshq.headlessmc.test;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Shorthands for building {@link TestCase.Action}s and {@link TestCase}s in tests.
 */
public final class Actions {
    private Actions() {
        throw new AssertionError();
    }

    public static TestCase.Action action(TestCase.Action.Type type, @Nullable String message) {
        return new TestCase.Action(type, null, null, message, null, null, null);
    }

    public static TestCase.Action contains(String message) {
        return action(TestCase.Action.Type.CONTAINS, message);
    }

    public static TestCase.Action send(String message) {
        return action(TestCase.Action.Type.SEND, message);
    }

    public static TestCase.Action success() {
        return action(TestCase.Action.Type.SUCCESS, null);
    }

    public static TestCase.Action failure() {
        return action(TestCase.Action.Type.FAIL, null);
    }

    public static TestCase.Action withTimeout(TestCase.Action action, long timeout) {
        return new TestCase.Action(action.getType(), action.getIgnoreCase(), timeout, action.getMessage(),
                                   action.getAnd(), action.getOr(), action.getThen());
    }

    public static TestCase.Action withAnd(TestCase.Action action, TestCase.Action... and) {
        return new TestCase.Action(action.getType(), action.getIgnoreCase(), action.getTimeout(), action.getMessage(),
                                   List.of(and), action.getOr(), action.getThen());
    }

    public static TestCase.Action withOr(TestCase.Action action, TestCase.Action... or) {
        return new TestCase.Action(action.getType(), action.getIgnoreCase(), action.getTimeout(), action.getMessage(),
                                   action.getAnd(), List.of(or), action.getThen());
    }

    public static TestCase.Action withThen(TestCase.Action action, TestCase.Action... then) {
        return new TestCase.Action(action.getType(), action.getIgnoreCase(), action.getTimeout(), action.getMessage(),
                                   action.getAnd(), action.getOr(), List.of(then));
    }

    public static TestCase testCase(TestCase.Action... steps) {
        return new TestCase(List.of(steps), "test", null, false, null);
    }

}
