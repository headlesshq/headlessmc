package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.platform.ModFileReader;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformDownloader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Forge
@Getter
@Named("forge")
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ForgePlatform implements Platform {
    private final @Forge PlatformDownloader platformDownloader;
    private final @Forge ModFileReader modFileReader;

    @Override
    public String getName() {
        return "forge";
    }

    @Override
    public boolean supportsMods() {
        return true;
    }

}
