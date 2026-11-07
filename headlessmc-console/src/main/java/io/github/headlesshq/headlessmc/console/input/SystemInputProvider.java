package io.github.headlesshq.headlessmc.console.input;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.ConsoleException;
import io.github.headlesshq.headlessmc.console.Password;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class SystemInputProvider implements InputProvider {
    private static @Nullable InputStream readerStream;
    private static @Nullable BufferedReader reader;

    @Override
    public Input get() {
        InputStream in = System.in;
        if (in == null) {
            throw new ConsoleException("STD_IN unavailable");
        }

        String charset = System.getProperty("stdin.encoding", "UTF-8");
        return new Input() {
            @Override
            public String read() {
                try {
                    String line = reader(in, charset).readLine();
                    if (line == null) {
                        throw new ConsoleException.Interrupted("STD_IN closed");
                    }

                    return line;
                } catch (IOException e) {
                    throw new HeadlessMcIOException(e);
                }
            }

            @Override
            public Password readPassword() {
                return Password.of(read());
            }
        };
    }

    @Override
    public int sort() {
        return Console.SORT_STD;
    }

    /**
     * A new {@link Input} is requested for every read,
     * but a {@link BufferedReader} reads ahead, so lines piped into STD_IN would get lost
     * if every read used a new reader. The reader is kept until {@link System#in} changes.
     */
    private static synchronized BufferedReader reader(InputStream in, String charset) throws UnsupportedEncodingException {
        if (reader == null || readerStream != in) {
            reader = new BufferedReader(new InputStreamReader(in, charset));
            readerStream = in;
        }

        return reader;
    }

}
