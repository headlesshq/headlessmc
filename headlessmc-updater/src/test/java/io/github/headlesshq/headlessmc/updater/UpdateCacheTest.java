package io.github.headlesshq.headlessmc.updater;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class UpdateCacheTest {
    private static final Instant NOW = Instant.parse("2026-10-26T12:00:00Z");
    private static final Duration DAY = Duration.ofHours(24);

    @TempDir
    Path tempDir;

    @Test
    public void testIsStale() {
        assertFalse(cache(NOW).isStale(NOW, DAY));
        assertFalse(cache(NOW.minus(Duration.ofHours(23))).isStale(NOW, DAY));
        assertTrue(cache(NOW.minus(DAY)).isStale(NOW, DAY));
        assertTrue(cache(NOW.minus(Duration.ofDays(3))).isStale(NOW, DAY));
        assertTrue(cache(NOW.plus(Duration.ofHours(1))).isStale(NOW, DAY));
    }

    @Test
    public void testWriteAndRead() {
        UpdateCache updateCache = new UpdateCache("headlesshq/headlessmc", NOW.toEpochMilli(), new GitHubRelease(
            "3.0.0-RC9",
            "https://github.com/headlesshq/headlessmc/releases/tag/3.0.0-RC9",
            List.of(new GitHubRelease.Asset("headlessmc.jar", "https://example.com/headlessmc.jar"))
        ));

        Path file = tempDir.resolve(UpdateCacheService.CACHE_FILE);
        createCacheService(file).set(updateCache);
        assertTrue(Files.exists(file));
        assertEquals(Optional.of(updateCache), createCacheService(file).get());
    }

    @Test
    public void testMissingFile() {
        assertEquals(Optional.empty(), createCacheService(tempDir.resolve(UpdateCacheService.CACHE_FILE)).get());
    }

    private UpdateCacheService createCacheService(Path file) {
        AppFiles appFiles = Mockito.mock(AppFiles.class);
        when(appFiles.getCacheDir()).thenReturn(tempDir);
        FileService fileService = Mockito.mock(FileService.class);
        when(fileService.getPath(tempDir, UpdateCacheService.CACHE_FILE)).thenReturn(file);
        return new UpdateCacheService(appFiles, fileService, new DefaultJacksonJsonService());
    }

    private static UpdateCache cache(Instant checkedAt) {
        return new UpdateCache("headlesshq/headlessmc", checkedAt.toEpochMilli(), new GitHubRelease("3.0.0", "", List.of()));
    }

}
