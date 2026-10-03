package io.github.headlesshq.headlessmc.platform.neoforge;

import io.github.headlesshq.headlessmc.platform.mods.ModReader;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@QuarkusTest
public class NeoForgePlatformTest {
    @Inject
    @NeoForge
    Platform platform;

    @Test
    public void testNeoForgePlatform() {
        assertInstanceOf(NeoForgePlatform.class, platform);
        List<ModReader> modReaders = platform.getModSupport().orElseThrow().modReaders();
        assertEquals(1, modReaders.size());
        assertInstanceOf(NeoForgeModReader.class, modReaders.getFirst());
        // just making sure bean proxying did not destroy anything
        assertEquals(
            Set.of("META-INF/neoforge.mods.toml", "META-INF/forge.mods.toml"),
            modReaders.getFirst().getEntryNames()
        );
    }

}
