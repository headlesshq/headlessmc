package io.github.headlesshq.headlessmc.lwjgl.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ByteBufferInputStreamTest {
    @Test
    public void testByteBufferInputStream() throws IOException {
        byte[] bytes = new byte[]{1};
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        try (ByteBufferInputStream is = new ByteBufferInputStream(byteBuffer)) {
            assertEquals(1, byteBuffer.remaining());
            assertEquals(1, is.available());

            assertEquals(1, is.read());
            assertEquals(0, is.available());
            assertEquals(0, byteBuffer.remaining());

            assertEquals(-1, is.read());
            assertEquals(0, is.available());
            assertEquals(0, byteBuffer.remaining());
        }
    }

    @Test
    public void testByteBufferInputStreamMethod2() throws IOException {
        byte[] bytes = new byte[]{1, 2};
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        try (ByteBufferInputStream is = new ByteBufferInputStream(byteBuffer)) {
            byte[] bytes2 = new byte[2];
            assertEquals(2, is.read(bytes2, 0, 2));
            assertEquals(0, is.available());
            assertEquals(-1, is.read(bytes2, 0, 2));
        }
    }

}
