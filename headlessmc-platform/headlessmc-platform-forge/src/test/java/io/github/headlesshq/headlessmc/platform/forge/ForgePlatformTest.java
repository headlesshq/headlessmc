package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.platform.mods.ModReader;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class ForgePlatformTest {
    @Inject
    @Forge
    Platform platform;

    @Test
    public void testForgePlatform() {
        assertInstanceOf(ForgePlatform.class, platform);
        List<ModReader> modReaders = platform.getModSupport().orElseThrow().modReaders();

        assertTrue(modReaders.stream().anyMatch(reader -> reader instanceof ForgeModsTomlReader),
            () -> "was: " + modReaders);
        assertTrue(modReaders.stream().anyMatch(reader -> reader instanceof McModInfoReader),
            () -> "was: " + modReaders);
    }

}
