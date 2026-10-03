package io.github.headlesshq.headlessmc.platform.neoforge;

import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.platform.mods.ModReader;
import io.github.headlesshq.headlessmc.platform.VersionService;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.client.VersionMatcher;
import io.github.headlesshq.headlessmc.platform.forge.*;
import io.github.headlesshq.headlessmc.platform.mods.ModSupport;
import io.github.headlesshq.headlessmc.platform.server.ServerFinder;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import io.github.headlesshq.headlessmc.platform.server.ServerSupport;
import io.github.headlesshq.headlessmc.platform.util.CommonInstallerServices;
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
 * required for the {@link NeoForge} platform.
 * Some Beans need a producer as they inherit Abstract base classes
 * with non-empty constructors and thus cannot be {@link ApplicationScoped}.
 */
@ApplicationScoped
public class NeoForgeFactory {
    @Produces
    @Dependent
    @NeoForgePrismIndex
    public URI createPrismIndex() {
        return URI.create("https://meta.prismlauncher.org/v1/net.neoforged/index.json");
    }

    @NeoForge
    @Produces
    @Dependent
    public MavenRepository createMavenRepository() {
        // also snapshots? MavenRepository.of("https://maven.neoforged.net/snapshots");
        return MavenRepository.of("https://maven.neoforged.net/releases");
    }

    @NeoForge
    @Produces
    @ApplicationScoped
    public ServerFinder createServerFinder(OS os) {
        return new ForgeServerFinder(NeoForge.PLATFORM_NAME, os);
    }

    @Produces
    @NeoForge
    @Dependent
    public ClientSupport createClientSupport(@NeoForge VersionMatcher matcher, @NeoForge ClientInstaller installer) {
        return new ClientSupport(matcher, installer);
    }

    @Produces
    @NeoForge
    @Dependent
    public ServerSupport createServerSupport(@NeoForge ServerFinder finder, @NeoForge ServerInstaller installer) {
        return new ServerSupport(finder, installer);
    }

    @Produces
    @NeoForge
    @Dependent
    public ModSupport createModSupport(@NeoForge Instance<ModReader> modReaders) {
        return new ModSupport(List.of(ModType.MOD), modReaders.stream().toList());
    }

    @NeoForge
    @Produces
    @ApplicationScoped
    VersionService createVersionService(@NeoForge Cache<PrismIndex> cache) {
        return new NeoForgeVersionService(NeoForge.PLATFORM_NAME, cache);
    }

    @NeoForge
    @Produces
    @ApplicationScoped
    Cache<PrismIndex> createPrismIndexCache(
        PlatformCacheService platformCacheService,
        @NeoForgePrismIndex URI url
    ) {
        //noinspection Convert2Diamond
        return platformCacheService.versionCache(
            new TypeLiteral<PrismIndex>() {},
            NeoForge.PLATFORM_NAME,
            url
        );
    }

    @NeoForge
    @Produces
    @ApplicationScoped
    public ForgeInstallerDownloader createInstallerDownloader(
        @NeoForge ForgeArtifactResolver artifactResolver,
        CommonInstallerServices services,
        @NeoForge MavenRepository repository,
        @NeoForge Cache<PrismIndex> cache
    ) {
        return new ForgeInstallerDownloader(artifactResolver, services, repository, cache, NeoForge.PLATFORM_NAME);
    }

    @NeoForge
    @Produces
    @ApplicationScoped
    public ForgeInstaller createInstaller(
        @NeoForge ForgeInstallerDownloader installerDownloader,
        CommonInstallerServices services,
        ForgeCLIInstaller cliInstaller,
        @NeoForge Cache<PrismIndex> cache
    ) {
        return new ForgeInstaller(installerDownloader, services, cliInstaller, cache, NeoForge.PLATFORM_NAME);
    }

}
