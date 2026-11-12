package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.os.LibCDetector;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.patcher.Patcher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MuslClientSupportTest {
    private static MuslClientSupport support(OS.Type type, boolean glibcLoaderInstalled) {
        return new MuslClientSupport(new OS("Linux", type, "6.12"), new LibCDetector() {
            @Override
            public boolean isGlibcLoaderInstalled() {
                return glibcLoaderInstalled;
            }
        });
    }

    @Test
    void testGlibcLoaderMissingOnMuslWithWindow() {
        assertTrue(support(OS.Type.LINUX_MUSL, false).isGlibcLoaderMissing(List.of(Patcher.LOG4J)));
    }

    @Test
    void testGlibcLoaderNotMissingWithLoader() {
        assertFalse(support(OS.Type.LINUX_MUSL, true).isGlibcLoaderMissing(List.of()));
    }

    @Test
    void testGlibcLoaderNotNeededWhenHeadless() {
        MuslClientSupport support = support(OS.Type.LINUX_MUSL, false);
        assertFalse(support.isGlibcLoaderMissing(List.of(Patcher.LWJGL)));
        assertFalse(support.isGlibcLoaderMissing(List.of(Patcher.LWJGL_PRE)));
    }

    @Test
    void testGlibcLoaderNotNeededOnGlibc() {
        assertFalse(support(OS.Type.LINUX, false).isGlibcLoaderMissing(List.of()));
    }

    @Test
    void testSystemAllocatorOnMuslWithWindow() {
        assertEquals(
            Map.of(MuslClientSupport.LWJGL_ALLOCATOR, "system"),
            support(OS.Type.LINUX_MUSL, true).getSystemProperties(List.of(Patcher.LOG4J))
        );
    }

    @Test
    void testNoSystemPropertiesWhenHeadlessOrOnGlibc() {
        assertEquals(Map.of(), support(OS.Type.LINUX_MUSL, true).getSystemProperties(List.of(Patcher.LWJGL)));
        assertEquals(Map.of(), support(OS.Type.LINUX, true).getSystemProperties(List.of()));
    }

}
