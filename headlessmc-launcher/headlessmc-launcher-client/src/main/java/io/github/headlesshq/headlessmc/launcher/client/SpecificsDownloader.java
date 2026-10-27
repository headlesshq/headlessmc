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

import java.nio.file.Path;

/**
 * Keeps the latest hmc-specifics in the mods folder of a client before it launches,
 * if {@link LauncherConfig#autoDownloadSpecifics()} is enabled.
 * Installed versions of hmc-specifics are detected by their mod id and replaced
 * if they differ from the latest release, see {@link io.github.headlesshq.headlessmc.mods.github.GitHubDistributionPlatform}.
 * This never fails the launch, if hmc-specifics cannot be downloaded a warning is logged.
 */
@Slf4j
@ApplicationScoped
public class SpecificsDownloader {
    private final ModDistributionPlatform gitHub;
    private final HmcSpecificsRepository specifics;
    private final LauncherConfig config;

    @Inject
    public SpecificsDownloader(
        @GitHub ModDistributionPlatform gitHub,
        HmcSpecificsRepository specifics,
        LauncherConfig config
    ) {
        this.gitHub = gitHub;
        this.specifics = specifics;
        this.config = config;
    }

    /**
     * Makes sure that the mods folder of the profile contains the latest hmc-specifics
     * for the {@link Profile#currentVersion()}.
     *
     * @param profile the profile that is about to be launched.
     */
    public void download(Profile profile) {
        if (!config.autoDownloadSpecifics()) {
            return;
        }

        if (!specifics.supports(profile.currentVersion())) {
            log.debug("{} is not available for {}", HmcSpecificsRepository.NAME, profile.currentVersion());
            return;
        }

        try {
            Path file = gitHub.download(profile.path(), null, HmcSpecificsRepository.NAME, profile.currentVersion(), ModType.MOD);
            log.debug("{} is installed at {}", HmcSpecificsRepository.NAME, file);
        } catch (HeadlessMcException e) {
            log.warn("Failed to download {} for {}: {}", HmcSpecificsRepository.NAME, profile.currentVersion(), e.getMessage());
            log.debug("Failed to download {}", HmcSpecificsRepository.NAME, e);
        }
    }

}
