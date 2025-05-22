package io.github.headlesshq.headlessmc.api.logging;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * A {@link PrintStream} that also manages a {@link PrintWriter} that can write into it.
 */
@Getter
public class PrintWriterPrintStream extends PrintStream {
    /**
     * A PrintWriter that is constructed to write on this PrintStream.
     * @see PrintWriter#PrintWriter(OutputStream, boolean)
     */
    private final PrintWriter writer;

    /**
     * Constructs a new PrintWriterPrintStream for the given {@link OutputStream}.
     *
     * @param out the OutputStream this PrintStream writes to.
     * @param autoFlush if you wish this PrintStream and its PrintWriter to autoFlush.
     * @see PrintStream#PrintStream(OutputStream, boolean)
     * @see PrintWriter#PrintWriter(OutputStream, boolean)
     */
    public PrintWriterPrintStream(@NotNull OutputStream out, boolean autoFlush) {
        super(out, autoFlush);
        this.writer = new PrintWriter(this, autoFlush);
    }

}
