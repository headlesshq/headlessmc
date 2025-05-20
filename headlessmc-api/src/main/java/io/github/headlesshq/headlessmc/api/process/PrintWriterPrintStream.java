package io.github.headlesshq.headlessmc.api.process;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

@Getter
public class PrintWriterPrintStream extends PrintStream {
    private final PrintWriter writer;

    public PrintWriterPrintStream(@NotNull OutputStream out, boolean autoFlush) {
        super(out, autoFlush);
        this.writer = new PrintWriter(this, autoFlush);
    }

}
