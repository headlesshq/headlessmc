package io.github.headlesshq.headlessmc.version.jackson;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.version.Version;
import org.junit.jupiter.api.Test;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JacksonVersionParserTest {
    private final JacksonVersionParser parser = new JacksonVersionParser();

    @Test
    public void testJacksonVersionParser() throws HeadlessMcException, IOException {
        List<String> resources = List.of(
            "versions/1.0.json",
            "versions/1.5.2-Forge7.8.0.684.json",
            "versions/1.21.1.json",
            "versions/1.21.1-forge-52.1.6.json",
            "versions/fabric-loader-0.18.1-1.7.10.json",
            "versions/fabric-loader-0.18.1-1.21.10.json",
            "versions/neoforge-21.10.63.json",
            "versions/26.3-snapshot-3.json"
        );

        for (String resource : resources) {
            try (InputStreamReader reader = new InputStreamReader(
                Objects.requireNonNull(JacksonVersionParserTest.class.getClassLoader().getResourceAsStream(resource))
            )) {
                try {
                    testParse(reader);
                } catch (HeadlessMcIOException e) {
                    throw new HeadlessMcIOException("Failed to parse " + resource, e);
                }
            }
        }
    }

    private void testParse(InputStreamReader reader) throws HeadlessMcException {
        Version version = parser.parse(reader);
        assertNotNull(version.getId());
        assertNotNull(version.getType());
        assertNotNull(version.getLibraries());
        for (Version.Library library : version.getLibraries()) {
            Map<String, String> natives = library.getNatives();
            if (natives != null) {
                for (String key : natives.keySet()) {
                    if (!Version.KNOWN_OS.contains(key)) {
                        throw new AssertionError("Unknown OS: " + key + " in library " + library.getName());
                    }
                }
            }
        }

        // this is important to know, legacy forge added completely overridden version.jsons
        if (version.getInheritsFrom() == null && !version.getId().toLowerCase(Locale.ENGLISH).contains("forge")) {
            assertNotNull(Objects.requireNonNull(version.getDownloads()).get(Version.DOWNLOAD_CLIENT));
        }
    }

    // a full test over all mc-versions moved to headlessmc-platform-all/VersionMatcherTest
    // clone mc-versions relative to this project, and it will parse all existing mc-version.json files
    // and check if we can match them to a VersionID.

}
