package io.github.headlesshq.headlessmc.console.input;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.ConsoleException;
import io.github.headlesshq.headlessmc.console.Password;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SystemInputProvider implements InputProvider {
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
                    String line = new BufferedReader(new InputStreamReader(in, charset)).readLine();
                    if (line == null) {
                        throw new HeadlessMcIOException("STD_IN closed");
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

}
