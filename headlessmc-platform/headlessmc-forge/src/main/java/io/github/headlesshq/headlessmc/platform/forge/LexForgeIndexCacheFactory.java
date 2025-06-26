package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.commons.DownloadService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import java.net.URI;

import static java.net.URI.create;

@ApplicationScoped
public class LexForgeIndexCacheFactory {
    private final URI lexForgeIndices = create("https://meta.prismlauncher.org/v1/net.minecraftforge/index.json");

    @Forge
    @Produces
    @Named("forge")
    @ApplicationScoped
    public ForgeIndexCache lexForgeIndexCache(DownloadService downloadService) {
        return new ForgeIndexCache(downloadService, lexForgeIndices);
    }

}
