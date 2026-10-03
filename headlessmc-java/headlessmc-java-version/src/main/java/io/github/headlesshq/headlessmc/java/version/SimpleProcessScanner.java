package io.github.headlesshq.headlessmc.java.version;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Qualifier;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple Java version scanner (from legacy HeadlessMc).
 * <br>Attempts to parse the output of {@code java -version}:
 * <pre>
 * {@code
 * openjdk version "25.0.1" 2025-10-21 LTS
 * OpenJDK Runtime Environment Temurin-25.0.1+8 (build 25.0.1+8-LTS)
 * OpenJDK 64-Bit Server VM Temurin-25.0.1+8 (build 25.0.1+8-LTS, mixed mode, sharing)
 * }
 * </pre>
 */
@Priority(100)
@ApplicationScoped
@SimpleProcessScanner.Simple
public class SimpleProcessScanner extends AbstractProcessScanner  implements JavaScanner {
    private static final Pattern PATTERN = Pattern.compile("version \"(\\d+)[.-]?(\\d*)");

    @Override
    public int parseOutput(String output) throws IOException {
        Matcher matcher = PATTERN.matcher(output);
        if (!matcher.find()) {
            throw new IOException("Couldn't parse '" + output + "'");
        }

        if ("1".equals(matcher.group(1))) {
            return Integer.parseInt(matcher.group(2));
        }

        // see Java.JAVA_VERSION_0_9
        if ("0".equals(matcher.group(1))) {
            return -Integer.parseInt(matcher.group(2));
        }

        return Integer.parseInt(matcher.group(1));
    }

    @Override
    public List<String> getCommand(String executable) {
        return List.of(executable, "-version");
    }

    @Qualifier
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Simple {}

}
