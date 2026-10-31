package io.github.headlesshq.headlessmc.plugins.version;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionRangeTest {
    @Test
    void testVersionOrder() {
        assertTrue(v("1.2.3").compareTo(v("1.10")) < 0);
        assertTrue(v("3.0.0-RC8").compareTo(v("3.0.0")) < 0);
        assertTrue(v("3.0.0-RC8").compareTo(v("3.0.0-RC10")) < 0);
        assertTrue(v("3.0.0-alpha").compareTo(v("3.0.0-beta")) < 0);
        assertEquals(v("1.0"), v("1.0.0"));
        assertEquals(v("1.0").hashCode(), v("1.0.0").hashCode());
        assertEquals(33, v("33.0.0-jre").major());
        assertEquals("jre", v("33.0.0-jre").qualifier());
        assertThrows(IllegalArgumentException.class, () -> v("abc"));
    }

    @Test
    void testBareVersionIsCompatibleRange() {
        VersionRange range = VersionRange.parse("1.2.0");
        assertTrue(range.contains(v("1.2.0")));
        assertTrue(range.contains(v("1.9.9")));
        assertFalse(range.contains(v("1.1.9")));
        assertFalse(range.contains(v("2.0.0")));
        assertFalse(range.contains(v("2.0.0-alpha")));
    }

    @Test
    void testMavenRanges() {
        assertTrue(VersionRange.parse("[1.0,2.0)").contains(v("1.5")));
        assertFalse(VersionRange.parse("[1.0,2.0)").contains(v("2.0")));
        assertFalse(VersionRange.parse("[1.0,2.0)").contains(v("2.0-RC1")));
        assertTrue(VersionRange.parse("[1.0,2.0-RC1)").contains(v("2.0-alpha")));
        assertTrue(VersionRange.parse("[1.0,2.0]").contains(v("2.0")));
        assertFalse(VersionRange.parse("(1.0,]").contains(v("1.0")));
        assertTrue(VersionRange.parse("(1.0,)").contains(v("100")));
        assertTrue(VersionRange.parse("[1.0]").contains(v("1.0.0")));
        assertFalse(VersionRange.parse("[1.0]").contains(v("1.0.1")));
        assertTrue(VersionRange.parse("*").contains(v("0.1")));
        assertTrue(VersionRange.parse("[3.0.0-RC8,4)").contains(v("3.0.0-RC8")));
        assertThrows(IllegalArgumentException.class, () -> VersionRange.parse("[1.0,2.0"));
        assertThrows(IllegalArgumentException.class, () -> VersionRange.parse("[1,2),[3,4)"));
    }

    @Test
    void testIntersect() {
        VersionRange intersection = VersionRange.parse("[1.0,2)").intersect(VersionRange.parse("[1.5,3)")).orElseThrow();
        assertEquals("[1.5,2)", intersection.toString());
        assertTrue(VersionRange.parse("[1.0,2)").intersect(VersionRange.parse("[2,3)")).isEmpty());
        assertEquals("[2]", VersionRange.parse("[1.0,2]").intersect(VersionRange.parse("[2,3)")).orElseThrow().toString());
        assertEquals("[1.0,2)", VersionRange.ANY.intersect(VersionRange.parse("[1.0,2)")).orElseThrow().toString());
    }

    private static Version v(String version) {
        return Version.parse(version);
    }

}
