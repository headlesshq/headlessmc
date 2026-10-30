package io.github.headlesshq.headlessmc.util.json.jackson;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.RelativePathService;
import io.github.headlesshq.headlessmc.util.json.RelativePathSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Files written before the {@link RelativePathSerializer} existed contain paths as plain strings,
 * these have to be read just like Jackson's default deserializer did.
 */
class LegacyPathCompatibilityTest {
    public record Holder(Path file, List<Path> files) implements ReflectionRegistered {}

    @TempDir
    Path root;

    private final DefaultJacksonJsonService service = new DefaultJacksonJsonService(new RelativePathService(List.of(
        new RelativePathSource() {
            @Override
            public String getIdentifier() {
                return "minecraft";
            }

            @Override
            public Path getPath() {
                return root.resolve(".minecraft");
            }
        }
    )));

    private Holder parse(String json) {
        return service.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), Holder.class);
    }

    @Test
    void jsonWrittenByJacksonsDefaultSerializerIsStillReadable() {
        Path file = root.resolve(".minecraft").resolve("versions").resolve("1.21.json");
        Path other = root.resolve("other file with spaces.txt");
        Holder original = new Holder(file, List.of(other));
        String legacyJson = JsonMapper.builder().build().writeValueAsString(original);
        assertTrue(legacyJson.contains("file:"), legacyJson);

        assertEquals(original, parse(legacyJson));
    }

    @Test
    void fileUrisAreParsed() {
        Path file = root.resolve("a b").resolve("c.txt");

        Holder holder = parse("{\"file\":\"" + file.toUri() + "\",\"files\":[]}");

        assertEquals(file, holder.file());
    }

    @Test
    void plainAbsolutePathsAreParsed() {
        Path file = root.resolve("c.txt");

        Holder holder = parse("{\"file\":\"" + file.toString().replace("\\", "\\\\") + "\",\"files\":[]}");

        assertEquals(file, holder.file());
    }

    @Test
    void plainRelativePathsAreParsed() {
        Holder holder = parse("{\"file\":\"mods/mod.jar\",\"files\":[\"a\"]}");

        assertEquals(Path.of("mods/mod.jar"), holder.file());
        assertEquals(List.of(Path.of("a")), holder.files());
    }

    @Test
    void legacyFilesAreRewrittenRelatively() {
        Path file = root.resolve(".minecraft").resolve("mods");
        Holder holder = parse("{\"file\":\"" + file.toUri() + "\",\"files\":[]}");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(out, holder, false);

        assertTrue(out.toString(StandardCharsets.UTF_8).contains("\"source\":\"minecraft\""));
    }

}
