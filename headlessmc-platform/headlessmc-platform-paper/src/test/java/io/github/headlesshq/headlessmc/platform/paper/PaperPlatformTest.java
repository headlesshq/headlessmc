package io.github.headlesshq.headlessmc.platform.paper;

import io.github.headlesshq.headlessmc.platform.Platform;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class PaperPlatformTest {
    @Inject
    @Paper
    Platform paperPlatform;

    @Test
    public void testPaperPlatform() {
        // TODO: check versionservice sorting
        assertInstanceOf(PaperPlatform.class, paperPlatform);
        assertEquals(Paper.PLATFORM_NAME, paperPlatform.getName());
        assertTrue(paperPlatform.getServerSupport().isPresent());
    }

}
