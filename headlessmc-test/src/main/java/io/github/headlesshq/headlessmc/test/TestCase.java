package io.github.headlesshq.headlessmc.test;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

// Legacy HeadlessMc code, rewrite candidate
@Data
@Slf4j
@RegisterForReflection
public class TestCase implements ReflectionRegistered {
    private final List<Action> steps;
    private final String name;
    private final @Nullable Long timeout;
    private final @Nullable Boolean implicitWaitForEnd;
    private final @Nullable Long totalTimeout;

    public Boolean getImplicitWaitForEnd() {
        return implicitWaitForEnd == null || implicitWaitForEnd;
    }

    public Long getTimeout() {
        return timeout == null ? 120L : timeout;
    }

    @RegisterForReflection
    public enum Result implements ReflectionRegistered {
        MATCH,
        PASS,
        END_SUCCESS,
        END_FAIL
    }

    @Data
    @RegisterForReflection
    public static class Action implements ReflectionRegistered {
        private final Type type;
        private final @Nullable Boolean ignoreCase;
        private final @Nullable Long timeout;
        private final @Nullable String message;
        private final @Nullable List<Action> and;
        private final @Nullable List<Action> or;
        private final @Nullable List<Action> then;

        public Long getTimeout(TestCase testCase) {
            return timeout == null ? testCase.getTimeout() : timeout;
        }

        public boolean isIgnoreCase() {
            return ignoreCase != null && ignoreCase;
        }

        @Getter
        @RequiredArgsConstructor
        @RegisterForReflection
        public enum Type implements ReflectionRegistered {
            /**
             * Sends the message as a command to the process.
             */
            SEND((process, action, _) -> {
                requireNonNull(action.getMessage(), "Message of action was null!");
                log.info("Sending command: {}", action.getMessage());
                process.getOutputStream().write(
                        (action.getMessage() + System.lineSeparator())
                                .getBytes(StandardCharsets.UTF_8));
                process.getOutputStream().flush();
                return Result.MATCH;
            }, false),
            /**
             * Checks for log messages that end with the message.
             */
            ENDS_WITH((_, action, message) -> {
                requireNonNull(action.getMessage(), "Message of action was null!");
                requireNonNull(message, "Cannot execute CONTAINS in end step!");

                boolean match = action.isIgnoreCase()
                        ? message.toLowerCase(Locale.ENGLISH).endsWith(action.getMessage().toLowerCase(Locale.ENGLISH))
                        : message.endsWith(action.getMessage());

                return match ? Result.MATCH : Result.PASS;
            }, true),
            /**
             * Checks for log messages that match the given regex
             */
            REGEX((_, action, message) -> {
                requireNonNull(action.getMessage(), "Message of action was null!");
                requireNonNull(message, "Cannot execute CONTAINS in end step!");
                Pattern pattern = action.isIgnoreCase()
                        ? Pattern.compile(action.getMessage(), Pattern.CASE_INSENSITIVE)
                        : Pattern.compile(action.getMessage());

                return pattern.matcher(message).matches()
                        ? Result.MATCH
                        : Result.PASS;
            }, true),
            /**
             * Checks for messages that contain the message.
             */
            CONTAINS((_, action, message) -> {
                requireNonNull(action.getMessage(), "Message of action was null!");
                requireNonNull(message, "Cannot execute CONTAINS in end step!");

                boolean match = action.isIgnoreCase()
                        ? message.toLowerCase(Locale.ENGLISH).contains(action.getMessage().toLowerCase(Locale.ENGLISH))
                        : message.contains(action.getMessage());

                return match ? Result.MATCH : Result.PASS;
            }, true),
            /**
             * Waits for the given timeout.
             */
            WAIT((_, action, _) -> {
                requireNonNull(action.getTimeout(), "Timeout of action was null!");

                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(action.getTimeout()));
                } catch (InterruptedException e) {
                    throw new TestException(e);
                }

                return Result.MATCH;
            }, false),
            /**
             * Match step.
             */
            MATCH((_, _, _) -> Result.MATCH, true),
            /**
             * Pass step.
             */
            PASS((_, _, _) -> Result.PASS, true),
            /**
             * Waits for the end of the process.
             */
            WAIT_FOR_END((process, action, message) -> {
                requireNonNull(action.getTimeout(), "Timeout of action was null!");
                try {
                    if (process.waitFor(action.getTimeout(), TimeUnit.SECONDS)) {
                        return Result.END_SUCCESS;
                    } else {
                        return Result.END_FAIL;
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }, false),
            /**
             * Ends the test successfully.
             */
            SUCCESS((_, _, _) -> Result.END_SUCCESS, false),
            /**
             * Ends the test signaling failure.
             */
            FAIL((_, _, _) -> Result.END_FAIL, false);

            private final ActionFunction function;
            private final boolean condition;
        }

        @FunctionalInterface
        @RegisterForReflection
        public interface ActionFunction {
            Result evaluate(Process process, Action action, @Nullable String message) throws IOException;
        }
    }

}
