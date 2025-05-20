package io.github.headlesshq.headlessmc.api.process;

import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;

public class ReadableOutputStream extends OutputStream {
    private final Deque<Integer> writes = new ArrayDeque<>();

    @Getter
    public InputStream inputStream = new InputStream() {
        @Override
        public int read() {
            if (writes.isEmpty()) {
                return -1;
            }

            return writes.removeFirst();
        }

        @Override
        public int available() {
            return writes.isEmpty() ? -1 : writes.size();
        }
    };

    @Override
    public void write(int b) throws IOException {
        writes.addLast(b);
    }

}
