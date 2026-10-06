package io.github.headlesshq.headlessmc.mods.packwiz;

import io.github.headlesshq.headlessmc.exceptions.BadArgumentException;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PackwizServiceTest {
    private final PackwizService service = new PackwizService(new FakePlatformService(
        new FakeVanillaPlatform("1.21.1", "1.20.1"),
        FakePlatform.create("fabric", new String[]{"1.21.1", "0.16.14"}),
        FakePlatform.create("quilt", new String[]{"1.21.1", "0.26.0"}),
        FakePlatform.create("forge", new String[]{"1.20.1", "47.1.104"})
    ));

    @Test
    public void testParsePackToml() throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("pack.toml")) {
            assert is != null;
            String toml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(List.of(VersionArg.parse("fabric", "1.21.1", "0.16.14")), args(service.parsePackwizToml(toml)));
        }
    }

    @Test
    public void testForge() {
        List<VersionID> ids = service.parsePackwizToml("""
            [versions]
            minecraft = "1.20.1"
            forge = "47.1.104"
            """);
        assertEquals(List.of(VersionArg.parse("forge", "1.20.1", "47.1.104")), args(ids));
    }

    @Test
    public void testVanilla() {
        List<VersionID> ids = service.parsePackwizToml("""
            [versions]
            minecraft = "1.21.1"
            """);
        assertEquals(List.of(VersionArg.parse("vanilla", "1.21.1")), args(ids));
    }

    @Test
    public void testMultipleLoaders() {
        List<VersionID> ids = service.parsePackwizToml("""
            [versions]
            minecraft = "1.21.1"
            quilt = "0.26.0"
            fabric = "0.16.14"
            """);
        assertEquals(
            List.of(VersionArg.parse("quilt", "1.21.1", "0.26.0"), VersionArg.parse("fabric", "1.21.1", "0.16.14")),
            args(ids)
        );
    }

    @Test
    public void testUnknownLoaderIsSkipped() {
        List<VersionID> ids = service.parsePackwizToml("""
            [versions]
            minecraft = "1.21.1"
            liteloader = "1.21.1-SNAPSHOT"
            fabric = "0.16.14"
            """);
        assertEquals(List.of(VersionArg.parse("fabric", "1.21.1", "0.16.14")), args(ids));
    }

    @Test
    public void testOnlyUnknownLoadersFails() {
        assertThrows(BadArgumentException.class, () -> service.parsePackwizToml("""
            [versions]
            minecraft = "1.21.1"
            liteloader = "1.21.1-SNAPSHOT"
            """));
    }

    @Test
    public void testNoMinecraftVersionFails() {
        assertThrows(BadArgumentException.class, () -> service.parsePackwizToml("""
            [versions]
            fabric = "0.16.14"
            """));
    }

    private static List<VersionArg> args(List<VersionID> ids) {
        return ids.stream().map(VersionID::asArg).toList();
    }

}
