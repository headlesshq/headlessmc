package io.github.headlesshq.headlessmc.launcher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A {@link Process} with fixed output and exit code.
 * If it does not exit by itself, {@link #waitFor()} blocks until it gets destroyed.
 */
class TestProcess extends Process {
    private final CountDownLatch destroyedLatch = new CountDownLatch(1);
    private final ByteArrayOutputStream stdin = new ByteArrayOutputStream();
    private final InputStream stdout;
    private final boolean exits;
    private final int exitCode;

    TestProcess(int exitCode, boolean exits, String... lines) {
        String output = lines.length == 0 ? "" : String.join("\n", lines) + "\n";
        this.stdout = new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8));
        this.exitCode = exitCode;
        this.exits = exits;
    }

    static TestProcess exiting(int exitCode, String... lines) {
        return new TestProcess(exitCode, true, lines);
    }

    static TestProcess running(String... lines) {
        return new TestProcess(0, false, lines);
    }

    boolean isDestroyed() {
        return destroyedLatch.getCount() == 0;
    }

    String getWrittenInput() {
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
    public int waitFor() throws InterruptedException {
        if (!exits && !destroyedLatch.await(30, TimeUnit.SECONDS)) {
            throw new IllegalStateException("TestProcess was never destroyed");
        }

        return exitCode;
    }

    @Override
    public boolean waitFor(long timeout, TimeUnit unit) {
        return exits || isDestroyed();
    }

    @Override
    public int exitValue() {
        if (!exits && !isDestroyed()) {
            throw new IllegalThreadStateException("Process has not exited");
        }

        return exitCode;
    }

    @Override
    public void destroy() {
        destroyedLatch.countDown();
    }

    @Override
    public Process destroyForcibly() {
        destroy();
        return this;
    }

}
