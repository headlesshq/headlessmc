package io.github.headlesshq.headlessmc.java.sources;

import io.github.headlesshq.headlessmc.os.OS;
import io.quarkus.test.component.QuarkusComponentTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusComponentTest({JavaHomeFinder.class, JavaExecutableFinder.class, TestBeans.class})
class JavaHomeFinderTest {
    private static final OS WINDOWS = new OS("windows", OS.Type.WINDOWS, "11");

    @TempDir
    Path root;

    /** Runs on the linux {@link OS} produced by {@link TestBeans}. */
    @Inject
    JavaHomeFinder finder;

    /** Windows is not the {@link OS} the container is set up for, so this one is built by hand. */
    private JavaHomeFinder windowsFinder() {
        return new JavaHomeFinder(new JavaExecutableFinder(WINDOWS), FakeJavaConfig.holder());
    }

    private Path createHome(Path dir, String executable) throws IOException {
        Path bin = dir.resolve("bin");
        Files.createDirectories(bin);
        Files.writeString(bin.resolve(executable), "binary");
        return dir;
    }

    @Test
    void findsTheHomeItself() throws IOException {
        Path home = createHome(root, "java");

        assertEquals(home, finder.find(home).orElseThrow());
    }

    @Test
    void findsWindowsExecutables() throws IOException {
        Path home = createHome(root, "java.exe");

        assertEquals(home, windowsFinder().find(home).orElseThrow());
        // java.exe is not what we look for on linux
        assertTrue(finder.find(home).isEmpty());
    }

    @Test
    void findsDeeplyNestedHomes() throws IOException {
        // e.g. OpenJDK8U-jre_x64_mac_hotspot_8u502b07/jdk8u502-b07-jre/Contents/Home on macOS
        Path home = createHome(
            root.resolve("OpenJDK8U-jre_x64_mac_hotspot_8u502b07")
                .resolve("jdk8u502-b07-jre")
                .resolve("Contents")
                .resolve("Home"),
            "java"
        );

        assertEquals(home, finder.find(root).orElseThrow());
    }

    @Test
    void doesNotSearchDeeperThanMaxDepth() throws IOException {
        Path home = createHome(root.resolve("a").resolve("b"), "java");

        assertTrue(finder.find(root, 1).isEmpty());
        assertEquals(home, finder.find(root, 2).orElseThrow());
    }

    @Test
    void doesNotSearchDeeperThanTheConfiguredDepth() throws IOException {
        Path dir = root;
        for (int depth = 0; depth <= FakeJavaConfig.MAX_SCAN_DEPTH; depth++) {
            dir = dir.resolve("nested-" + depth);
        }

        createHome(dir, "java");

        assertTrue(finder.find(root).isEmpty());
        assertEquals(dir, finder.find(root, FakeJavaConfig.MAX_SCAN_DEPTH + 1).orElseThrow());
    }

    @Test
    void searchesBreadthFirst() throws IOException {
        createHome(root.resolve("a").resolve("nested"), "java");
        Path shallow = createHome(root.resolve("b"), "java");

        assertEquals(shallow, finder.find(root).orElseThrow());
    }

    @Test
    void returnsEmptyWithoutAnyExecutable() throws IOException {
        Files.createDirectories(root.resolve("bin"));
        Files.createDirectories(root.resolve("docs").resolve("api"));

        assertTrue(finder.find(root).isEmpty());
        assertTrue(finder.find(root.resolve("does-not-exist")).isEmpty());
    }

    @Test
    void findsExecutableInsideAHome() throws IOException {
        Path home = createHome(root, "java");

        Optional<Path> executable = finder.findExecutable(home);

        assertTrue(executable.isPresent());
        assertEquals(home.resolve("bin").resolve("java"), executable.get());
        assertTrue(finder.findExecutable(root.resolve("nope")).isEmpty());
    }

}
