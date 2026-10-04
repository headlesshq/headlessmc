package io.github.headlesshq.headlessmc.platform.paper;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.mods.Mod;
import io.github.headlesshq.headlessmc.platform.mods.ModReader;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static io.smallrye.common.constraint.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@QuarkusTest
public class PaperModReaderTest {
    @Paper
    @Inject
    ModReader modReader;

    @Test
    public void testPaperModReader() throws HeadlessMcException, IOException {
        assertInstanceOf(PaperModReader.class, modReader);
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assert inputStream != null;
            Optional<List<Mod>> modReadingResult = modReader.readEntry(inputStream);
            assertTrue(modReadingResult.isPresent());
            List<Mod> mods = modReadingResult.get();
            assertEquals(1, mods.size());
            Mod mod = mods.getFirst();
            assertEquals("Paper-Test-Plugin", mod.name());
            assertEquals("Paper-Test-Plugin", mod.id());
            assertTrue(mod.description().isPresent());
            assertEquals("Paper Test Plugin", mod.description().get());
            assertEquals(1, mod.authors().size());
            assertEquals("PaperMC", mod.authors().getFirst());
        }
    }

    @Test
    public void testPaperModReaderMultipleAuthors() throws HeadlessMcException, IOException {
        assertInstanceOf(PaperModReader.class, modReader);
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("plugin-multiple-authors.yml")) {
            assert inputStream != null;
            Optional<List<Mod>> modReadingResult = modReader.readEntry(inputStream);
            assertTrue(modReadingResult.isPresent());
            List<Mod> mods = modReadingResult.get();
            assertEquals(1, mods.size());
            Mod mod = mods.getFirst();
            assertEquals(2, mod.authors().size());
            assertEquals("PaperMC", mod.authors().getFirst());
            assertEquals("TestAuthor", mod.authors().get(1));
        }
    }

}
