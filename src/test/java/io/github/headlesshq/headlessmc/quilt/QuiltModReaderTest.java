package io.github.headlesshq.headlessmc.quilt;

import io.github.headlesshq.headlessmc.platform.mods.Mod;
import io.github.headlesshq.headlessmc.util.json.jackson.LenientJacksonJsonService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.*;

class QuiltModReaderTest {
    private static final String QUILT_MOD_JSON = """
        {
          "schema_version": 1,
          "quilt_loader": {
            "group": "com.example",
            "id": "example_mod",
            "version": "1.0.0",
            "metadata": {
              "name": "Example Mod",
              "description": "An example mod.",
              "contributors": {"Someone": "Owner", "Other": ["Developer"]}
            },
            "depends": [
              "quilt_loader",
              {"id": "minecraft", "versions": ">=1.21"},
              {"id": "optional_mod", "optional": true},
              [{"id": "fabric-api", "versions": ["*"]}, "quilted_fabric_api"]
            ]
          }
        }
        """;

    private final QuiltModReader reader = new QuiltModReader(new LenientJacksonJsonService());

    @Test
    void testReadEntry() {
        Optional<List<Mod>> mods = reader.readEntry(new ByteArrayInputStream(QUILT_MOD_JSON.getBytes(StandardCharsets.UTF_8)));
        assertTrue(mods.isPresent());
        Mod mod = mods.get().getFirst();
        assertEquals("example_mod", mod.id());
        assertEquals("Example Mod", mod.name());
        assertEquals(Optional.of("An example mod."), mod.description());
        assertEquals(List.of("Someone", "Other"), mod.authors());
        assertEquals(Map.of(
            "quilt_loader", List.of("*"),
            "minecraft", List.of(">=1.21"),
            "fabric-api", List.of("*"),
            "quilted_fabric_api", List.of("*")
        ), mod.dependencies());
    }

    @Test
    void testMinimal() {
        String json = "{\"schema_version\": 1, \"quilt_loader\": {\"id\": \"minimal\"}}";
        Mod mod = reader.readEntry(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))).orElseThrow().getFirst();
        assertEquals("minimal", mod.name());
        assertEquals(List.of(), mod.authors());
    }

    @Test
    void testReadJar(@TempDir Path dir) throws IOException {
        Path jar = dir.resolve("mod.jar");
        try (OutputStream outputStream = Files.newOutputStream(jar); JarOutputStream jarOutputStream = new JarOutputStream(outputStream)) {
            jarOutputStream.putNextEntry(new ZipEntry(QuiltModReader.QUILT_MOD_JSON));
            jarOutputStream.write(QUILT_MOD_JSON.getBytes(StandardCharsets.UTF_8));
            jarOutputStream.closeEntry();
        }

        assertEquals("example_mod", reader.read(jar).getFirst().id());
        assertEquals(List.of(), reader.read(writeEmptyJar(dir)));
    }

    private static Path writeEmptyJar(Path dir) throws IOException {
        Path jar = dir.resolve("empty.jar");
        try (OutputStream outputStream = Files.newOutputStream(jar); JarOutputStream ignored = new JarOutputStream(outputStream)) {
            return jar;
        }
    }

}
