package io.github.headlesshq.headlessmc.net.hash;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ObservableInputStreamTest {
    private static final byte[] DATA = "observed".getBytes(StandardCharsets.UTF_8);

    private static final class RecordingObserver implements ObservableInputStream.Observer {
        private final ByteArrayOutputStream seen = new ByteArrayOutputStream();
        private int eofs;
        private boolean closed;

        @Override
        public void byteRead(byte b) {
            seen.write(b);
        }

        @Override
        public void bytesRead(byte[] b, int off, int len) {
            seen.write(b, off, len);
        }

        @Override
        public void eof() {
            eofs++;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    @Test
    public void observesSingleByteReads() throws IOException {
        RecordingObserver observer = new RecordingObserver();
        try (ObservableInputStream in = new ObservableInputStream(List.of(observer), new ByteArrayInputStream(DATA))) {
            while (in.read() != -1) {
                // drain
            }
        }

        assertArrayEquals(DATA, observer.seen.toByteArray());
        assertEquals(1, observer.eofs);
        assertTrue(observer.closed);
    }

    @Test
    public void observesBulkReads() throws IOException {
        RecordingObserver observer = new RecordingObserver();
        try (ObservableInputStream in = new ObservableInputStream(List.of(observer), new ByteArrayInputStream(DATA))) {
            byte[] buffer = new byte[4];
            int n;
            while ((n = in.read(buffer, 0, buffer.length)) != -1) {
                assertTrue(n > 0);
            }
        }

        assertArrayEquals(DATA, observer.seen.toByteArray());
        assertEquals(1, observer.eofs);
    }

    @Test
    public void closeSuppressesObserverExceptions() {
        InputStream failing = new FilterInputStream(new ByteArrayInputStream(DATA)) {
            @Override
            public void close() throws IOException {
                throw new IOException("stream close failed");
            }
        };
        ObservableInputStream.Observer failingObserver = new ObservableInputStream.Observer() {
            @Override
            public void byteRead(byte b) {
            }

            @Override
            public void bytesRead(byte[] b, int off, int len) {
            }

            @Override
            public void eof() {
            }

            @Override
            public void close() throws IOException {
                throw new IOException("observer close failed");
            }
        };

        ObservableInputStream in = new ObservableInputStream(List.of(failingObserver), failing);
        IOException e = assertThrows(IOException.class, in::close);
        assertEquals("stream close failed", e.getMessage());
        assertEquals(1, e.getSuppressed().length);
        assertEquals("observer close failed", e.getSuppressed()[0].getMessage());
    }

    @Test
    public void closeThrowsObserverExceptionIfStreamCloses() {
        ObservableInputStream.Observer failingObserver = new ObservableInputStream.Observer() {
            @Override
            public void byteRead(byte b) {
            }

            @Override
            public void bytesRead(byte[] b, int off, int len) {
            }

            @Override
            public void eof() {
            }

            @Override
            public void close() throws IOException {
                throw new IOException("observer close failed");
            }
        };

        ObservableInputStream in = new ObservableInputStream(List.of(failingObserver), new ByteArrayInputStream(DATA));
        IOException e = assertThrows(IOException.class, in::close);
        assertEquals("observer close failed", e.getMessage());
    }

}
