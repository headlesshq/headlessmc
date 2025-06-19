package io.github.headlesshq.headlessmc.platform.paper;

import io.github.headlesshq.headlessmc.platform.ModFileReader;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformDownloader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Paper
@Getter
@Named("paper")
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PaperPlatform implements Platform {
    private final @Paper PlatformDownloader platformDownloader;
    private final @Paper ModFileReader modFileReader;

    @Override
    public String getName() {
        return "paper";
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
