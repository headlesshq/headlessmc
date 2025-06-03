package io.github.headlesshq.headlessmc.platform.vanilla;

import io.github.headlesshq.headlessmc.platform.ModFileReader;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformDownloader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

/**
 * Represents the raw versions of Mc supplied by Mojang, e.g. 1.12.2, 1.21.6, etc.
 * Mod files are not supported.
 */
@Vanilla
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class VanillaPlatform implements Platform {
    private final @Vanilla PlatformDownloader platformDownloader;
    private final @Vanilla ModFileReader modFileReader;

    @Override
    public String getName() {
        return "vanilla";
    }

    @Override
    public ModFileReader getModFileReader() {
        return modFileReader;
    }

    @Override
    public PlatformDownloader getPlatformDownloader() {
        return platformDownloader;
    }

    @Override
    public boolean supportsMods() {
        return false;
    }

}
