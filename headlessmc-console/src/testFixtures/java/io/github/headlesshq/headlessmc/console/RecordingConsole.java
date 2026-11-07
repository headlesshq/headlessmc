package io.github.headlesshq.headlessmc.console;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A {@link Console} for tests that records everything written to it
 * and replays queued input lines.
 */
public class RecordingConsole implements Console {
    /**
     * Compared by identity, a queued input that makes {@link #read()} behave like the user pressed Ctrl+C.
     */
    @SuppressWarnings("StringOperationCanBeSimplified")
    private static final String INTERRUPT = new String("<interrupt>");

    private final List<String> lines = new ArrayList<>();
    private final Deque<String> input = new ArrayDeque<>();
    private final Deque<String> passwords = new ArrayDeque<>();

    public List<String> lines() {
        return lines;
    }

    public String output() {
        return String.join("\n", lines);
    }

    public RecordingConsole addInput(String line) {
        input.add(line);
        return this;
    }

    /**
     * Queues an input that makes {@link #read()} throw a {@link ConsoleException.Interrupted},
     * like the user pressed Ctrl+C.
     */
    public RecordingConsole addInterrupt() {
        input.add(INTERRUPT);
        return this;
    }

    public RecordingConsole addPassword(String password) {
        passwords.add(password);
        return this;
    }

    @Override
    public void write(String message) {
        lines.add(message);
    }

    @Override
    public void write(String message, String nextLine) {
        lines.add(message);
    }

    @Override
    public String read() {
        String line = input.poll();
        if (line == INTERRUPT) {
            throw new ConsoleException.Interrupted();
        }

        return line == null ? "" : line;
    }

    @Override
    public Password readPassword() {
        return Password.of(passwords.isEmpty() ? "" : passwords.poll());
    }

}
