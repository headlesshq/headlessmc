package io.github.headlesshq.headlessmc.platform.vanilla;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.Vanilla;
import io.github.headlesshq.headlessmc.platform.VanillaVersionService;
import io.github.headlesshq.headlessmc.platform.VersionService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class VanillaVersionServiceTest {
    @Inject
    VanillaVersionService vanillaVersionService;

    @Inject
    @Vanilla
    VersionService versionService;

    @Test
    public void testVersionServices() {
        assertEquals(vanillaVersionService, versionService);
        assertSame(vanillaVersionService, versionService);
    }

    @Test
    public void testGetVersions() throws HeadlessMcException {
        assertTrue(vanillaVersionService.hasVersion("1.12.2"));
        assertTrue(vanillaVersionService.hasVersion("1.14"));
    }

    @Test
    public void regressionTestCacheIteratorHasVersion() {
        assertTrue(vanillaVersionService.hasVersion("26.2"));
        assertTrue(vanillaVersionService.hasVersion("26.2"));
        assertTrue(vanillaVersionService.hasVersion("26.2"));
        assertTrue(vanillaVersionService.hasVersion("26.2"));
    }

}
