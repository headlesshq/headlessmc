package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.launcher.LauncherConfig;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionException;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionPlatform;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

class SpecificsDownloaderTest {
    private static final VersionArg VERSION = VersionArg.parse("fabric", "1.21.1");

    @TempDir
    Path gameDir;

    private final ModDistributionPlatform gitHub = Mockito.mock(ModDistributionPlatform.class);
    private final LauncherConfig config = Mockito.mock(LauncherConfig.class);
    private final SpecificsDownloader downloader = new SpecificsDownloader(gitHub, config);

    private Profile profile() {
        return new Profile("test", VERSION, gameDir);
    }

    @Test
    void doesNothingIfDisabled() {
        downloader.download(profile());

        Mockito.verifyNoInteractions(gitHub);
    }

    @Test
    void downloadsSpecificsForTheLaunchedVersion() throws Exception {
        Mockito.when(config.autoDownloadSpecifics()).thenReturn(true);

        downloader.download(profile());

        Mockito.verify(gitHub).download(eq(gameDir), isNull(), eq("hmc-specifics"), eq(VERSION), eq(ModType.MOD));
    }

    @Test
    void doesNotDownloadIfTheModsFolderAlreadyContainsSpecifics() throws Exception {
        Mockito.when(config.autoDownloadSpecifics()).thenReturn(true);
        Path mods = Files.createDirectories(gameDir.resolve("mods"));
        Files.createFile(mods.resolve("hmc-specifics-1.21.1-fabric-3.0.1.jar"));

        downloader.download(profile());

        Mockito.verifyNoInteractions(gitHub);
    }

    @Test
    void doesNotFailIfSpecificsIsNotAvailable() throws Exception {
        Mockito.when(config.autoDownloadSpecifics()).thenReturn(true);
        Mockito.when(gitHub.download(any(), any(), any(), any(), any()))
            .thenThrow(new ModDistributionException("Failed to get release 1.0-latest of headlesshq/hmc-specifics"));

        assertDoesNotThrow(() -> downloader.download(profile()));
    }

}
