package io.github.headlesshq.headlessmc.util.json.jackson;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonParseException;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DefaultJacksonJsonServiceTest {
    public record Example(String name, int value) implements ReflectionRegistered {}

    private final DefaultJacksonJsonService service = new DefaultJacksonJsonService();

    private InputStream stream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void nameIsJackson() {
        assertEquals(JsonService.JACKSON, service.getName());
    }

    @Test
    void parsesFromAStream() {
        assertEquals(new Example("a", 1), service.parse(stream("{\"name\":\"a\",\"value\":1}"), Example.class));
    }

    @Test
    void parsesFromAStreamWithATypeLiteral() {
        Example example = service.parse(stream("{\"name\":\"a\",\"value\":1}"), new TypeLiteral<>() {});
        assertEquals(new Example("a", 1), example);
    }

    @Test
    void parsesListsFromAStream() {
        List<Example> examples = service.parseList(
            stream("[{\"name\":\"a\",\"value\":1},{\"name\":\"b\",\"value\":2}]"), new TypeLiteral<>() {}
        );

        assertEquals(List.of(new Example("a", 1), new Example("b", 2)), examples);
    }

    @Test
    void singleValuesAreAcceptedAsArrays() {
        List<Example> examples = service.parseList(stream("{\"name\":\"a\",\"value\":1}"), new TypeLiteral<>() {});

        assertEquals(List.of(new Example("a", 1)), examples);
    }

    @Test
    void parsesFromAFile(@TempDir Path root) throws IOException {
        Path file = Files.writeString(root.resolve("example.json"), "{\"name\":\"a\",\"value\":1}");

        assertEquals(new Example("a", 1), service.parse(file, Example.class));
        assertEquals(new Example("a", 1), service.parse(file, new TypeLiteral<Example>() {}));
    }

    @Test
    void parsesTypesThatAreOnlyRegisteredForReflection() {
        List<String> strings = service.parseRegisteredForReflection(stream("[\"a\",\"b\"]"), new TypeLiteral<>() {});

        assertEquals(List.of("a", "b"), strings);
    }

    @Test
    void writesToAStream() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(out, new Example("a", 1), false);

        assertEquals("{\"name\":\"a\",\"value\":1}", out.toString(StandardCharsets.UTF_8));
    }

    @Test
    void writesPrettyToAStream() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(out, new Example("a", 1), true);

        assertTrue(out.toString(StandardCharsets.UTF_8).contains("\n"));
    }

    @Test
    void writesToAFileCreatingParentDirectories(@TempDir Path root) throws IOException {
        Path file = root.resolve("nested").resolve("example.json");

        service.write(file, new Example("a", 1), false);
        assertEquals("{\"name\":\"a\",\"value\":1}", Files.readString(file));

        service.write(file, new Example("b", 2), true);
        assertTrue(Files.readString(file).contains("\n"));
    }

    @Test
    void parsesVersionedEnvelopes() {
        Optional<Example> example = service.parseVersioned(
            stream("{\"version\":1,\"object\":{\"name\":\"a\",\"value\":1}}"), new TypeLiteral<>() {}, 1
        );

        assertEquals(Optional.of(new Example("a", 1)), example);
    }

    @Test
    void versionedEnvelopesOfOtherVersionsAreSkipped() {
        TypeLiteral<Example> type = new TypeLiteral<>() {};

        assertEquals(Optional.empty(), service.parseVersioned(
            stream("{\"version\":2,\"object\":{\"name\":\"a\",\"value\":1}}"), type, 1
        ));
        assertEquals(Optional.empty(), service.parseVersioned(
            stream("{\"object\":{\"name\":\"a\",\"value\":1}}"), type, 1
        ));
    }

    @Test
    void malformedJsonThrows(@TempDir Path root) throws IOException {
        Path file = Files.writeString(root.resolve("broken.json"), "not json");
        TypeLiteral<Example> type = new TypeLiteral<>() {};

        assertThrows(JsonParseException.class, () -> service.parse(stream("not json"), Example.class));
        assertThrows(JsonParseException.class, () -> service.parse(stream("not json"), type));
        assertThrows(JsonParseException.class,
            () -> service.parseList(stream("not json"), new TypeLiteral<List<Example>>() {}));
        assertThrows(JsonParseException.class, () -> service.parse(file, Example.class));
        assertThrows(JsonParseException.class, () -> service.parse(file, type));
        assertThrows(JsonParseException.class,
            () -> service.parseRegisteredForReflection(stream("not json"), type));
        assertThrows(JsonParseException.class, () -> service.parseVersioned(stream("not json"), type, 1));
    }

}
