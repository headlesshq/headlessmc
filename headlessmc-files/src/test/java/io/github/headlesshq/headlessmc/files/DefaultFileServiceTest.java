package io.github.headlesshq.headlessmc.files;

import io.github.headlesshq.headlessmc.exceptions.FileException;
import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultFileServiceTest {
    private final DefaultFileService service = new DefaultFileService(new DefaultFileSystemProvider());

    @Test
    public void getPathJoinsSegments() {
        assertEquals(Path.of(""), service.getUserPath());
        assertEquals(Path.of("a", "b", "c"), service.getUserPath("a", "b", "c"));
        assertEquals(Path.of("base", "x", "y"), service.getPath(Path.of("base"), "x", "y"));
        assertEquals(Path.of("base"), service.getPath(Path.of("base")));
        assertEquals(Path.of("/base", "x", "y"), service.getUserPath("/base", "x", "y"));
        assertEquals(Path.of("base", "x", "y"), service.getPath(Path.of("base"), "x/y"));
        assertEquals(Path.of("a"), service.getPath(Path.of(""), "a"));
    }

    @Test
    public void getPathNormalizesRedundantSegments() {
        assertEquals(Path.of("base", "x"), service.getPath(Path.of("base"), "./x"));
        assertEquals(Path.of("base", "x", "y"), service.getPath(Path.of("base"), "x/./y"));
        // slips which stay inside of the base are fine, they just get normalized away
        assertEquals(Path.of("base", "y"), service.getPath(Path.of("base"), "x/../y"));
        assertEquals(Path.of("base", "y"), service.getPath(Path.of("base"), "x", "../y"));
        assertEquals(Path.of("base", "x"), service.getPath(Path.of("base"), "x", ""));
    }

    @Test
    public void getPathRejectsSlips() {
        assertThrows(FileException.class, () -> service.getPath(Path.of("base"), ".."));
        assertThrows(FileException.class, () -> service.getPath(Path.of("base"), "../x"));
        assertThrows(FileException.class, () -> service.getPath(Path.of("base"), "x/../../y"));
        assertThrows(FileException.class, () -> service.getPath(Path.of("base"), "x", "../../y"));
        assertThrows(FileException.class, () -> service.getPath(Path.of("/base"), "../etc"));
    }

    @Test
    public void getPathRejectsAbsoluteSegments() {
        assertThrows(FileException.class, () -> service.getPath(Path.of("base"), "/etc"));
        assertThrows(FileException.class, () -> service.getPath(Path.of("base"), "x", "/etc/passwd"));
    }

    @Test
    public void getPathRejectsSegmentsWhichAreNotChildren() {
        assertThrows(FileException.class, () -> service.getPath(Path.of("base"), ""));
        assertThrows(FileException.class, () -> service.getPath(Path.of("base"), "."));
        assertThrows(FileException.class, () -> service.getPath(Path.of("base"), "x/.."));
        assertThrows(FileException.class, () -> service.getPath(Path.of("base"), "x", ".."));
    }

    @Test
    public void getPathDoesNotRestrictTheBase() {
        assertEquals(Path.of("..", "base"), service.getUserPath("../base"));
        assertEquals(Path.of("..", "base", "x"), service.getUserPath("../base", "x"));
        assertEquals(Path.of("..", "x"), service.getPath(Path.of(".."), "x"));
    }

    @Test
    public void getPathAllowsSlips() {
        assertEquals(Path.of(""), service.getUserPath());
        assertEquals(Path.of("base", "..", "x"), service.getUserPath("base", "../x"));
        assertEquals(Path.of("/etc"), service.getUserPath("base", "/etc"));
    }

    @Test
    public void deletesFilesAndDirectoryTrees(@TempDir Path dir) throws IOException {
        Path file = Files.writeString(dir.resolve("file.txt"), "content");
        service.delete(file);
        assertTrue(Files.notExists(file));

        Path tree = Files.createDirectories(dir.resolve("tree").resolve("nested"));
        Files.writeString(tree.resolve("file.txt"), "content");
        service.delete(dir.resolve("tree"));
        assertTrue(Files.notExists(dir.resolve("tree")));

        assertDoesNotThrow(() -> service.delete(dir.resolve("does-not-exist")));
    }

    @Test
    public void tempProvidesAndCleansUpDirectory() {
        List<Path> seen = new ArrayList<>();
        service.temp(path -> {
            assertTrue(Files.isDirectory(path));
            Files.writeString(path.resolve("file.txt"), "content");
            seen.add(path);
        });

        assertEquals(1, seen.size());
        assertTrue(Files.notExists(seen.getFirst()));
    }

    @Test
    public void tempWrapsExceptionsAndStillCleansUp() {
        List<Path> seen = new ArrayList<>();
        assertThrows(FileException.class, () -> service.temp(path -> {
            seen.add(path);
            throw new IOException("simulated failure");
        }));
        assertTrue(Files.notExists(seen.getFirst()));

        assertThrows(UncheckedInterruptedException.class, () -> service.temp(path -> {
            throw new InterruptedException("simulated interrupt");
        }));
    }

    @Test
    public void atomicReplacesDestination(@TempDir Path dir) throws IOException {
        Path destination = Files.createDirectories(dir.resolve("destination"));
        Files.writeString(destination.resolve("old.txt"), "old");

        service.atomic(destination, path -> Files.writeString(path.resolve("new.txt"), "new"));

        assertTrue(Files.exists(destination.resolve("new.txt")));
        assertTrue(Files.notExists(destination.resolve("old.txt")));
    }

    @Test
    public void createWritesStreamAndParentDirs(@TempDir Path dir) throws IOException {
        Path target = dir.resolve("nested").resolve("file.txt");
        service.create(target, new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)));
        assertEquals("content", Files.readString(target));
    }

    @Test
    public void createCleansUpOnFailingStream(@TempDir Path dir) {
        Path target = dir.resolve("file.txt");
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("simulated read failure");
            }
        };

        assertThrows(FileException.class, () -> service.create(target, failing));
        assertTrue(Files.notExists(target));
    }

    @Test
    public void createWithConsumerWritesFile(@TempDir Path dir) throws IOException {
        Path target = dir.resolve("nested").resolve("file.txt");
        service.create(target, out -> {
            try {
                out.write("content".getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals("content", Files.readString(target));
    }

    @Test
    public void createWithConsumerThrowsForDirectoryTarget(@TempDir Path dir) {
        assertThrows(FileException.class, () -> service.create(dir, out -> {
        }));
    }

    @Test
    public void saveMoveMovesFile(@TempDir Path dir) throws IOException {
        Path origin = Files.writeString(dir.resolve("origin.txt"), "content");
        Path destination = dir.resolve("nested").resolve("destination.txt");

        service.saveMove(origin, destination);

        assertTrue(Files.notExists(origin));
        assertEquals("content", Files.readString(destination));
    }

    @Test
    public void saveMoveThrowsWithoutOrigin(@TempDir Path dir) {
        assertThrows(FileException.class,
            () -> service.saveMove(dir.resolve("missing"), dir.resolve("destination")));
    }

    @Test
    public void saveMoveMergesIntoNonEmptyDirectory(@TempDir Path dir) throws IOException {
        Path origin = Files.createDirectories(dir.resolve("origin"));
        Files.writeString(origin.resolve("new.txt"), "new");
        Path destination = Files.createDirectories(dir.resolve("destination"));
        Files.writeString(destination.resolve("existing.txt"), "existing");

        service.saveMove(origin, destination);

        assertTrue(Files.notExists(origin));
        assertEquals("new", Files.readString(destination.resolve("new.txt")));
        assertEquals("existing", Files.readString(destination.resolve("existing.txt")));
    }

    @Test
    public void extractsResourcesFromClasspath(@TempDir Path dir) throws IOException {
        Path target = dir.resolve("extracted.txt");
        service.extractResource("test-resource.txt", target);
        assertEquals("test resource content", Files.readString(target).trim());
    }

    @Test
    public void extractResourceThrowsForUnknownResource(@TempDir Path dir) {
        assertThrows(FileException.class,
            () -> service.extractResource("does-not-exist.txt", dir.resolve("target")));
    }

    @Test
    public void ensureFileExistsOnlyWritesWhenMissing(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("file.txt");
        service.ensureFileExists(file, "default".getBytes(StandardCharsets.UTF_8));
        assertEquals("default", Files.readString(file));

        service.ensureFileExists(file, "other".getBytes(StandardCharsets.UTF_8));
        assertEquals("default", Files.readString(file));
    }

    @Test
    public void deleteFileAndEmptyParentDirsStopsAtNonEmptyParent(@TempDir Path dir) throws IOException {
        Path keep = Files.createDirectories(dir.resolve("keep"));
        Files.writeString(keep.resolve("sibling.txt"), "content");
        Path nested = Files.createDirectories(keep.resolve("a").resolve("b"));
        Path file = Files.writeString(nested.resolve("file.txt"), "content");

        service.deleteFileAndEmptyParentDirs(file);

        assertTrue(Files.notExists(keep.resolve("a")));
        assertTrue(Files.exists(keep.resolve("sibling.txt")));
    }

    @Test
    public void reportsDefaultFileSystem() {
        assertFalse(service.fs().isVirtual());
        assertEquals(Path.of("").getFileSystem(), service.fs().getFileSystem());
    }

}
