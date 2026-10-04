package io.github.headlesshq.headlessmc.net.hash;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import lombok.RequiredArgsConstructor;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RequiredArgsConstructor
public class ObservableInputStream extends InputStream {
    private final List<Observer> observers;
    private final InputStream in;

    public interface Observer extends Closeable {
        void byteRead(byte b) throws HeadlessMcException, IOException;

        void bytesRead(byte[] b, int off, int len) throws HeadlessMcException, IOException;

        void eof() throws HeadlessMcException, IOException;

        @Override
        default void close() throws HeadlessMcException, IOException {

        }
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        if (b == -1) {
            for (Observer observer : observers) {
                observer.eof();
            }
        } else {
            for (Observer observer : observers) {
                observer.byteRead((byte) b);
            }
        }

        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = in.read(b, off, len);
        if (n == -1) {
            for (Observer observer : observers) {
                observer.eof();
            }
        } else {
            for (Observer observer : observers) {
                observer.bytesRead(b, off, n);
            }
        }

        return n;
    }

    @Override
    public void close() throws IOException {
        IOException exception = null;
        try {
            in.close();
        } catch (IOException e) {
            exception = e;
        }

        for (Observer observer : observers) {
            try {
                observer.close();
            } catch (IOException e) {
                if (exception == null) {
                    exception = e;
                } else {
                    exception.addSuppressed(e);
                }
            }
        }

        if (exception != null) {
            throw exception;
        }
    }

    // eventually support markSupported()? will require special care around tracking the
    // reset position such that the observer does not see the same bytes multiple times.
    // That is why we ruled out commons-io.

}
