package io.github.headlesshq.headlessmc.launcher.libraries;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.version.Version;
import org.jspecify.annotations.Nullable;
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

public class LibraryExtractorTest {
    private final LibraryExtractor extractor = new LibraryExtractor();

    private static Version.Extract extract(@Nullable List<String> exclude) {
        return () -> exclude;
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
    public void extractsEntriesSkippingExcludes(@TempDir Path root) throws IOException {
        Path zip = writeZip(root.resolve("natives.jar"), Map.of(
            "libnative.so", "native library",
            "subdir/other.so", "other library",
            "META-INF/MANIFEST.MF", "manifest"
        ));
        Path destination = root.resolve("out");

        extractor.extract(
            new TestLibraries.FakeLibrary("com.example:natives:1.0"),
            extract(List.of("META-INF/")),
            zip,
            destination
        );

        assertEquals("native library", Files.readString(destination.resolve("libnative.so")));
        assertEquals("other library", Files.readString(destination.resolve("subdir").resolve("other.so")));
        assertTrue(Files.notExists(destination.resolve("META-INF")));
    }

    @Test
    public void extractsEverythingWithoutExcludes(@TempDir Path root) throws IOException {
        Path zip = writeZip(root.resolve("natives.jar"), Map.of("libnative.so", "native library"));
        Path destination = root.resolve("out");

        extractor.extract(
            new TestLibraries.FakeLibrary("com.example:natives:1.0"),
            extract(null),
            zip,
            destination
        );

        assertTrue(Files.exists(destination.resolve("libnative.so")));
    }

    @Test
    public void detectsZipSlip(@TempDir Path root) throws IOException {
        Path zip = writeZip(root.resolve("evil.jar"), Map.of("../evil.txt", "evil"));
        Path destination = root.resolve("out");

        assertThrows(HeadlessMcIOException.class, () -> extractor.extract(
            new TestLibraries.FakeLibrary("com.example:evil:1.0"),
            extract(null),
            zip,
            destination
        ));
        assertTrue(Files.notExists(root.resolve("evil.txt")));
    }

}
