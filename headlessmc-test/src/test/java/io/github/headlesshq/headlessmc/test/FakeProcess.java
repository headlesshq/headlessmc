package io.github.headlesshq.headlessmc.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * A {@link Process} whose output is a fixed stream and whose input is recorded.
 */
public class FakeProcess extends Process {
    private final ByteArrayOutputStream stdin = new ByteArrayOutputStream();
    private final InputStream stdout;
    private final boolean exits;

    public volatile boolean destroyed;

    public FakeProcess(InputStream stdout, boolean exits) {
        this.stdout = stdout;
        this.exits = exits;
    }

    public static FakeProcess withLines(boolean exits, String... lines) {
        String output = lines.length == 0 ? "" : String.join("\n", lines) + "\n";
        return new FakeProcess(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)), exits);
    }

    public String getWrittenInput() {
        return stdin.toString(StandardCharsets.UTF_8);
    }

    @Override
    public OutputStream getOutputStream() {
        return stdin;
    }

    @Override
    public InputStream getInputStream() {
        return stdout;
    }

    @Override
    public InputStream getErrorStream() {
        return InputStream.nullInputStream();
    }

    @Override
    public int waitFor() {
        return 0;
    }

    @Override
    public boolean waitFor(long timeout, TimeUnit unit) {
        return exits || destroyed;
    }

    @Override
    public int exitValue() {
        if (!exits && !destroyed) {
            throw new IllegalThreadStateException("Process has not exited");
        }

        return 0;
    }

    @Override
    public void destroy() {
        destroyed = true;
    }

    @Override
    public Process destroyForcibly() {
        destroyed = true;
        return this;
    }

}
