package io.github.headlesshq.headlessmc.util.json.jackson;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonParseException;
import io.github.headlesshq.headlessmc.util.json.RelativePathService;
import io.github.headlesshq.headlessmc.util.json.RelativePathSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RelativePathSerializationTest {
    public record Files(Path file, List<Path> files) implements ReflectionRegistered {}

    public record Holder(Path file) implements ReflectionRegistered {}

    @TempDir
    Path root;

    private record Source(String getIdentifier, Path getPath) implements RelativePathSource {}

    private DefaultJacksonJsonService service(Map<String, Path> sources) {
        return new DefaultJacksonJsonService(new RelativePathService(
            sources.entrySet().stream().map(e -> new Source(e.getKey(), e.getValue())).toList()
        ));
    }

    private String write(DefaultJacksonJsonService service, ReflectionRegistered object) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(out, object, false);
        return out.toString(StandardCharsets.UTF_8);
    }

    private <T extends ReflectionRegistered> T parse(DefaultJacksonJsonService service, String json, Class<T> type) {
        return service.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), type);
    }

    private static String escape(Object path) {
        return path.toString().replace("\\", "\\\\");
    }

    @Test
    void pathsAreWrittenRelativeToTheBestMatchingSource() {
        Path home = root.resolve("home");
        Path minecraft = home.resolve(".minecraft");
        DefaultJacksonJsonService service = service(Map.of("user.home", home, "minecraft", minecraft));

        Path file = minecraft.resolve("versions").resolve("1.21").resolve("1.21.json");
        String json = write(service, new Holder(file));

        assertEquals("{\"file\":{\"source\":\"minecraft\",\"sourcePath\":\"" + escape(minecraft)
                         + "\",\"path\":\"versions/1.21/1.21.json\"}}", json);
        assertEquals(new Holder(file), parse(service, json, Holder.class));
    }

    @Test
    void pathsOutsideTheDeepestSourceUseTheEnclosingSource() {
        Path home = root.resolve("home");
        Path minecraft = home.resolve(".minecraft");
        DefaultJacksonJsonService service = service(Map.of("user.home", home, "minecraft", minecraft));

        Path file = home.resolve("Downloads").resolve("mod.jar");
        String json = write(service, new Holder(file));

        assertTrue(json.contains("\"source\":\"user.home\""), json);
        assertTrue(json.contains("\"path\":\"Downloads/mod.jar\""), json);
        assertEquals(new Holder(file), parse(service, json, Holder.class));
    }

    @Test
    void sourceDirectoriesThemselvesRoundTrip() {
        Path minecraft = root.resolve(".minecraft");
        DefaultJacksonJsonService service = service(Map.of("minecraft", minecraft));

        String json = write(service, new Holder(minecraft));

        assertTrue(json.contains("\"path\":\"\""), json);
        assertEquals(new Holder(minecraft), parse(service, json, Holder.class));
    }

    @Test
    void pathsOutsideOfAllSourcesAreWrittenAsIs() {
        DefaultJacksonJsonService service = service(Map.of("minecraft", root.resolve(".minecraft")));
        Path file = root.resolve("elsewhere").resolve("file.txt");

        String json = write(service, new Holder(file));

        assertEquals("{\"file\":{\"path\":\"" + escape(file) + "\"}}", json);
        assertEquals(new Holder(file), parse(service, json, Holder.class));
    }

    @Test
    void relativePathsStayRelative() {
        DefaultJacksonJsonService service = service(Map.of("minecraft", root));
        Path file = Path.of("mods", "mod.jar");

        String json = write(service, new Holder(file));

        assertEquals("{\"file\":{\"path\":\"" + escape(file) + "\"}}", json);
        assertEquals(new Holder(file), parse(service, json, Holder.class));
    }

    @Test
    void pathsAreResolvedAgainstTheCurrentLocationOfTheirSource() {
        Path oldMinecraft = root.resolve("old").resolve(".minecraft");
        Path newMinecraft = root.resolve("new").resolve(".minecraft");
        String json = write(service(Map.of("minecraft", oldMinecraft)),
                            new Holder(oldMinecraft.resolve("versions").resolve("1.21")));

        Holder holder = parse(service(Map.of("minecraft", newMinecraft)), json, Holder.class);

        assertEquals(newMinecraft.resolve("versions").resolve("1.21"), holder.file());
    }

    @Test
    void unknownSourcesFallBackToTheSerializedSourcePath() {
        Path minecraft = root.resolve(".minecraft");
        String json = write(service(Map.of("minecraft", minecraft)), new Holder(minecraft.resolve("mods")));

        Holder holder = parse(service(Map.of()), json, Holder.class);

        assertEquals(minecraft.resolve("mods"), holder.file());
    }

    @Test
    void unknownSourcesWithoutSourcePathFail() {
        assertThrows(JsonParseException.class, () -> parse(
            service(Map.of()), "{\"file\":{\"source\":\"minecraft\",\"path\":\"mods\"}}", Holder.class
        ));
    }

    @Test
    void pathsEscapingTheirSourceFail() {
        Path minecraft = root.resolve(".minecraft");
        DefaultJacksonJsonService service = service(Map.of("minecraft", minecraft));
        String sourcePath = escape(minecraft);

        for (String path : List.of("..", "../secret", "mods/../../secret", "./../secret")) {
            String json = "{\"file\":{\"source\":\"minecraft\",\"sourcePath\":\"" + sourcePath
                + "\",\"path\":\"" + path + "\"}}";
            assertThrows(JsonParseException.class, () -> parse(service, json, Holder.class), path);
        }
    }

    @Test
    void pathsEscapingTheSerializedSourcePathOfUnknownSourcesFail() {
        String json = "{\"file\":{\"source\":\"minecraft\",\"sourcePath\":\"" + escape(root.resolve(".minecraft"))
            + "\",\"path\":\"../secret\"}}";

        assertThrows(JsonParseException.class, () -> parse(service(Map.of()), json, Holder.class));
    }

    @Test
    void dotSegmentsThatStayInsideTheSourceAreAllowed() {
        Path minecraft = root.resolve(".minecraft");
        String json = "{\"file\":{\"source\":\"minecraft\",\"sourcePath\":\"" + escape(minecraft)
            + "\",\"path\":\"mods/../versions/./1.21\"}}";

        Holder holder = parse(service(Map.of("minecraft", minecraft)), json, Holder.class);

        assertEquals(minecraft.resolve("versions").resolve("1.21"), holder.file());
    }

    @Test
    void parentSegmentsAreNormalizedBeforeRelativizing() {
        Path home = root.resolve("home");
        Path minecraft = home.resolve(".minecraft");
        DefaultJacksonJsonService service = service(Map.of("user.home", home, "minecraft", minecraft));

        String json = write(service, new Holder(minecraft.resolve("..").resolve("Downloads").resolve("mod.jar")));

        assertTrue(json.contains("\"source\":\"user.home\""), json);
        assertTrue(json.contains("\"path\":\"Downloads/mod.jar\""), json);
        assertFalse(json.contains(".."), json);
    }

    @Test
    void siblingsSharingANamePrefixAreNotRelative() {
        DefaultJacksonJsonService service = service(Map.of("user.home", root.resolve("user")));
        Path file = root.resolve("user2").resolve("file.txt");

        String json = write(service, new Holder(file));

        assertEquals("{\"file\":{\"path\":\"" + escape(file) + "\"}}", json);
    }

    @Test
    void objectsWithoutPathFail() {
        assertThrows(JsonParseException.class, () -> parse(
            service(Map.of()), "{\"file\":{\"source\":\"minecraft\"}}", Holder.class
        ));
    }

    @Test
    void collectionsOfPathsAreRelative() {
        Path minecraft = root.resolve(".minecraft");
        DefaultJacksonJsonService service = service(Map.of("minecraft", minecraft));
        Files files = new Files(minecraft.resolve("a"), List.of(minecraft.resolve("b"), root.resolve("c")));

        String json = write(service, files);

        assertTrue(json.contains("\"path\":\"b\""), json);
        assertEquals(files, parse(service, json, Files.class));
    }

    @Test
    void lenientServiceUsesRelativePathsToo() {
        Path minecraft = root.resolve(".minecraft");
        LenientJacksonJsonService service = new LenientJacksonJsonService(
            new RelativePathService(List.of(new Source("minecraft", minecraft)))
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(out, new Holder(minecraft.resolve("mods")), false);

        assertTrue(out.toString(StandardCharsets.UTF_8).contains("\"source\":\"minecraft\""));
    }

}
