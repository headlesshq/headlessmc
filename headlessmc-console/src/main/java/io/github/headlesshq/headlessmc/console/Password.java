package io.github.headlesshq.headlessmc.console;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class Password implements AutoCloseable {
    private final AtomicBoolean closed = new AtomicBoolean();
    private final char[] password;

    public char[] get() {
        if (closed.get()) {
            throw new IllegalStateException("Password used after closing.");
        }

        return password;
    }

    @Override
    public void close() {
        closed.set(true);
        Arrays.fill(password, (char) 0);
    }

    public static Password of(String password) {
        return new Password(password.toCharArray());
    }

}
