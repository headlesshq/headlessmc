package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.platform.mods.Mod;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.wildfly.common.Assert.assertTrue;

@QuarkusTest
public class ForgeModsTomlReaderTest {
    @Inject
    @Forge
    ForgeModsTomlReader tomlReader;

    @Test
    public void testForgeModsTomlReader() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("forge.mods.toml")) {
            assert inputStream != null;
            Optional<List<Mod>> result = tomlReader.readEntry(inputStream);
            assertTrue(result.isPresent());
            List<Mod> mods = result.get();
            // TODO: multiple mods
            assertEquals(1, mods.size());
            Mod mod = mods.getFirst();
            assertEquals("headlessmc", mod.id());
            assertEquals("HeadlessMc", mod.name());
            assertEquals(Optional.of("A Minecraft Launcher\n"), mod.description());
            assertEquals(Map.of(), mod.dependencies());
        }
    }

    @Test
    public void testReadMultipleMods() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("forge.mods-multiple-mods.toml")) {
            assert inputStream != null;
            Optional<List<Mod>> result = tomlReader.readEntry(inputStream);
            assertTrue(result.isPresent());
            List<Mod> mods = result.get();
            List<Mod> expected = List.of(
                new Mod(
                    "headlessmc",
                    "HeadlessMc",
                    Optional.of("A Minecraft Launcher\n"),
                    List.of("3arthqu4ke"),
                    Map.of()
                ),
                new Mod(
                    "headlessmc2",
                    "HeadlessMc2",
                    Optional.of("A Minecraft Launcher2\n"),
                    List.of("3arthqu4ke2"),
                    Map.of()
                )
            );

            assertEquals(expected, mods);
        }
    }

}
