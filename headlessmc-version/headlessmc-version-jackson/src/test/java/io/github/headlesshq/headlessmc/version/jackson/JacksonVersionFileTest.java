package io.github.headlesshq.headlessmc.version.jackson;

import io.github.headlesshq.headlessmc.version.ProcessedVersion;
import io.github.headlesshq.headlessmc.version.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A version knows the version.json it has been parsed from, see {@link Version#asFile()}.
 */
class JacksonVersionFileTest {
    private static final String JSON = """
        {
          "id": "1.21.1",
          "type": "release",
          "mainClass": "net.minecraft.client.main.Main",
          "libraries": []
        }
        """;

    private final JacksonVersionParser parser = new JacksonVersionParser();

    private static Path write(Path dir, String content) throws IOException {
        Files.createDirectories(dir);
        return Files.writeString(dir.resolve("1.21.1.json"), content);
    }

    @Test
    void aVersionParsedFromAFileKnowsItsFile(@TempDir Path root) throws IOException {
        Path file = write(root.resolve("versions").resolve("1.21.1"), JSON);

        Version version = parser.parse(file);

        assertEquals("1.21.1", version.getId());
        assertEquals(Optional.of(file.toAbsolutePath().normalize()), version.asFile());
    }

    @Test
    void theFileIsAbsoluteAndNormalized(@TempDir Path root) throws IOException {
        Path file = write(root.resolve("versions").resolve("1.21.1"), JSON);
        Files.createDirectories(root.resolve("other"));
        Path indirect = root.resolve("other").resolve("..").resolve("versions").resolve("1.21.1").resolve("1.21.1.json");

        assertEquals(Optional.of(file.toAbsolutePath().normalize()), parser.parse(indirect).asFile());
    }

    @Test
    void aVersionParsedFromAStreamHasNoFile() {
        Version version = parser.parse(new StringReader(JSON));

        assertEquals("1.21.1", version.getId());
        assertEquals(Optional.empty(), version.asFile());
    }

    @Test
    void theFileIsNotPartOfTheVersionJson(@TempDir Path root) throws IOException {
        // a version.json cannot claim to be another file
        String withFile = JSON.replace("\"id\": \"1.21.1\",", "\"id\": \"1.21.1\", \"file\": \"/somewhere/else.json\",");
        Path file = write(root, withFile);

        assertEquals(Optional.of(file.toAbsolutePath().normalize()), parser.parse(file).asFile());
        assertEquals(Optional.empty(), parser.parse(new StringReader(withFile)).asFile());
    }

    @Test
    void theSameVersionInTwoDirectoriesIsNotTheSameInstallation(@TempDir Path root) throws IOException {
        Path first = write(root.resolve("mc").resolve("versions").resolve("1.21.1"), JSON);
        Path second = write(root.resolve("other-mc").resolve("versions").resolve("1.21.1"), JSON);

        Version a = parser.parse(first);
        Version b = parser.parse(second);

        assertNotEquals(a, b);
        assertEquals(2, new java.util.HashSet<>(List.of(a, b)).size());
        // the same file is the same version
        assertEquals(a, parser.parse(first));
        assertEquals(a.hashCode(), parser.parse(first).hashCode());
        // without a file, versions are equal by their content
        assertEquals(parser.parse(new StringReader(JSON)), parser.parse(new StringReader(JSON)));
        assertNotEquals(a, parser.parse(new StringReader(JSON)));
    }

    @Test
    void aProcessedVersionHasTheFileOfTheVersionItHasBeenCreatedFor(@TempDir Path root) throws IOException {
        Path parentFile = write(root.resolve("versions").resolve("1.21.1"), JSON);
        Path childDir = Files.createDirectories(root.resolve("versions").resolve("fabric"));
        Path childFile = Files.writeString(childDir.resolve("fabric.json"), """
            {"id": "fabric", "inheritsFrom": "1.21.1", "type": "release", "libraries": []}
            """);

        ProcessedVersion processed = new ProcessedVersion(List.of(parser.parse(parentFile), parser.parse(childFile)));

        // the child, the parent is only what it inherits from
        assertEquals(Optional.of(childFile.toAbsolutePath().normalize()), processed.asFile());
        assertEquals(
            Optional.empty(),
            new ProcessedVersion(List.of(parser.parse(parentFile), parser.parse(new StringReader(JSON)))).asFile()
        );
    }

}
