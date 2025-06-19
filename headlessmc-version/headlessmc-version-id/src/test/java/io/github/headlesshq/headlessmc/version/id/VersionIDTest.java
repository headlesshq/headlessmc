package io.github.headlesshq.headlessmc.version.id;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VersionIDTest {
    @Test
    public void testToString() {
        VersionID versionID = new VersionID("1.12.2", null);
        assertEquals("1.12.2", versionID.toString());
        versionID = new VersionID("1.12.2", null, true);
        assertEquals("server:1.12.2", versionID.toString());
        versionID = new VersionID("1.12.2", "forge", false);
        assertEquals("forge:1.12.2", versionID.toString());
        versionID = new VersionID("1.12.2", "forge", true);
        assertEquals("server:forge:1.12.2", versionID.toString());
        versionID = new VersionID(false, "1.12.2", "forge", "latest");
        assertEquals("forge:1.12.2:latest", versionID.toString());
        versionID = new VersionID(true, "1.12.2", "forge", "latest");
        assertEquals("server:forge:1.12.2:latest", versionID.toString());
    }

}
