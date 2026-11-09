package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.patcher.Patcher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MuslClientSupportTest {
    @Test
    void testGlibcLoaderMissingOnMuslWithWindow() {
        assertTrue(MuslClientSupport.isGlibcLoaderMissing(OS.Type.LINUX_MUSL, List.of(Patcher.LOG4J), () -> false));
    }

    @Test
    void testGlibcLoaderNotMissingWithLoader() {
        assertFalse(MuslClientSupport.isGlibcLoaderMissing(OS.Type.LINUX_MUSL, List.of(), () -> true));
    }

    @Test
    void testGlibcLoaderNotNeededWhenHeadless() {
        assertFalse(MuslClientSupport.isGlibcLoaderMissing(OS.Type.LINUX_MUSL, List.of(Patcher.LWJGL), () -> false));
        assertFalse(MuslClientSupport.isGlibcLoaderMissing(OS.Type.LINUX_MUSL, List.of(Patcher.LWJGL_PRE), () -> false));
    }

    @Test
    void testGlibcLoaderNotNeededOnGlibc() {
        assertFalse(MuslClientSupport.isGlibcLoaderMissing(OS.Type.LINUX, List.of(), () -> false));
    }

    @Test
    void testSystemAllocatorOnMuslWithWindow() {
        assertEquals(
            Map.of(MuslClientSupport.LWJGL_ALLOCATOR, "system"),
            MuslClientSupport.getSystemProperties(OS.Type.LINUX_MUSL, List.of(Patcher.LOG4J))
        );
    }

    @Test
    void testNoSystemPropertiesWhenHeadlessOrOnGlibc() {
        assertEquals(Map.of(), MuslClientSupport.getSystemProperties(OS.Type.LINUX_MUSL, List.of(Patcher.LWJGL)));
        assertEquals(Map.of(), MuslClientSupport.getSystemProperties(OS.Type.LINUX, List.of()));
    }

}
