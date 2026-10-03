package io.github.headlesshq.headlessmc.version.service;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.NotFoundException;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.ProcessedVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VersionJsonServiceImplTest {
    /** Parses the trivial {@code <id>[:<inheritsFrom>]} format written by {@link #write}. */
    private static final VersionParser PARSER = reader -> {
        String content = read(reader);
        if (content.startsWith("!")) {
            throw new HeadlessMcException("Unparseable version " + content);
        }

        String[] parts = content.split(":");
        FakeVersion version = new FakeVersion(parts[0]);
        return parts.length > 1 ? version.withInheritsFrom(parts[1]) : version;
    };

    private static String read(Reader reader) {
        try (BufferedReader buffered = new BufferedReader(reader)) {
            return String.join("", buffered.readAllLines()).trim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @TempDir
    Path root;

    private Path versionsDir;
    private VersionJsonServiceImpl service;

    @BeforeEach
    void setup() {
        versionsDir = root.resolve("versions");
        service = new VersionJsonServiceImpl(PARSER, () -> versionsDir);
    }

    private void write(String id, String inheritsFrom) {
        try {
            Path dir = Files.createDirectories(versionsDir.resolve(id));
            Files.writeString(dir.resolve(id + ".json"), inheritsFrom == null ? id : id + ":" + inheritsFrom);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void missingVersionsDirHasNoVersions() {
        assertEquals(Set.of(), service.getInstalledFileNames());
        assertEquals(List.of(), service.getInstalledVersions());
    }

    @Test
    void listsOnlyDirectoriesWithMatchingJson() throws IOException {
        write("1.21.1", null);
        Files.createDirectories(versionsDir.resolve("empty"));
        Files.writeString(versionsDir.resolve("stray.json"), "stray");

        assertEquals(Set.of("1.21.1"), service.getInstalledFileNames());
    }

    @Test
    void parsesInstalledVersion() {
        write("1.21.1", null);

        assertEquals("1.21.1", service.getVersion("1.21.1").getId());
        assertTrue(service.tryGetVersion("1.21.1").isPresent());
        assertEquals(1, service.getInstalledVersions().size());
        assertSame(PARSER, service.getParser());
    }

    @Test
    void missingVersionThrows() {
        assertEquals(Optional.empty(), service.tryGetVersion("nope"));
        assertThrows(NotFoundException.class, () -> service.getVersion("nope"));
    }

    @Test
    void unreadableVersionIsSkippedWhenListing() throws IOException {
        write("1.21.1", null);
        Path broken = Files.createDirectories(versionsDir.resolve("broken"));
        Files.writeString(broken.resolve("broken.json"), "!broken");

        assertEquals(2, service.getInstalledFileNames().size());
        assertEquals(1, service.getInstalledVersions().size());
    }

    @Test
    void resolvesVersionHierarchy() {
        write("1.21.1", null);
        write("fabric-1.21.1", "1.21.1");

        ProcessedVersion resolved = service.resolve(service.getVersion("fabric-1.21.1"));

        assertEquals(List.of("1.21.1", "fabric-1.21.1"), resolved.hierarchy().stream().map(Version::getId).toList());
        assertEquals("fabric-1.21.1", resolved.getId());
    }

    @Test
    void resolveInstallsMissingParent() {
        write("fabric-1.21.1", "1.21.1");

        ProcessedVersion resolved = service.resolve(
            service.getVersion("fabric-1.21.1"), id -> write(id, null)
        );

        assertEquals(2, resolved.hierarchy().size());
    }

    @Test
    void resolveThrowsIfParentCannotBeInstalled() {
        write("fabric-1.21.1", "1.21.1");

        assertThrows(HeadlessMcException.class, () -> service.resolve(service.getVersion("fabric-1.21.1")));
        assertThrows(NotFoundException.class,
            () -> service.resolve(service.getVersion("fabric-1.21.1"), id -> { }));
    }

    @Test
    void processFailsOnMissingParent() {
        write("fabric-1.21.1", "1.21.1");

        HeadlessMcException e = assertThrows(
            HeadlessMcException.class, () -> service.process(service.getVersion("fabric-1.21.1"))
        );
        assertTrue(e.getMessage().contains("Failed to resolve version 1.21.1"));
    }

    @Test
    void circularHierarchyThrows() {
        write("a", "b");
        write("b", "a");

        assertThrows(NotFoundException.class, () -> service.resolve(service.getVersion("a")));
    }

    @Test
    void observeChangesReportsNewVersions() {
        write("1.21.1", null);

        List<Version> added = service.observeChanges(() -> write("fabric-1.21.1", "1.21.1"));

        assertEquals(List.of("fabric-1.21.1"), added.stream().map(Version::getId).toList());
    }

    @Test
    void observeChangesWithoutChangesIsEmpty() {
        write("1.21.1", null);
        assertEquals(List.of(), service.observeChanges(() -> { }));
    }

}
