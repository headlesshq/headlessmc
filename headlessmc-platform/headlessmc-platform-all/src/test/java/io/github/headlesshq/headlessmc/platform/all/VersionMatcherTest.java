package io.github.headlesshq.headlessmc.platform.all;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.McVersions;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.client.VersionMatcherService;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionParser;
import io.github.headlesshq.headlessmc.version.service.VersionJsonService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Slf4j
@QuarkusTest
public class VersionMatcherTest {
    @Inject
    VersionMatcherService versionMatcherService;
    @Inject
    VersionJsonService versionService;
    @Inject
    VersionParser parser;

    @Test
    public void testAllVersions() throws HeadlessMcException, IOException {
        Optional<McVersions> mcVersionsCheckout = McVersions.find();
        assumeTrue(mcVersionsCheckout.isPresent(), "mc-versions has not been cloned next to HeadlessMc");
        Path mcVersions = mcVersionsCheckout.get().versionsDir();

        try (Stream<Path> directories = Files.list(mcVersions)) {
            long count = directories.parallel().filter(dir -> {
                if (Files.isDirectory(dir)) {
                    Path json = dir.resolve(dir.getFileName() + ".json");
                    if (Files.exists(json)) {
                        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(json))) {
                            Version version = testParse(reader);
                            Set<VersionID> versionIds = versionMatcherService.match(version, versionService);
                            assertFalse(versionIds.isEmpty(), version.getId() + " had no matched version ids.");
                            // exception for 1.20.1, a few builds exist on both forge and neoforge
                            assertTrue(versionIds.size() == 1 || versionIds.stream().allMatch(
                                id -> (id.getPlatform().getName().equalsIgnoreCase("neoforge")
                                    || id.getPlatform().getName().equalsIgnoreCase("forge"))
                                    && id.getVersion().getName().equals("1.20.1")
                            ), version.getId() + " matched " + versionIds);

                            return true;
                        } catch (Throwable t) {
                            throw new AssertionError("Failed to read " + json, t);
                        }
                    }
                }

                return false;
            }).count();
            log.info("Checked {} versions", count);
        }
    }

    private Version testParse(InputStreamReader reader) throws HeadlessMcException {
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

        return version;
    }

}
