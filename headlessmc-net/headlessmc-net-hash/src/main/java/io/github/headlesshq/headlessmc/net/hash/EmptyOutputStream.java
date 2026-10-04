package io.github.headlesshq.headlessmc.net.hash;

import java.io.OutputStream;

final class EmptyOutputStream extends OutputStream {
    static final EmptyOutputStream INSTANCE = new EmptyOutputStream();

    @Override
    public void write(int b) {

    }

    @Override
    public void write(byte[] b) {

    }

    @Override
    public void write(byte[] b, int off, int len) {

    }

    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }

}
