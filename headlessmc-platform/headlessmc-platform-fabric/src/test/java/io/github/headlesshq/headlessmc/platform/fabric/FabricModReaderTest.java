package io.github.headlesshq.headlessmc.platform.fabric;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.mods.Mod;
import io.github.headlesshq.headlessmc.platform.mods.ModReader;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class FabricModReaderTest {
    @Inject
    @Fabric
    Platform platform;

    public ModReader getReader() {
        List<ModReader> readers = platform.getModSupport().orElseThrow().modReaders();
        assertEquals(1, readers.size());
        ModReader reader = readers.getFirst();
        assertInstanceOf(FabricModReader.class, reader);
        return reader;
    }

    @Test
    public void testReadMod() throws HeadlessMcException, IOException {
        try (InputStream inputStream = FabricModReaderTest.class.getResourceAsStream("test-mod.json")) {
            assert inputStream != null;
            List<Mod> mods = getReader().readEntry(inputStream).orElseThrow();
            assertEquals(1, mods.size());
            Mod mod = mods.getFirst();
            assertEquals("test", mod.id());
            assertEquals("Test", mod.name());
            assertEquals("test", mod.description().orElseThrow());
            assertEquals(List.of("TestAuthor"), mod.authors());
            assertEquals(Map.of(
                "dependency1", List.of("1.0.0"),
                "dependency2", List.of("0.1.0", "0.2.0")
            ), mod.dependencies());
        }
    }

    @Test
    public void testMultipleMods() throws HeadlessMcException, IOException {
        try (InputStream inputStream = FabricModReaderTest.class.getResourceAsStream("multiple-mods.json")) {
            assert inputStream != null;
            List<Mod> mods = getReader().readEntry(inputStream).orElseThrow();
            assertEquals(2, mods.size());
            Mod mod = mods.getFirst();
            assertEquals("test2", mod.id());
            assertEquals("Test2", mod.name());
            assertTrue(mod.authors().isEmpty());
            assertTrue(mod.description().isEmpty());
            assertTrue(mod.dependencies().isEmpty());

            mod = mods.get(1);
            assertEquals("test3", mod.id());
            assertEquals("Test3", mod.name());
            assertEquals("test3", mod.description().orElseThrow());
            assertEquals(List.of("Test3Author1", "Test3Author2"), mod.authors());
            assertTrue(mod.dependencies().isEmpty());
        }
    }

    @Test
    public void testMultiLineStringMod() throws HeadlessMcException, IOException {
        try (InputStream inputStream = FabricModReaderTest.class.getResourceAsStream("multi-line-mod.json.txt")) {
            assert inputStream != null;
            List<Mod> mods = getReader().readEntry(inputStream).orElseThrow();
            assertEquals(1, mods.size());
            Mod mod = mods.getFirst();
            assertEquals("This is an expansion of the ETF mod, it adds support for OptiFine format Custom Entity Model (CEM) resource packs.\n" +
                             "  While still allowing you to disable this to use a different model mod :)", mod.description().orElseThrow());
            System.out.println(mod.description());
        }
    }

}
