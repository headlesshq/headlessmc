package io.github.headlesshq.headlessmc.files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class McDirDetectorTest {
    @TempDir
    Path root;

    private final McDirDetector detector = new McDirDetector();

    /** Creates a {@code .minecraft} directory with a versions directory. */
    private Path mcDir(String... path) throws IOException {
        Path mcDir = root;
        for (String name : path) {
            mcDir = mcDir.resolve(name);
        }

        Files.createDirectories(mcDir.resolve("versions"));
        return mcDir;
    }

    /** The order of paths depends on the file system, e.g. on whether it is case sensitive. */
    private static List<Path> sorted(Path... paths) {
        return java.util.Arrays.stream(paths).sorted().toList();
    }

    @Test
    void findsMinecraftDirectoriesDownToTheDepth() throws IOException {
        Path top = mcDir("ssd", ".minecraft");
        Path nextToALauncher = mcDir("ssd", "Games", "HMCL", ".minecraft");
        Path tooDeep = mcDir("ssd", "a", "b", "c", ".minecraft");
        Path drive = root.resolve("ssd");

        assertEquals(List.of(top), detector.detect(List.of(drive), 1));
        assertEquals(sorted(top, nextToALauncher), detector.detect(List.of(drive), 3));
        assertEquals(sorted(top, nextToALauncher, tooDeep), detector.detect(List.of(drive), 4));
        assertEquals(List.of(), detector.detect(List.of(drive), 0));
    }

    @Test
    void searchesAllDrives() throws IOException {
        Path onSsd = mcDir("ssd", ".minecraft");
        Path onHdd = mcDir("hdd", "games", ".minecraft");
        Files.createDirectories(root.resolve("empty"));

        List<Path> found = detector.detect(
            List.of(root.resolve("ssd"), root.resolve("hdd"), root.resolve("empty"), root.resolve("not-mounted")), 3
        );

        assertEquals(sorted(onHdd, onSsd), found);
    }

    @Test
    void onlyDirectoriesThatLookLikeAMinecraftDirectoryAreFound() throws IOException {
        // called .minecraft, but nothing is installed in it
        Files.createDirectories(root.resolve("ssd").resolve(".minecraft").resolve("saves"));
        // installed versions, but not a .minecraft directory
        Files.createDirectories(root.resolve("ssd").resolve("backup").resolve("versions"));
        // a file
        Files.writeString(Files.createDirectories(root.resolve("ssd").resolve("files")).resolve(".minecraft"), "file");
        Path real = mcDir("ssd", "real", ".minecraft");

        assertEquals(List.of(real), detector.detect(List.of(root.resolve("ssd")), 3));
    }

    @Test
    void nothingIsSearchedForInsideAMinecraftDirectoryOrInSystemDirectories() throws IOException {
        Path mcDir = mcDir("ssd", ".minecraft");
        // a game directory of a modpack that brought its own .minecraft along
        mcDir("ssd", ".minecraft", "versions", ".minecraft");
        mcDir("ssd", "Windows", ".minecraft");
        mcDir("ssd", "$RECYCLE.BIN", ".minecraft");
        mcDir("ssd", "Program Files", ".minecraft");

        assertEquals(List.of(mcDir), detector.detect(List.of(root.resolve("ssd")), 4));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void linksAreNotFollowed() throws IOException {
        Path real = mcDir("ssd", ".minecraft");
        Files.createDirectories(root.resolve("hdd"));
        Files.createSymbolicLink(root.resolve("hdd").resolve(".minecraft"), real);
        Files.createSymbolicLink(root.resolve("hdd").resolve("loop"), root.resolve("hdd"));

        assertEquals(List.of(), detector.detect(List.of(root.resolve("hdd")), 4));
        assertEquals(List.of(real), detector.detect(List.of(root.resolve("ssd"), root.resolve("hdd")), 4));
    }

    @Test
    void theDrivesOfThisComputerCanBeListed() {
        // whatever they are, listing and searching them never fails
        List<Path> drives = detector.getDrives();

        assertTrue(drives.stream().allMatch(Path::isAbsolute), drives.toString());
        assertFalse(drives.contains(Path.of("/")), drives.toString());
        assertDoesNotThrow(() -> detector.detect(1));
    }

}
