package io.github.headlesshq.headlessmc.platform.forge;

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
 * required for the {@link Forge} platform.
 * Some  Beans need a producer as they inherit Abstract base classes
 * with non-empty constructors and thus cannot be {@link ApplicationScoped}.
 */
@ApplicationScoped
public class ForgeFactory {
    @Produces
    @ForgePrismIndex
    @Dependent
    public URI createPrismIndex() {
        // TODO: mirror?
        return URI.create("https://meta.prismlauncher.org/v1/net.minecraftforge/index.json");
    }

    @Forge
    @Produces
    @Dependent
    public MavenRepository createMavenRepository() {
        return MavenRepository.of("https://maven.minecraftforge.net");
    }

    @Forge
    @Produces
    @ApplicationScoped
    public VersionMatcher createVersionMatcher(
        VanillaVersionService vanillaVersionService,
        @Forge VersionService versionService
    ) {
        return new DefaultVersionMatcher(vanillaVersionService, versionService);
    }

    @Produces
    @Forge
    @Dependent
    public ClientSupport createClientSupport(@Forge VersionMatcher matcher, @Forge ClientInstaller installer) {
        return new ClientSupport(matcher, installer);
    }

    @Produces
    @Forge
    @Dependent
    public ServerSupport createServerSupport(@Forge ServerFinder finder, @Forge ServerInstaller installer) {
        return new ServerSupport(finder, installer);
    }

    @Produces
    @Forge
    @Dependent
    public ModSupport createModSupport(@Forge Instance<ModReader> modReaders) {
        return new ModSupport(List.of(ModType.MOD), modReaders.stream().toList());
    }

    @Forge
    @Produces
    @ApplicationScoped
    VersionService createVersionService(@Forge Cache<PrismIndex> cache) {
        return new ForgeVersionService(Forge.PLATFORM_NAME, cache);
    }

    @Forge
    @Produces
    @ApplicationScoped
    Cache<PrismIndex> createPrismIndexCache(
        PlatformCacheService platformCacheService,
        @ForgePrismIndex URI url
    ) {
        //noinspection Convert2Diamond
        return platformCacheService.versionCache(
            new TypeLiteral<PrismIndex>() {},
            Forge.PLATFORM_NAME,
            url
        );
    }

}
