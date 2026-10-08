package io.github.headlesshq.headlessmc.platform.fabric;

import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.platform.*;
import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.client.DefaultVersionMatcher;
import io.github.headlesshq.headlessmc.platform.client.VersionMatcher;
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

/**
 * Helps with constructing ({@link Produces}) Beans
 * required for the {@link Fabric} platform.
 * Some  Beans need a producer as they inherit Abstract base classes
 * with non-empty constructors and thus cannot be {@link ApplicationScoped}.
 */
@ApplicationScoped
class FabricFactory {
    @Produces
    @FabricBuildData
    @Dependent
    public URI getFabricBuildData() {
        return URI.create("https://meta.fabricmc.net/v2/versions/loader");
    }

    @Produces
    @Fabric
    @Dependent
    public MavenRepository createFabricMavenRepository() {
        return MavenRepository.of("https://maven.fabricmc.net");
    }

    @Produces
    @Fabric
    @Dependent
    public MavenRepository createLegacyFabricMavenRepository() {
        return MavenRepository.of("https://maven.legacyfabric.net");
    }

    @Produces
    @Fabric
    @Dependent
    public ClientSupport createClientSupport(@Fabric VersionMatcher matcher, @Fabric ClientInstaller installer) {
        return new ClientSupport(matcher, installer);
    }

    @Produces
    @Fabric
    @Dependent
    public ServerSupport createServerSupport(@Fabric ServerFinder finder, @Fabric ServerInstaller installer) {
        return new ServerSupport(finder, installer);
    }

    @Produces
    @Fabric
    @Dependent
    public ModSupport createModSupport(@Fabric Instance<ModReader> modReaders) {
        return new ModSupport(List.of(ModType.MOD), modReaders.stream().toList());
    }

    @Fabric
    @Produces
    @ApplicationScoped
    public VersionMatcher createVersionMatcher(
        VanillaVersionService vanillaVersionService,
        @Fabric VersionService versionService
    ) {
        return new DefaultVersionMatcher(vanillaVersionService, versionService);
    }

    @Fabric
    @Produces
    @ApplicationScoped
    VersionService createFabricVersionService(@Fabric Cache<List<BuildData>> cache) {
        return new FabricVersionService(cache);
    }

    // TODO: one problem is eduroam
    //  it blocks one of the IPs that are resolved for the fabric host name
    //  and the Java HttpClient never tries the other IPs
    //  this is a lack of a feature in Java HttpClient (and maybe also vert.x?)
    //  TODO: we could manually in JavaDownloadBuilder resolve the IPs on failure
    //   and try the other ones
    @Fabric
    @Produces
    @ApplicationScoped
    Cache<List<BuildData>> createBuildDataCache(
        PlatformCacheService platformCacheService,
        @FabricBuildData URI url
    ) {
        //noinspection Convert2Diamond
        return platformCacheService.versionCacheList(
            new TypeLiteral<List<BuildData>>() {},
            Fabric.PLATFORM_NAME,
            url,
            0L
        );
    }

}
