package io.github.headlesshq.headlessmc.version.arg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VersionArgTest {
    @Test
    public void testInvalidVersion() {
        assertThrows(IllegalArgumentException.class, () -> VersionArg.builder().build());
    }

}
