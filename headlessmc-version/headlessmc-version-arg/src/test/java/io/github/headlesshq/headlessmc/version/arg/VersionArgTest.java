package io.github.headlesshq.headlessmc.version.arg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VersionArgTest {
    @Test
    public void testInvalidVersion() {
        assertThrows(IllegalArgumentException.class, () -> VersionArg.builder().build());
    }

    @Test
    public void testBuildClassifiers() {
        VersionArg stable = VersionArg.parse("fabric", "1.21.1", "Stable");
        assertTrue(stable.isStable());
        assertFalse(stable.isLatest());
        assertFalse(stable.isSpecificBuild());

        VersionArg latest = VersionArg.parse("fabric", "1.21.1", "latest");
        assertTrue(latest.isLatest());
        assertFalse(latest.isStable());
        assertFalse(latest.isSpecificBuild());

        VersionArg specific = VersionArg.parse("fabric", "1.21.1", "0.16.14");
        assertFalse(specific.isStable());
        assertFalse(specific.isLatest());
        assertTrue(specific.isSpecificBuild());

        assertFalse(VersionArg.parse("fabric", "stable").isStable());
    }

}
