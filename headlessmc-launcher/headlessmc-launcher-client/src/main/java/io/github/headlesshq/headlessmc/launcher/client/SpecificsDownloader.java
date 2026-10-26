package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.launcher.LauncherConfig;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionPlatform;
import io.github.headlesshq.headlessmc.mods.github.GitHub;
import io.github.headlesshq.headlessmc.mods.github.HmcSpecificsRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Downloads hmc-specifics into the mods folder of a client before it launches,
 * if {@link LauncherConfig#autoDownloadSpecifics()} is enabled.
 * This never fails the launch, if hmc-specifics cannot be downloaded a warning is logged.
 */
@Slf4j
@ApplicationScoped
public class SpecificsDownloader {
    private final ModDistributionPlatform gitHub;
    private final LauncherConfig config;

    @Inject
    public SpecificsDownloader(@GitHub ModDistributionPlatform gitHub, LauncherConfig config) {
        this.gitHub = gitHub;
        this.config = config;
    }

    /**
     * Downloads hmc-specifics for the {@link Profile#currentVersion()} into the mods folder of the profile,
     * unless the mods folder already contains a version of hmc-specifics.
     *
     * @param profile the profile that is about to be launched.
     */
    public void download(Profile profile) {
        if (!config.autoDownloadSpecifics()) {
            return;
        }

        Path modsDir = ModType.MOD.directory().getDir(profile.path());
        if (containsSpecifics(modsDir)) {
            log.debug("{} already contains {}, not downloading it", modsDir, HmcSpecificsRepository.NAME);
            return;
        }

        try {
            Path file = gitHub.download(profile.path(), null, HmcSpecificsRepository.NAME, profile.currentVersion(), ModType.MOD);
            log.info("Downloaded {} to {}", HmcSpecificsRepository.NAME, file);
        } catch (HeadlessMcException e) {
            log.warn("Failed to download {} for {}: {}", HmcSpecificsRepository.NAME, profile.currentVersion(), e.getMessage());
            log.debug("Failed to download {}", HmcSpecificsRepository.NAME, e);
        }
    }

    private static boolean containsSpecifics(Path modsDir) {
        if (!Files.isDirectory(modsDir)) {
            return false;
        }

        try (Stream<Path> files = Files.list(modsDir)) {
            return files.map(file -> file.getFileName().toString())
                .anyMatch(name -> name.startsWith(HmcSpecificsRepository.NAME + "-") && name.endsWith(".jar"));
        } catch (IOException e) {
            log.warn("Failed to list {}", modsDir, e);
            return false;
        }
    }

}
