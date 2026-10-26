package io.github.headlesshq.headlessmc.updater;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import jakarta.enterprise.util.TypeLiteral;
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

public class UpdateCheckCacheTest {
    private static final Instant NOW = Instant.parse("2026-10-26T12:00:00Z");
    private static final Duration DAY = Duration.ofHours(24);
    private static final TypeLiteral<UpdateCheck<LatestRelease>> TYPE = new TypeLiteral<>() {};

    @TempDir
    Path tempDir;

    @Test
    public void testIsStale() {
        assertFalse(check(NOW).isStale(NOW, DAY));
        assertFalse(check(NOW.minus(Duration.ofHours(23))).isStale(NOW, DAY));
        assertTrue(check(NOW.minus(DAY)).isStale(NOW, DAY));
        assertTrue(check(NOW.minus(Duration.ofDays(3))).isStale(NOW, DAY));
        assertTrue(check(NOW.plus(Duration.ofHours(1))).isStale(NOW, DAY));
    }

    @Test
    public void testWriteAndRead() {
        LatestRelease latestRelease = new LatestRelease("headlesshq/headlessmc", new GitHubRelease(
            "3.0.0-RC9",
            "https://github.com/headlesshq/headlessmc/releases/tag/3.0.0-RC9",
            List.of(new GitHubRelease.Asset("headlessmc.jar", "https://example.com/headlessmc.jar"))
        ));

        Path file = tempDir.resolve("updater").resolve("headlessmc.json");
        Instant before = Instant.now();
        factory().create(file, 1, TYPE).set(latestRelease);
        assertTrue(Files.exists(file));

        UpdateCheck<LatestRelease> check = factory().create(file, 1, TYPE).get().orElseThrow();
        assertEquals(latestRelease, check.value());
        assertFalse(Instant.ofEpochMilli(check.checkedAt()).isBefore(before.minusMillis(1)));
        assertEquals(Optional.empty(), factory().create(file, 2, TYPE).get());
    }

    @Test
    public void testMissingFile() {
        assertEquals(Optional.empty(), factory().create(tempDir.resolve("missing.json"), 1, TYPE).get());
    }

    private static UpdateCheckCacheFactory factory() {
        return new UpdateCheckCacheFactory(
            Mockito.mock(AppFiles.class),
            Mockito.mock(FileService.class),
            new DefaultJacksonJsonService()
        );
    }

    private static UpdateCheck<LatestRelease> check(Instant checkedAt) {
        return new UpdateCheck<>(checkedAt.toEpochMilli(), new LatestRelease("headlesshq/headlessmc", new GitHubRelease("3.0.0", "", List.of())));
    }

}
