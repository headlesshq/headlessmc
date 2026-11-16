package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.launcher.LauncherConfig;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionPlatform;
import io.github.headlesshq.headlessmc.mods.github.GitHub;
import io.github.headlesshq.headlessmc.mods.github.HmcSpecificsRepository;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.Nullable;

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
     * Makes sure that the mods folder of the profile contains the latest hmc-specifics for the given version.
     *
     * @param profile the profile that is about to be launched.
     * @param version the version the profile launches, {@code null} if it is not known,
     *                e.g. for a version.json no platform recognizes. Nothing is downloaded then.
     */
    public void download(Profile profile, @Nullable VersionArg version) {
        if (!config.autoDownloadSpecifics()) {
            return;
        }

        if (version == null) {
            log.debug("Version of profile {} is unknown, not downloading {}", profile.name(), HmcSpecificsRepository.NAME);
            return;
        }

        if (!specifics.supports(version)) {
            log.debug("{} is not available for {}", HmcSpecificsRepository.NAME, version);
            return;
        }

        try {
            Path file = gitHub.download(profile.path(), null, HmcSpecificsRepository.NAME, version, ModType.MOD);
            log.debug("{} is installed at {}", HmcSpecificsRepository.NAME, file);
        } catch (HeadlessMcException e) {
            log.warn("Failed to download {} for {}: {}", HmcSpecificsRepository.NAME, version, e.getMessage());
            log.debug("Failed to download {}", HmcSpecificsRepository.NAME, e);
        }
    }

}
