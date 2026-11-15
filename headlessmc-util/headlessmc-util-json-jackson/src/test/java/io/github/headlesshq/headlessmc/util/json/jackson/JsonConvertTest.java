package io.github.headlesshq.headlessmc.util.json.jackson;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonParseException;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.util.json.RelativePathService;
import io.github.headlesshq.headlessmc.util.json.RelativePathSource;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A field of type {@link Object} carries json its owner does not know the type of.
 * {@link JsonService#convert(Object, Class)} lets whoever knows the type turn it into a typed value.
 */
class JsonConvertTest {
    private enum Kind {
        FIRST,
        SECOND
    }

    /** Something that carries data of an unknown type, like a profile carries the data of its launch target. */
    public record Envelope(String name, Object data) implements ReflectionRegistered {
    }

    public record Inner(String text, int number) implements ReflectionRegistered {
    }

    public record Data(
        String text,
        @Nullable Path file,
        Optional<Kind> kind,
        @Nullable Inner inner,
        List<String> list
    ) implements ReflectionRegistered {
    }

    private record Source(String identifier, Path path) implements RelativePathSource {
        @Override
        public String getIdentifier() {
            return identifier;
        }

        @Override
        public Path getPath() {
            return path;
        }
    }

    private static JsonService service(RelativePathSource... sources) {
        return new DefaultJacksonJsonService(new RelativePathService(List.of(sources)));
    }

    /** Writes the envelope and reads it back, its data is an untyped tree then. */
    private static Envelope roundTrip(JsonService service, Envelope envelope) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(out, envelope, true);
        return service.parse(new ByteArrayInputStream(out.toByteArray()), Envelope.class);
    }

    private static String write(JsonService service, Envelope envelope) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(out, envelope, true);
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    void convertsATreeThatHasBeenReadIntoItsType(@TempDir Path root) {
        JsonService service = service();
        Data data = new Data("text", root.resolve("file.json"), Optional.of(Kind.SECOND), new Inner("inner", 7), List.of("a", "b"));

        Envelope read = roundTrip(service, new Envelope("data", data));

        // not typed anymore
        assertInstanceOf(Map.class, read.data());
        assertEquals(data, service.convert(read.data(), Data.class));
    }

    @Test
    void aValueThatAlreadyHasTheTypeIsReturnedAsItIs() {
        Data data = new Data("text", null, Optional.empty(), null, List.of());

        assertSame(data, service().convert(data, Data.class));
    }

    @Test
    void missingAndNullValuesAreConverted() {
        JsonService service = service();
        Data data = new Data("text", null, Optional.empty(), null, List.of());

        Data converted = service.convert(roundTrip(service, new Envelope("data", data)).data(), Data.class);

        assertEquals(data, converted);
        // a tree in which the optional things are not there at all
        Data sparse = service.convert(Map.of("text", "text", "list", List.of("x")), Data.class);
        assertEquals("text", sparse.text());
        assertNull(sparse.file());
        assertNull(sparse.inner());
        assertEquals(List.of("x"), sparse.list());
    }

    @Test
    void pathsRelativeToAKnownLocationSurviveTheLocationMoving(@TempDir Path root) {
        Path oldLocation = root.resolve("old");
        Path newLocation = root.resolve("new");
        Data data = new Data("text", oldLocation.resolve("versions").resolve("a.json"), Optional.empty(), null, List.of());

        Envelope read = roundTrip(service(new Source("minecraft", oldLocation)), new Envelope("data", data));
        // the same tree, read where the location is somewhere else
        Data converted = service(new Source("minecraft", newLocation)).convert(read.data(), Data.class);

        assertEquals(newLocation.resolve("versions").resolve("a.json"), converted.file());
    }

    @Test
    void pathsOutsideOfKnownLocationsStayAbsolute(@TempDir Path root) {
        JsonService service = service(new Source("minecraft", root.resolve("mc")));
        Path file = root.resolve("another-disk").resolve("versions").resolve("a.json");
        Data data = new Data("text", file, Optional.empty(), null, List.of());

        Envelope read = roundTrip(service, new Envelope("data", data));

        assertEquals(file, service.convert(read.data(), Data.class).file());
    }

    @Test
    void pathsWrittenAsPlainStringsAreConverted(@TempDir Path root) {
        Path file = root.resolve("file.json");

        Data converted = service().convert(Map.of("text", "text", "file", file.toString(), "list", List.of()), Data.class);

        assertEquals(file, converted.file());
    }

    @Test
    void typedAndReadDataAreWrittenTheSame(@TempDir Path root) {
        JsonService service = service(new Source("minecraft", root));
        Data data = new Data("text", root.resolve("a.json"), Optional.of(Kind.FIRST), new Inner("inner", 7), List.of("a"));
        Envelope typed = new Envelope("data", data);

        Envelope read = roundTrip(service, typed);

        // both representations of the data produce the same json, saving something that has been read changes nothing
        assertEquals(write(service, typed), write(service, read));
        assertEquals(read, roundTrip(service, read));
        // but as objects they are not equal
        assertNotEquals(typed, read);
    }

    @Test
    void dataOfAnUnknownTypeIsKeptAsItIs() {
        JsonService service = service();
        Envelope envelope = new Envelope("unknown", Map.of("anything", List.of(1, "two", Map.of("three", true))));

        Envelope read = roundTrip(service, envelope);

        assertEquals(envelope, read);
        assertEquals(write(service, envelope), write(service, read));
    }

    @Test
    void aTreeOfTheWrongShapeIsRejected() {
        JsonService service = service();

        assertThrows(JsonParseException.class, () -> service.convert("just a string", Data.class));
        assertThrows(JsonParseException.class, () -> service.convert(Map.of("number", "not a number"), Inner.class));
        assertThrows(JsonParseException.class, () -> service.convert(Map.of("kind", "THIRD"), Data.class));
    }

    @Test
    void theLenientServiceConvertsToo() {
        Inner converted = new LenientJacksonJsonService().convert(Map.of("text", "text", "number", 7), Inner.class);

        assertEquals(new Inner("text", 7), converted);
    }

}
