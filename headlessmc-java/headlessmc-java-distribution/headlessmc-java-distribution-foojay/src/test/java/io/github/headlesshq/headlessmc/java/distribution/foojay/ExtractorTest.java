package io.github.headlesshq.headlessmc.java.distribution.foojay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExtractorTest {
    @TempDir
    Path root;

    @Test
    void extractsZip() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("jdk/", "");
        entries.put("jdk/bin/java", "binary");

        new Extractor("zip", null).extract(root, new ByteArrayInputStream(Archives.zip(entries)));

        assertTrue(Files.isDirectory(root.resolve("jdk")));
        assertEquals("binary", Files.readString(root.resolve("jdk/bin/java")));
    }

    @Test
    void extractsTarGz() throws IOException {
        new Extractor("tar", "gz").extract(
            root, new ByteArrayInputStream(Archives.tarGz(Map.of("jdk/bin/java", "binary")))
        );

        assertEquals("binary", Files.readString(root.resolve("jdk/bin/java")));
    }

    @Test
    void detectsZipSlip() {
        byte[] archive = Archives.zip(Map.of("../escaped.txt", "evil"));

        IOException e = assertThrows(IOException.class,
            () -> new Extractor("zip", null).extract(root.resolve("target"), new ByteArrayInputStream(archive)));
        assertTrue(e.getMessage().contains("Zip slip"));
    }

    @Test
    void unknownArchiverThrows() {
        assertThrows(Exception.class,
            () -> new Extractor("not-an-archiver", null).extract(root, new ByteArrayInputStream(new byte[0])));
    }

}
