package io.github.headlesshq.headlessmc.plugins;

import io.github.headlesshq.headlessmc.net.hash.DefaultHashAlgorithmProvider;
import io.github.headlesshq.headlessmc.net.hash.HashAlgorithmProvider;
import io.github.headlesshq.headlessmc.net.hash.HashServiceImpl;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PluginScannerTest {
    @TempDir
    Path dir;

    @Test
    void testScan() throws IOException {
        jar(dir.resolve("b-plugin.jar"), Map.of(PluginManifest.PATH, """
            {
              "schemaVersion": 1,
              "id": "b-plugin",
              "version": "1.0.0",
              "name": "B Plugin",
              "authors": ["someone"],
              "headlessmc": "[3.0.0-RC8,4)",
              "java": 25,
              "unknownField": true,
              "dependencies": [{"id": "a-plugin", "versions": "[1,2)", "optional": false}],
              "libraries": [{
                "group": "com.example", "artifact": "lib", "version": "1.0.0",
                "path": "META-INF/jars/lib-1.0.0.jar", "sha256": "abc"
              }]
            }
            """));
        jar(dir.resolve("a-not-a-plugin.jar"), Map.of("foo.txt", "bar"));
        jar(dir.resolve("c-invalid-id.jar"), Map.of(PluginManifest.PATH, """
            {"schemaVersion": 1, "id": "Invalid Id", "version": "1.0.0", "headlessmc": "*"}
            """));
        Files.writeString(dir.resolve("readme.txt"), "not a jar");

        List<PluginFile> files = scanner().scan(dir);
        assertEquals(List.of("a-not-a-plugin.jar", "b-plugin.jar", "c-invalid-id.jar"),
            files.stream().map(PluginFile::fileName).toList());

        assertNotNull(files.get(0).error());
        assertNull(files.get(0).manifest());

        PluginFile plugin = files.get(1);
        assertNull(plugin.error());
        assertEquals(64, plugin.sha256().length());
        PluginManifest manifest = plugin.manifest();
        assertNotNull(manifest);
        assertEquals("b-plugin", manifest.id());
        assertEquals("B Plugin", manifest.displayName());
        assertEquals(25, manifest.java());
        assertEquals("a-plugin", manifest.dependencyList().getFirst().id());
        assertEquals("com.example:lib", manifest.libraryList().getFirst().module());

        assertTrue(files.get(2).error().contains("invalid id"));
    }

    @Test
    void testMissingDirectory() {
        assertEquals(List.of(), scanner().scan(dir.resolve("missing")));
    }

    @SuppressWarnings("unchecked")
    private static PluginScanner scanner() {
        Instance<HashAlgorithmProvider> providers = Mockito.mock(Instance.class);
        Mockito.when(providers.stream()).thenAnswer(_ -> Stream.of(new DefaultHashAlgorithmProvider()));
        return new PluginScanner(new HashServiceImpl(providers), new DefaultJacksonJsonService());
    }

    static void jar(Path jar, Map<String, String> entries) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(jar);
             ZipOutputStream zip = new ZipOutputStream(outputStream)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }

}
