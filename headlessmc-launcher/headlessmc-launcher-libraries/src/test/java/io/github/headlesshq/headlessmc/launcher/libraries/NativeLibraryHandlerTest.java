package io.github.headlesshq.headlessmc.launcher.libraries;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.net.hash.HashService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class NativeLibraryHandlerTest {
    @Inject
    HashService hashService;

    private NativeLibraryHandler handler(Path root) {
        AppFiles appFiles = new AppFiles() {
            @Override
            public Path getConfigDir() {
                return root.resolve("hmc");
            }

            @Override
            public Path getDataDir() {
                return root.resolve("hmc");
            }

            @Override
            public Path getCacheDir() {
                return root.resolve("hmc").resolve("cache");
            }

            @Override
            public Path getStateDir() {
                return root.resolve("hmc").resolve("state");
            }
        };
        return new NativeLibraryHandler(new LibraryExtractor(), hashService, appFiles);
    }

    private static Path writeZip(Path file, Map<String, String> entries) throws IOException {
        Files.createDirectories(file.getParent());
        try (OutputStream out = Files.newOutputStream(file);
             ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }

        return file;
    }

    @Test
    public void extractsNativeLibrariesToStableDir(@TempDir Path root) throws IOException {
        Path zip = writeZip(root.resolve("natives.jar"), Map.of("libnative.so", "native"));
        TestLibraries.FakeLibrary library = new TestLibraries.FakeLibrary("org.lwjgl:lwjgl-platform:2.9.4");
        library.extract = () -> List.of("META-INF/");
        LibraryFile nativeFile = new LibraryFile(library, zip, TestLibraries.download(null, 1L, null, "abc"));
        Path plainJar = root.resolve("plain.jar");
        LibraryFile plainFile = new LibraryFile(
            new TestLibraries.FakeLibrary("com.example:plain:1.0"), plainJar, null);

        NativeLibraryHandler handler = handler(root);
        Path first = handler.handleNativeLibraries(List.of(nativeFile, plainFile));
        Path second = handler.handleNativeLibraries(List.of(nativeFile, plainFile));

        assertEquals(first, second, "natives dir should be stable for the same libraries");
        assertTrue(first.startsWith(root.resolve("hmc").resolve("cache").resolve("natives")));
        assertEquals("native", Files.readString(first.resolve("libnative.so")));
    }

    @Test
    public void nativesDirDependsOnLibraries(@TempDir Path root) throws IOException {
        Path zip = writeZip(root.resolve("natives.jar"), Map.of("libnative.so", "native"));
        TestLibraries.FakeLibrary library = new TestLibraries.FakeLibrary("org.lwjgl:lwjgl-platform:2.9.4");
        library.extract = () -> null;
        TestLibraries.FakeLibrary other = new TestLibraries.FakeLibrary("org.lwjgl:lwjgl-platform:3.3.0");
        other.extract = () -> null;

        NativeLibraryHandler handler = handler(root);
        Path first = handler.handleNativeLibraries(List.of(new LibraryFile(library, zip, null)));
        Path second = handler.handleNativeLibraries(List.of(new LibraryFile(other, zip, null)));

        assertNotEquals(first, second);
    }

    @Test
    public void classifiedNativesWithoutExtractAreNotExtracted(@TempDir Path root) throws IOException {
        Path zip = writeZip(root.resolve("natives.jar"), Map.of("libnative.so", "native"));
        TestLibraries.FakeLibrary library =
            new TestLibraries.FakeLibrary("org.lwjgl:lwjgl-platform:2.9.4:natives-linux");

        Path dir = handler(root).handleNativeLibraries(List.of(new LibraryFile(library, zip, null)));

        assertTrue(Files.notExists(dir.resolve("libnative.so")),
            "library without extract config must not be extracted");
    }

}
