package io.github.headlesshq.headlessmc.test;

import io.github.headlesshq.headlessmc.api.process.ReadablePrintStream;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("resource")
public class ReadablePrintStreamTest {
    @Test
    public void testReadablePrintStreamBasicFunctionality() throws IOException {
        ReadablePrintStream readablePrintStream = new ReadablePrintStream();
        assertEquals(-1, readablePrintStream.getReadableOutputStream().getInputStream().read());
        readablePrintStream.print('A');
        readablePrintStream.print('B');
        readablePrintStream.print('C');
        assertEquals('A', readablePrintStream.getReadableOutputStream().getInputStream().read());
        assertEquals('B', readablePrintStream.getReadableOutputStream().getInputStream().read());
        assertEquals('C', readablePrintStream.getReadableOutputStream().getInputStream().read());
        assertEquals(-1, readablePrintStream.getReadableOutputStream().getInputStream().read());
    }

    @Test
    public void testReadablePrintStreamWriteAndReadMixed() throws IOException {
        ReadablePrintStream readablePrintStream = new ReadablePrintStream();
        readablePrintStream.print('1');
        readablePrintStream.print('2');
        assertEquals('1', readablePrintStream.getReadableOutputStream().getInputStream().read());
        readablePrintStream.print('3');
        assertEquals('2', readablePrintStream.getReadableOutputStream().getInputStream().read());
        assertEquals('3', readablePrintStream.getReadableOutputStream().getInputStream().read());
        assertEquals(-1, readablePrintStream.getReadableOutputStream().getInputStream().read());
    }

    @Test
    public void testReadablePrintStreamAfterClose() throws IOException {
        ReadablePrintStream readablePrintStream = new ReadablePrintStream();
        readablePrintStream.print('X');
        readablePrintStream.print('Y');
        readablePrintStream.close();
        assertEquals('X', readablePrintStream.getReadableOutputStream().getInputStream().read());
        assertEquals('Y', readablePrintStream.getReadableOutputStream().getInputStream().read());
        assertEquals(-1, readablePrintStream.getReadableOutputStream().getInputStream().read());
    }

    @Test
    public void testMultipleReadCalls() throws IOException {
        ReadablePrintStream readablePrintStream = new ReadablePrintStream();
        readablePrintStream.print('Z');
        assertEquals('Z', readablePrintStream.getReadableOutputStream().getInputStream().read());
        assertEquals(-1, readablePrintStream.getReadableOutputStream().getInputStream().read());
        assertEquals(-1, readablePrintStream.getReadableOutputStream().getInputStream().read());
        assertEquals(-1, readablePrintStream.getReadableOutputStream().getInputStream().read());
    }

}
