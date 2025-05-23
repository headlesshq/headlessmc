package io.github.headlesshq.headlessmc.api.logging;

import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Deque;

public class WritableInputStream extends InputStream {
    private final Deque<Integer> writes = new ArrayDeque<>();

    @Getter
    private final OutputStream outputStream = new OutputStream() {
        @Override
        public void write(int b) {
            writes.addLast(b);
        }
    };

    @Getter
    private final PrintStream printStream = new PrintStream(outputStream, true) {
        @Override
        public void close() {
            // do not close
        }
    };

    @Override
    public int read() throws IOException {
        if (writes.isEmpty()) {
            return -1;
        }

        return writes.removeFirst();
    }

}
