package io.github.headlesshq.headlessmc.mods.modrinth;

import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionPlatform;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Checks that the platform wires up in a real container.
 * What it does once wired is covered by {@link ModrinthPlatformTest}, which
 * serves the downloads from a mock instead of reaching out to modrinth.com.
 */
@QuarkusTest
public class ModrinthDistributionPlatformTest {
    @Inject
    ModrinthDistributionPlatform modrinthDistributionPlatform;

    @Test
    public void testModrinthDistributionPlatform() {
        assertNotNull(modrinthDistributionPlatform);
        assertEquals(ModDistributionPlatform.DEFAULT, modrinthDistributionPlatform.getName());
    }

}
