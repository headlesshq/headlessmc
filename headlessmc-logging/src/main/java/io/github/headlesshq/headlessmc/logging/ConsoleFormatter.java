package io.github.headlesshq.headlessmc.logging;

import io.github.headlesshq.headlessmc.config.Holder;
import lombok.RequiredArgsConstructor;
import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Formats records for the console.
 * Unlike the log file, which keeps timestamps, levels, categories and
 * full stacktraces, the console only shows what a user of the launcher
 * needs, coloured by level:
 * <pre>
 * Failed to execute headlessmc java install: Failed to process response
 *     - Api response has status code 400
 * </pre>
 * The message of an exception is appended to the message of the record and
 * the messages of its causes are listed below it.
 * If {@link LogConfig#stacktraces()} is enabled the full stacktrace is
 * printed instead, which is re-read for every record, so that the setting
 * can be changed at runtime.
 */
@RequiredArgsConstructor
public class ConsoleFormatter extends ExtFormatter {
    static final String INDENT = "    ";
    static final int MAX_CAUSES = 5;
    static final String RESET = "\u001b[0m";
    static final String RED = "\u001b[31m";
    static final String YELLOW = "\u001b[33m";

    private final Holder<LogConfig> config;
    private final boolean colored;

    @Override
    public String format(ExtLogRecord record) {
        StringBuilder result = new StringBuilder(Objects.requireNonNullElse(formatMessage(record), ""));
        Throwable thrown = record.getThrown();
        if (thrown != null) {
            if (config.get().stacktraces()) {
                result.append(": ").append(stacktrace(thrown));
            } else {
                describe(thrown, result);
            }
        }

        String color = color(record.getLevel());
        if (color != null) {
            result.insert(0, color).append(RESET);
        }

        return result.append(System.lineSeparator()).toString();
    }

    @Override
    public boolean isCallerCalculationRequired() {
        return false;
    }

    @VisibleForTesting
    static void describe(Throwable thrown, StringBuilder result) {
        if (!result.isEmpty()) {
            result.append(": ");
        }

        String message = message(thrown);
        result.append(message);
        List<String> causes = causes(thrown, message);
        for (String cause : causes.subList(0, Math.min(causes.size(), MAX_CAUSES))) {
            result.append(System.lineSeparator()).append(INDENT).append("- ").append(cause);
        }

        if (causes.size() > MAX_CAUSES) {
            result.append(System.lineSeparator()).append(INDENT)
                .append("- ... and ").append(causes.size() - MAX_CAUSES).append(" more");
        }
    }

    private static List<String> causes(Throwable thrown, String message) {
        List<String> result = new ArrayList<>();
        // wrapping exceptions often just repeat the message of their cause
        Set<String> messages = new HashSet<>();
        messages.add(message);
        // guards against exceptions which (transitively) cause themselves
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        seen.add(thrown);
        for (Throwable cause = thrown.getCause(); cause != null && seen.add(cause); cause = cause.getCause()) {
            String causeMessage = message(cause);
            if (messages.add(causeMessage)) {
                result.add(causeMessage);
            }
        }

        return result;
    }

    private @Nullable String color(java.util.logging.Level level) {
        if (!colored) {
            return null;
        }

        if (level.intValue() >= Level.ERROR.intValue()) {
            return RED;
        }

        return level.intValue() >= Level.WARN.intValue() ? YELLOW : null;
    }

    private static String stacktrace(Throwable thrown) {
        StringWriter result = new StringWriter();
        try (PrintWriter writer = new PrintWriter(result)) {
            thrown.printStackTrace(writer);
        }

        return result.toString().stripTrailing();
    }

    private static String message(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }

        return message;
    }

}
