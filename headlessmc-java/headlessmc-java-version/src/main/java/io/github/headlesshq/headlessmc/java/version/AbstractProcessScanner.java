package io.github.headlesshq.headlessmc.java.version;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

/**
 * A {@link JavaScanner} that takes the STD_ERR output
 * of a process like {@code java -version} and parses
 * it to get the Java version.
 */
public abstract class AbstractProcessScanner implements JavaScanner {
    public abstract int parseOutput(String output) throws IOException;

    public abstract List<String> getCommand(String executable);

    @Override
    public int scan(Path executable) throws HeadlessMcException {
        try {
            Process process = new ProcessBuilder(getCommand(executable.toAbsolutePath().toString()))
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }

                String output = builder.toString();
                return parseOutput(output);
            }
        } catch (IOException e) {
            throw new HeadlessMcIOException(e);
        }
    }

}
