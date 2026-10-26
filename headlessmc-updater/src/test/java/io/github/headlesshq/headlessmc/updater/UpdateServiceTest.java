package io.github.headlesshq.headlessmc.updater;

import io.github.headlesshq.headlessmc.HeadlessMc;
import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.os.OSService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusComponentTest(UpdateService.class)
public class UpdateServiceTest {
    private static final String REPOSITORY = "headlesshq/headlessmc";
    private static final GitHubRelease NEWER = release("0.0.0-test");
    private static final GitHubRelease CURRENT = release(HeadlessMc.VERSION);

    @Inject
    UpdateService updateService;

    @InjectMock
    Holder<UpdaterConfig> config;

    @InjectMock
    GitHubReleaseClient client;

    @InjectMock
    UpdateCacheService cache;

    @InjectMock
    OSService osService;

    private UpdaterConfig updaterConfig;

    @BeforeEach
    public void setup() {
        updaterConfig = Mockito.mock(UpdaterConfig.class);
        when(updaterConfig.enabled()).thenReturn(true);
        when(updaterConfig.repository()).thenReturn(REPOSITORY);
        when(updaterConfig.intervalHours()).thenReturn(24);
        when(config.get()).thenReturn(updaterConfig);
        when(osService.getOS()).thenReturn(new OS("Linux", OS.Type.LINUX, "6.0"));
        when(osService.getCPU()).thenReturn(CPU.X64);
    }

    @Test
    public void testCheckForUpdateFetchesAndCaches() {
        when(cache.get()).thenReturn(Optional.of(cached(REPOSITORY, Duration.ZERO, CURRENT)));
        when(client.getLatestRelease(updaterConfig)).thenReturn(Optional.of(NEWER));

        assertEquals(Optional.of(NEWER), updateService.checkForUpdate().map(Update::release));
        verify(cache).set(argThat(entry -> entry.release().equals(NEWER) && entry.repository().equals(REPOSITORY)));
    }

    @Test
    public void testCheckForUpdateCachesWithoutUpdate() {
        when(client.getLatestRelease(updaterConfig)).thenReturn(Optional.of(CURRENT));

        assertEquals(Optional.empty(), updateService.checkForUpdate());
        verify(cache).set(argThat(entry -> entry.release().equals(CURRENT)));
    }

    @Test
    public void testCheckForUpdateFallsBackToCache() {
        when(client.getLatestRelease(updaterConfig)).thenReturn(Optional.empty());
        when(cache.get()).thenReturn(Optional.of(cached(REPOSITORY, Duration.ofDays(3), NEWER)));

        assertEquals(Optional.of(NEWER), updateService.checkForUpdate().map(Update::release));
        verify(cache, never()).set(any());
    }

    @Test
    public void testFreshCacheIsNotFetchedAgain() {
        when(cache.get()).thenReturn(Optional.of(cached(REPOSITORY, Duration.ofHours(1), NEWER)));

        assertEquals(Optional.of(NEWER), updateService.checkForUpdateIfStale().map(Update::release));
        verifyNoInteractions(client);
    }

    @Test
    public void testFreshCacheWithoutUpdate() {
        when(cache.get()).thenReturn(Optional.of(cached(REPOSITORY, Duration.ofHours(1), CURRENT)));

        assertEquals(Optional.empty(), updateService.checkForUpdateIfStale());
        verifyNoInteractions(client);
    }

    @Test
    public void testStaleCacheIsFetchedAgain() {
        when(cache.get()).thenReturn(Optional.of(cached(REPOSITORY, Duration.ofHours(25), CURRENT)));
        when(client.getLatestRelease(updaterConfig)).thenReturn(Optional.of(NEWER));

        assertEquals(Optional.of(NEWER), updateService.checkForUpdateIfStale().map(Update::release));
        verify(client).getLatestRelease(updaterConfig);
    }

    @Test
    public void testMissingCacheIsFetched() {
        when(cache.get()).thenReturn(Optional.empty());
        when(client.getLatestRelease(updaterConfig)).thenReturn(Optional.of(NEWER));

        assertEquals(Optional.of(NEWER), updateService.checkForUpdateIfStale().map(Update::release));
    }

    @Test
    public void testCacheOfOtherRepositoryIsIgnored() {
        when(cache.get()).thenReturn(Optional.of(cached("someone/fork", Duration.ofHours(1), NEWER)));
        when(client.getLatestRelease(updaterConfig)).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), updateService.checkForUpdateIfStale());
        verify(client).getLatestRelease(updaterConfig);
    }

    @Test
    public void testDisabled() {
        when(updaterConfig.enabled()).thenReturn(false);

        assertEquals(Optional.empty(), updateService.checkForUpdate());
        assertEquals(Optional.empty(), updateService.checkForUpdateIfStale());
        verifyNoInteractions(client, cache);
    }

    @Test
    public void testIsUpdate() {
        assertTrue(updateService.isUpdate("3.0.0-RC8", release("3.0.0-RC9")));
        assertTrue(updateService.isUpdate("3.0.0-RC8", release("3.0.0-RC7")));
        assertFalse(updateService.isUpdate("3.0.0-RC8", release("3.0.0-RC8")));
        assertFalse(updateService.isUpdate("3.0.0-RC8", release(" 3.0.0-rc8 ")));
    }

    @Test
    public void testGetAssetName() {
        assertEquals("headlessmc.jar", updateService.getAssetName(Installation.Type.JAR, OS.Type.WINDOWS, CPU.X64));
        assertEquals("headlessmc-launcher-linux-x64", updateService.getAssetName(Installation.Type.NATIVE, OS.Type.LINUX, CPU.X64));
        assertEquals("headlessmc-launcher-linux-arm64", updateService.getAssetName(Installation.Type.NATIVE, OS.Type.LINUX, CPU.AARCH64));
        assertEquals("headlessmc-launcher-macos-arm64", updateService.getAssetName(Installation.Type.NATIVE, OS.Type.MACOS, CPU.AARCH64));
        assertEquals("headlessmc-launcher-macos-x64", updateService.getAssetName(Installation.Type.NATIVE, OS.Type.MACOS, CPU.X64));
        assertEquals("headlessmc-launcher-windows-x64.exe", updateService.getAssetName(Installation.Type.NATIVE, OS.Type.WINDOWS, CPU.X64));
        assertNull(updateService.getAssetName(Installation.Type.NATIVE, OS.Type.FREE_BSD, CPU.X64));
        assertNull(updateService.getAssetName(Installation.Type.NATIVE, OS.Type.LINUX, CPU.X32));
        assertNull(updateService.getAssetName(Installation.Type.UNKNOWN, OS.Type.LINUX, CPU.X64));
    }

    private static UpdateCache cached(String repository, Duration age, GitHubRelease release) {
        return new UpdateCache(repository, Instant.now().minus(age).toEpochMilli(), release);
    }

    private static GitHubRelease release(String tag) {
        return new GitHubRelease(tag, "https://github.com/headlesshq/headlessmc/releases/tag/" + tag, List.of());
    }

}
