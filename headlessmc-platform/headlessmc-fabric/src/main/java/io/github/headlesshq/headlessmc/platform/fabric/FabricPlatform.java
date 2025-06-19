package io.github.headlesshq.headlessmc.platform.fabric;

import io.github.headlesshq.headlessmc.platform.ModFileReader;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformDownloader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Fabric
@Getter
@Named("fabric")
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class FabricPlatform implements Platform {
    private final @Fabric PlatformDownloader platformDownloader;
    private final @Fabric ModFileReader modFileReader;

    @Override
    public String getName() {
        return "fabric";
    }

    @Override
    public boolean supportsMods() {
        return true;
    }

}
