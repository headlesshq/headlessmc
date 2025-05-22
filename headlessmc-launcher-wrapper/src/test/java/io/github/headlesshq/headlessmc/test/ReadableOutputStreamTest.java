package io.github.headlesshq.headlessmc.test;

import io.github.headlesshq.headlessmc.api.process.ReadableOutputStream;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("resource")
public class ReadableOutputStreamTest {

    @Test
    public void testReadableOutputStream() throws IOException {
        ReadableOutputStream readableOutputStream = new ReadableOutputStream();

        assertEquals(-1, readableOutputStream.getInputStream().read());

        readableOutputStream.write('A');
        readableOutputStream.write('B');
        readableOutputStream.write('C');

        assertEquals('A', readableOutputStream.getInputStream().read());
        assertEquals('B', readableOutputStream.getInputStream().read());
        assertEquals('C', readableOutputStream.getInputStream().read());

        assertEquals(-1, readableOutputStream.getInputStream().read());
    }

    @Test
    public void testWriteAndReadMixed() throws IOException {
        ReadableOutputStream readableOutputStream = new ReadableOutputStream();

        readableOutputStream.write('1');
        readableOutputStream.write('2');

        assertEquals('1', readableOutputStream.getInputStream().read());

        readableOutputStream.write('3');

        assertEquals('2', readableOutputStream.getInputStream().read());
        assertEquals('3', readableOutputStream.getInputStream().read());

        assertEquals(-1, readableOutputStream.getInputStream().read());
    }

    @Test
    public void testMultipleReadCalls() throws IOException {
        ReadableOutputStream readableOutputStream = new ReadableOutputStream();

        readableOutputStream.write('X');

        assertEquals('X', readableOutputStream.getInputStream().read());

        assertEquals(-1, readableOutputStream.getInputStream().read());
        assertEquals(-1, readableOutputStream.getInputStream().read());
        assertEquals(-1, readableOutputStream.getInputStream().read());
    }

}
