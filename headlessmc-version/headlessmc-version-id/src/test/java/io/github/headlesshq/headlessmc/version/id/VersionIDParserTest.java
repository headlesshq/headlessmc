package io.github.headlesshq.headlessmc.version.id;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VersionIDParserTest {
    @Test
    public void testVersionIDParser() {
        VersionIDParser parser = new VersionIDParser();
        VersionID id = parser.parse("1.21.5");
        assertEquals("1.21.5", id.getVersion());
        assertFalse(id.isServer());
        assertNull(id.getPlatform());
        assertNull(id.getBuild());

        id = parser.parse("server:1.21.5");
        assertEquals("1.21.5", id.getVersion());
        assertTrue(id.isServer());
        assertNull(id.getPlatform());
        assertNull(id.getBuild());

        id = parser.parse("fabric:1.21.5");
        assertEquals("1.21.5", id.getVersion());
        assertFalse(id.isServer());
        assertEquals("fabric", id.getPlatform());
        assertNull(id.getBuild());

        id = parser.parse("server:fabric:1.21.5");
        assertEquals("1.21.5", id.getVersion());
        assertTrue(id.isServer());
        assertEquals("fabric", id.getPlatform());
        assertNull(id.getBuild());

        id = parser.parse("fabric:1.21.5:0.16.14");
        assertEquals("1.21.5", id.getVersion());
        assertFalse(id.isServer());
        assertEquals("fabric", id.getPlatform());
        assertEquals("0.16.14", id.getBuild());

        id = parser.parse("server:fabric:1.21.5:0.16.14");
        assertEquals("1.21.5", id.getVersion());
        assertTrue(id.isServer());
        assertEquals("fabric", id.getPlatform());
        assertEquals("0.16.14", id.getBuild());

        assertThrows(VersionIDParseException.class, () -> parser.parse("fabric:1.21.5:0.16.14:more"));
        assertThrows(VersionIDParseException.class, () -> parser.parse("server:fabric:1.21.5:0.16.14:more"));
    }

}
