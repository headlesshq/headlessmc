package io.github.headlesshq.headlessmc.quilt;

import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.platform.VanillaVersionService;
import io.github.headlesshq.headlessmc.platform.VersionService;
import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.client.DefaultVersionMatcher;
import io.github.headlesshq.headlessmc.platform.client.VersionMatcher;
import io.github.headlesshq.headlessmc.platform.fabric.Fabric;
import io.github.headlesshq.headlessmc.platform.mods.ModReader;
import io.github.headlesshq.headlessmc.platform.mods.ModSupport;
import io.github.headlesshq.headlessmc.platform.server.ServerFinder;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import io.github.headlesshq.headlessmc.platform.server.ServerSupport;
import io.github.headlesshq.headlessmc.platform.util.PlatformCacheService;
import io.github.headlesshq.headlessmc.util.maven.MavenRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.util.TypeLiteral;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

/**
 * Produces the Beans of the {@link Quilt} platform, like the FabricFactory of HeadlessMc does for Fabric.
 */
@ApplicationScoped
class QuiltFactory {
    /**
     * Lists all Quilt loader versions, newest first.
     */
    static final URI LOADER_VERSIONS = URI.create("https://meta.quiltmc.org/v3/versions/loader");

    @Produces
    @Quilt
    @Dependent
    public MavenRepository createQuiltMavenRepository() {
        return MavenRepository.of("https://maven.quiltmc.org/repository/release");
    }

    @Produces
    @Quilt
    @Dependent
    public MavenRepository createFabricMavenRepository() {
        // Quilt versions depend on Fabric's intermediary mappings
        return MavenRepository.of("https://maven.fabricmc.net");
    }

    @Produces
    @Quilt
    @Dependent
    public ClientSupport createClientSupport(@Quilt VersionMatcher matcher, @Quilt ClientInstaller installer) {
        return new ClientSupport(matcher, installer);
    }

    @Produces
    @Quilt
    @Dependent
    public ServerSupport createServerSupport(@Quilt ServerFinder finder, @Quilt ServerInstaller installer) {
        return new ServerSupport(finder, installer);
    }

    @Produces
    @Quilt
    @Dependent
    public ModSupport createModSupport(@Quilt Instance<ModReader> quiltReaders, @Fabric Instance<ModReader> fabricReaders) {
        // Quilt loads Fabric mods, too
        return new ModSupport(List.of(ModType.MOD), Stream.concat(quiltReaders.stream(), fabricReaders.stream()).toList());
    }

    @Quilt
    @Produces
    @ApplicationScoped
    public VersionMatcher createVersionMatcher(
        VanillaVersionService vanillaVersionService,
        @Quilt VersionService versionService
    ) {
        return new DefaultVersionMatcher(vanillaVersionService, versionService);
    }

    @Quilt
    @Produces
    @ApplicationScoped
    VersionService createQuiltVersionService(@Quilt Cache<List<QuiltBuildData>> cache) {
        return new QuiltVersionService(cache);
    }

    @Quilt
    @Produces
    @ApplicationScoped
    Cache<List<QuiltBuildData>> createBuildDataCache(PlatformCacheService platformCacheService) {
        //noinspection Convert2Diamond
        return platformCacheService.versionCacheList(
            new TypeLiteral<List<QuiltBuildData>>() {},
            Quilt.PLATFORM_NAME,
            LOADER_VERSIONS,
            0L
        );
    }

}
