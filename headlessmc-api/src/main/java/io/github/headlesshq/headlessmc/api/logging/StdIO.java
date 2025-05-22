package io.github.headlesshq.headlessmc.api.logging;

import io.github.headlesshq.headlessmc.api.Application;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.function.Supplier;

/**
 * This class contains the standard input and output streams used by an {@link Application}
 * to communicate command output with the user.
 * <p>
 * The main reason we use {@link FileDescriptor#out} and {@link FileDescriptor#err}
 * over {@link System#out} and {@link System#err} is,
 * that mc replaces the standard out and error streams
 * with ones that log to mcs Logger.
 * An Example, when mc redirects System.out:
 * <pre>
 * System.out.println("Hello World!");
 * </pre>
 * Results in:
 * <pre>
 * [12:33:21] [Server thread/INFO] [STDOUT]: Hello World!
 * </pre>
 * However, when our Application communicates with the user
 * we do not want to add this information.
 */
@Getter
@Setter
public class StdIO {
    /**
     * The standard OutputStream.
     * @see FileDescriptor#out
     * @see System#out
     */
    private volatile Supplier<PrintWriterPrintStream> out = new Lazy<>(() -> new PrintWriterPrintStream(new FileOutputStream(FileDescriptor.out), true), null);
    /**
     * The standard error OutputStream.
     * @see FileDescriptor#err
     * @see System#err
     */
    private volatile Supplier<PrintWriterPrintStream> err = new Lazy<>(() -> new PrintWriterPrintStream(new FileOutputStream(FileDescriptor.err), true), null);
    /**
     * The standard InputStream.
     * @see FileDescriptor#in
     * @see System#in
     */
    private volatile Supplier<InputStream> in = new Lazy<>(() -> new FileInputStream(FileDescriptor.in), null);
    /**
     * The System Console.
     * @see System#console()
     */
    private volatile Supplier<@Nullable Console> console = System::console;

}
