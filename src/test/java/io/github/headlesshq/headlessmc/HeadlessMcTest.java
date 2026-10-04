package io.github.headlesshq.headlessmc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class HeadlessMcTest {
    @Test
    public void testVersionReplacement() {
        // Check that ${version} was replaced with the current project version
        assertNotEquals("${version}", HeadlessMc.VERSION);
    }

}
