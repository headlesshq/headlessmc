package io.github.headlesshq.headlessmc.version.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionComparatorTest {
    private final VersionComparator comparator = VersionComparator.INSTANCE;

    @Test
    void comparesEqualVersionsAsEqual() {
        assertEquals(0, comparator.compare("1.2.3", "1.2.3"));
        assertEquals(0, comparator.compare("1.2", "1.2.0"));
        assertEquals(0, comparator.compare("1", "1.0.0"));
    }

    @Test
    void comparesNumericVersionsCorrectly() {
        assertTrue(comparator.compare("1.2.3", "1.2.4") < 0);
        assertTrue(comparator.compare("1.2.5", "1.2.4") > 0);

        assertTrue(comparator.compare("2.0.0", "1.9.9") > 0);
        assertTrue(comparator.compare("1.10.0", "1.2.0") > 0);
    }

    @Test
    void comparesDifferentLengthVersions() {
        assertTrue(comparator.compare("1.2", "1.2.1") < 0);
        assertTrue(comparator.compare("1.2.1", "1.2") > 0);
    }

    @Test
    void handlesNonNumericPartsInFirstVersionAsSmaller() {
        assertTrue(comparator.compare("1.a.3", "1.2.3") < 0);
    }

    @Test
    void handlesNonNumericPartsInSecondVersionAsGreater() {
        assertTrue(comparator.compare("1.2.3", "1.a.3") > 0);
    }

}
