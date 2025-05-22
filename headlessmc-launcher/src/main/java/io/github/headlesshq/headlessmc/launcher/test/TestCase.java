package io.github.headlesshq.headlessmc.launcher.test;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.CustomLog;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import io.github.headlesshq.headlessmc.api.traits.HasName;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

@Data
@CustomLog
public class TestCase implements HasName {
    @SerializedName("steps")
    private final List<Action> steps;
    @SerializedName("name")
    private final String name;
    @SerializedName("timeout")
    private final @Nullable Long timeout;
    @SerializedName("implicitWaitForEnd")
    private final @Nullable Boolean implicitWaitForEnd;
    @SerializedName("totalTimeout")
    private final @Nullable Long totalTimeout;

    public Boolean getImplicitWaitForEnd() {
        return implicitWaitForEnd == null || implicitWaitForEnd;
    }

    public Long getTimeout() {
        return timeout == null ? 120L : timeout;
    }

    public enum Result {
        MATCH,
        PASS,
        END_SUCCESS,
        END_FAIL
    }

    @Data
    public static class Action {
        @SerializedName("type")
        private final Type type;
        @SerializedName("ignoreCase")
        private final boolean ignoreCase;
        @SerializedName("timeout")
        private final @Nullable Long timeout;
        @SerializedName("message")
        private final @Nullable String message;
        @SerializedName("and")
        private final @Nullable List<Action> and;
        @SerializedName("or")
        private final @Nullable List<Action> or;
        @SerializedName("then")
        private final @Nullable List<Action> then;

        public Long getTimeout(TestCase testCase) {
            return timeout == null ? testCase.getTimeout() : timeout;
        }

        @Getter
        @RequiredArgsConstructor
        public enum Type {
            /**
             * Sends the message as a command to the process.
             */
            SEND((process, action, message) -> {
                requireNonNull(action.getMessage(), "Message of action was null!");
                log.info("Sending command: " + action.getMessage());
                process.getOutputStream().write(
                        (action.getMessage() + System.lineSeparator())
                                .getBytes(StandardCharsets.UTF_8));
                process.getOutputStream().flush();
                return Result.MATCH;
            }, false),
            /**
             * Checks for log messages that end with the message.
             */
            ENDS_WITH((process, action, message) -> {
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
            REGEX((process, action, message) -> {
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
            CONTAINS((process, action, message) -> {
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
            WAIT((process, action, message) -> {
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
            MATCH((process, action, message) -> Result.MATCH, true),
            /**
             * Pass step.
             */
            PASS((process, action, message) -> Result.PASS, true),
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
            SUCCESS((process, action, message) -> Result.END_SUCCESS, false),
            /**
             * Ends the test signaling failure.
             */
            FAIL((process, action, message) -> Result.END_FAIL, false);

            private final ActionFunction function;
            private final boolean condition;
        }

        @FunctionalInterface
        public interface ActionFunction {
            Result evaluate(Process process, Action action, @Nullable String message) throws IOException;
        }
    }

    public static TestCase load(InputStream stream) throws IOException {
        try (InputStreamReader isr = new InputStreamReader(stream)) {
            Gson gson = new Gson();
            return gson.fromJson(isr, TestCase.class);
        }
    }

}
