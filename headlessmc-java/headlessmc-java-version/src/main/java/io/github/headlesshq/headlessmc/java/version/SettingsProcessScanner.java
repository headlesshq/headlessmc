package io.github.headlesshq.headlessmc.java.version;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import lombok.RequiredArgsConstructor;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Similarly to {@link SimpleProcessScanner} {@code java -version} is scanned,
 * but with {@code -XshowSettings:properties}, which shows the default
 * SystemProperties. The property {@code java.specification.version}
 * is then parsed.
 * <pre>
 * {@code
 * Property settings:
 *     file.encoding = UTF-8
 *     ...
 *     java.specification.version = 25
 *     ...
 *
 * openjdk version "25.0.1" 2025-10-21 LTS
 * ...
 * }
 * </pre>
 */
@Priority(200) // makes this be injected before SimpleProcessScanner, as its more accurate
@ApplicationScoped
@SettingsProcessScanner.Settings
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class SettingsProcessScanner extends AbstractProcessScanner implements JavaScanner {
    private static final Pattern PATTERN = Pattern.compile("java.specification.version = (.*)");

    private final SpecificationVersionParser specificationVersionParser;

    @Override
    public int parseOutput(String output) {
        Matcher matcher = PATTERN.matcher(output);
        if (matcher.find()) {
            String match = matcher.group(1);
            return specificationVersionParser.parseSpecificationVersion(match);
        }

        throw new HeadlessMcIOException("Failed to find java.specification.version in output: " + output);
    }

    @Override
    public List<String> getCommand(String executable) {
        return List.of(executable, "-XshowSettings:properties", "-version");
    }

    @Qualifier
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Settings {}

}
