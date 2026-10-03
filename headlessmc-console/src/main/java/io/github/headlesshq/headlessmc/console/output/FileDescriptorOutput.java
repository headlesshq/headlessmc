package io.github.headlesshq.headlessmc.console.output;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class FileDescriptorOutput implements OutputProvider {
    @Override
    public Output get() {
        return new Output() {
            @Override
            public void write(String message) {
                write(message, System.lineSeparator());
            }

            @Override
            public void write(String message, String nextLine) {
                try {
                    OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(FileDescriptor.out));
                    writer.write(message + nextLine);
                    writer.flush();
                } catch (IOException e) {
                    throw new HeadlessMcIOException(e);
                }
            }
        };
    }

    @Override
    public int sort() {
        // for hmc-specifics we actually want to write to FileDescriptor.out
        // because mc redirects System.in to the logger.
        return Console.SORT_FILE_DESCRIPTOR;
    }

}
