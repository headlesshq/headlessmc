package io.github.headlesshq.headlessmc.plugins;

import io.github.headlesshq.headlessmc.plugins.version.Version;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The libraries HeadlessMc itself ships, read from {@value #RESOURCE},
 * which the build of headlessmc-application generates from its runtime classpath.
 * Plugins cannot replace these libraries, the HeadlessMc version is always used.
 */
@Slf4j
@ApplicationScoped
public class CoreLibraries {
    /**
     * The resource containing a {@code group:artifact:version} line for every library.
     */
    public static final String RESOURCE = "META-INF/headlessmc/core-libraries.txt";

    private final Map<String, Version> libraries;

    public CoreLibraries() {
        this(load());
    }

    /**
     * @param libraries {@code group:artifact} to version.
     */
    public CoreLibraries(Map<String, Version> libraries) {
        this.libraries = Collections.unmodifiableMap(libraries);
    }

    /**
     * @param module {@code group:artifact}.
     * @return the version HeadlessMc ships, or empty if HeadlessMc does not contain the library.
     */
    public Optional<Version> get(String module) {
        return Optional.ofNullable(libraries.get(module));
    }

    /**
     * @return {@code group:artifact} to version.
     */
    public Map<String, Version> getAll() {
        return libraries;
    }

    private static Map<String, Version> load() {
        Map<String, Version> result = new LinkedHashMap<>();
        try (InputStream inputStream = CoreLibraries.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (inputStream == null) {
                log.warn("{} not found, conflicts with libraries of HeadlessMc cannot be detected", RESOURCE);
                return result;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split(":");
                if (parts.length >= 3) {
                    try {
                        result.put(parts[0] + ":" + parts[1], Version.parse(parts[2]));
                    } catch (IllegalArgumentException e) {
                        log.debug("Ignoring core library with unparseable version {}", line);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + RESOURCE, e);
        }

        return result;
    }

}
