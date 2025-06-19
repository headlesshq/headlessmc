package io.github.headlesshq.headlessmc.platform.purpur;

import io.github.headlesshq.headlessmc.platform.ModFileReader;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformDownloader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Purpur
@Getter
@Named("purpur")
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PurpurPlatform implements Platform {
    private final @Purpur PlatformDownloader platformDownloader;
    private final @Purpur ModFileReader modFileReader;

    @Override
    public String getName() {
        return "purpur";
    }

    @Override
    public boolean supportsMods() {
        return true;
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

}
