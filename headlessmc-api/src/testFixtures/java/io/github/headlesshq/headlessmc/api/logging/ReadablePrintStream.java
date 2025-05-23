package io.github.headlesshq.headlessmc.api.logging;

import lombok.Getter;

import java.io.PrintStream;

@Getter
public class ReadablePrintStream extends PrintStream {
    private final ReadableOutputStream readableOutputStream;

    public ReadablePrintStream() {
        this(new ReadableOutputStream());
    }

    private ReadablePrintStream(ReadableOutputStream readableOutputStream) {
        super(readableOutputStream, true);
        this.readableOutputStream = readableOutputStream;
    }

    @Override
    public void close() {
        // no need to close
    }

}
