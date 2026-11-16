package io.github.headlesshq.headlessmc.version.service;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.NotFoundException;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
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
    /**
     * Parses the trivial {@code <id>[:<inheritsFrom>]} format written by {@link #write}.
     * Like a real parser, it connects a version to the file it has been parsed from.
     */
    private static final VersionParser PARSER = new VersionParser() {
        @Override
        public Version parse(Reader reader) {
            String content = read(reader);
            if (content.startsWith("!")) {
                throw new HeadlessMcException("Unparseable version " + content);
            }

            String[] parts = content.split(":");
            FakeVersion version = new FakeVersion(parts[0]);
            return parts.length > 1 ? version.withInheritsFrom(parts[1]) : version;
        }

        @Override
        public Version parse(Path jsonFile) {
            return ((FakeVersion) VersionParser.super.parse(jsonFile)).withFile(jsonFile);
        }
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
    private McFiles mcFiles;
    private McFiles otherMcFiles;
    private VersionJsonServiceImpl service;

    @BeforeEach
    void setup() {
        mcFiles = McFiles.of(root.resolve("mc"));
        otherMcFiles = McFiles.of(root.resolve("other-mc"));
        versionsDir = mcFiles.getVersionsDir();
        service = new VersionJsonServiceImpl(PARSER, mcFiles);
    }

    /** A service that knows both directories, {@link #mcFiles} is the primary one. */
    private VersionJsonServiceImpl multiService() {
        return new VersionJsonServiceImpl(PARSER, McFilesService.of(mcFiles, otherMcFiles));
    }

    private void write(String id, String inheritsFrom) {
        write(mcFiles, id, inheritsFrom);
    }

    private static Path write(McFiles mcFiles, String id, String inheritsFrom) {
        try {
            Path dir = Files.createDirectories(mcFiles.getVersionsDir().resolve(id));
            Path file = dir.resolve(id + ".json");
            Files.writeString(file, inheritsFrom == null ? id : id + ":" + inheritsFrom);
            return file.toAbsolutePath().normalize();
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
            service.getVersion("fabric-1.21.1"), (dir, id) -> write(id, null)
        );

        assertEquals(2, resolved.hierarchy().size());
    }

    @Test
    void resolveThrowsIfParentCannotBeInstalled() {
        write("fabric-1.21.1", "1.21.1");

        assertThrows(HeadlessMcException.class, () -> service.resolve(service.getVersion("fabric-1.21.1")));
        assertThrows(NotFoundException.class,
            () -> service.resolve(service.getVersion("fabric-1.21.1"), (dir, id) -> { }));
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

        List<Version> added = service.observeChanges(mcFiles, () -> write("fabric-1.21.1", "1.21.1"));

        assertEquals(List.of("fabric-1.21.1"), added.stream().map(Version::getId).toList());
    }

    @Test
    void observeChangesWithoutChangesIsEmpty() {
        write("1.21.1", null);
        assertEquals(List.of(), service.observeChanges(mcFiles, () -> { }));
    }

    @Test
    void versionsKnowTheFileTheyAreInstalledAs() {
        Path file = write(mcFiles, "1.21.1", null);

        assertEquals(Optional.of(file), service.getVersion("1.21.1").asFile());
        assertEquals(Optional.of(file), service.getInstalledVersions().getFirst().asFile());
        assertEquals(mcFiles, service.getMcFiles(service.getVersion("1.21.1")));
    }

    @Test
    void versionWithoutFileBelongsToThePrimaryDirectory() {
        assertEquals(mcFiles, multiService().getMcFiles(new FakeVersion("in-memory")));
    }

    @Test
    void listsVersionsOfAllDirectoriesPrimaryFirst() {
        Path other = write(otherMcFiles, "1.20.4", null);
        Path primary = write(mcFiles, "1.21.1", null);
        VersionJsonServiceImpl service = multiService();

        assertEquals(List.of(primary, other),
                     service.getInstalledVersions().stream().map(v -> v.asFile().orElseThrow()).toList());
        assertEquals(Set.of("1.21.1", "1.20.4"), service.getInstalledFileNames());
        assertEquals(List.of("1.20.4"), service.getInstalledVersions(otherMcFiles).stream().map(Version::getId).toList());
        assertEquals(Set.of("1.21.1"), service.getInstalledFileNames(mcFiles));
    }

    @Test
    void sameVersionInTwoDirectoriesIsListedTwice() {
        Path primary = write(mcFiles, "1.21.1", null);
        Path other = write(otherMcFiles, "1.21.1", null);
        VersionJsonServiceImpl service = multiService();

        List<Version> versions = service.getInstalledVersions();
        assertEquals(List.of("1.21.1", "1.21.1"), versions.stream().map(Version::getId).toList());
        assertEquals(List.of(primary, other), versions.stream().map(v -> v.asFile().orElseThrow()).toList());
        // names are a set
        assertEquals(Set.of("1.21.1"), service.getInstalledFileNames());
    }

    @Test
    void theFirstDirectoryWinsForAName() {
        Path primary = write(mcFiles, "1.21.1", null);
        Path other = write(otherMcFiles, "1.21.1", null);
        write(otherMcFiles, "only-other", null);
        VersionJsonServiceImpl service = multiService();

        assertEquals(Optional.of(primary), service.getVersion("1.21.1").asFile());
        assertEquals(Optional.of(other), service.tryGetVersion(otherMcFiles, "1.21.1").orElseThrow().asFile());
        assertEquals("only-other", service.getVersion("only-other").getId());
        assertEquals(Optional.empty(), service.tryGetVersion(mcFiles, "only-other"));
    }

    @Test
    void directoriesThatDoNotExistAreSkipped() {
        write(mcFiles, "1.21.1", null);
        VersionJsonServiceImpl service = multiService();

        assertFalse(otherMcFiles.isAvailable());
        assertEquals(1, service.getInstalledVersions().size());
        assertEquals(List.of(), service.getInstalledVersions(otherMcFiles));
        assertEquals(Optional.empty(), service.tryGetVersion(otherMcFiles, "1.21.1"));
    }

    @Test
    void parentsAreResolvedInTheDirectoryOfTheChild() {
        write(mcFiles, "1.21.1", null);
        Path otherParent = write(otherMcFiles, "1.21.1", null);
        write(otherMcFiles, "fabric-1.21.1", "1.21.1");
        VersionJsonServiceImpl service = multiService();

        Version child = service.tryGetVersion(otherMcFiles, "fabric-1.21.1").orElseThrow();
        ProcessedVersion resolved = service.resolve(child);

        assertEquals(otherMcFiles, service.getMcFiles(child));
        assertEquals(Optional.of(otherParent), resolved.hierarchy().getFirst().asFile());
        assertEquals(child.asFile(), resolved.asFile());
    }

    @Test
    void parentInAnotherDirectoryIsNotUsed() {
        // nothing is shared between directories, the parent in the primary directory does not count
        write(mcFiles, "1.21.1", null);
        write(otherMcFiles, "fabric-1.21.1", "1.21.1");
        VersionJsonServiceImpl service = multiService();
        Version child = service.tryGetVersion(otherMcFiles, "fabric-1.21.1").orElseThrow();

        assertThrows(HeadlessMcException.class, () -> service.resolve(child));
        assertThrows(HeadlessMcException.class, () -> service.process(child));
    }

    @Test
    void missingParentIsInstalledIntoTheDirectoryOfTheChild() {
        write(otherMcFiles, "fabric-1.21.1", "1.21.1");
        VersionJsonServiceImpl service = multiService();
        Version child = service.tryGetVersion(otherMcFiles, "fabric-1.21.1").orElseThrow();
        List<McFiles> installedInto = new java.util.ArrayList<>();

        ProcessedVersion resolved = service.resolve(child, (dir, id) -> {
            installedInto.add(dir);
            write(dir, id, null);
        });

        assertEquals(List.of(otherMcFiles), installedInto);
        assertEquals(2, resolved.hierarchy().size());
        assertTrue(Files.exists(otherMcFiles.getVersionsDir().resolve("1.21.1").resolve("1.21.1.json")));
        assertFalse(Files.exists(mcFiles.getVersionsDir().resolve("1.21.1")));
    }

    @Test
    void versionOutsideOfAllDirectoriesResolvesInItsOwnDirectory() {
        McFiles unknown = McFiles.of(root.resolve("unknown-mc"));
        write(unknown, "1.21.1", null);
        Path file = write(unknown, "fabric-1.21.1", "1.21.1");
        VersionJsonServiceImpl service = multiService();

        Version child = service.getParser().parse(file);

        assertEquals(unknown, service.getMcFiles(child));
        assertEquals(2, service.resolve(child).hierarchy().size());
    }

    @Test
    void observeChangesSeesAVersionThatExistsInAnotherDirectory() {
        // by name the version is not new, it is already installed in the primary directory
        write(mcFiles, "1.21.1", null);
        VersionJsonServiceImpl service = multiService();

        List<Version> added = service.observeChanges(otherMcFiles, () -> write(otherMcFiles, "1.21.1", null));

        assertEquals(1, added.size());
        assertEquals(otherMcFiles, service.getMcFiles(added.getFirst()));
    }

    @Test
    void observeChangesIgnoresOtherDirectories() {
        VersionJsonServiceImpl service = multiService();

        assertEquals(List.of(), service.observeChanges(mcFiles, () -> write(otherMcFiles, "1.21.1", null)));
    }

    @Test
    void observeChangesWorksForADirectoryThatIsNotKnown() {
        McFiles unknown = McFiles.of(root.resolve("unknown-mc"));

        List<Version> added = service.observeChanges(unknown, () -> write(unknown, "1.21.1", null));

        assertEquals(List.of("1.21.1"), added.stream().map(Version::getId).toList());
    }

}
