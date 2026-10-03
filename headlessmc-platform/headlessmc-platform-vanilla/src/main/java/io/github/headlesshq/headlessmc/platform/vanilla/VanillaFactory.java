package io.github.headlesshq.headlessmc.platform.vanilla;

import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.platform.Vanilla;
import io.github.headlesshq.headlessmc.platform.VanillaVersionService;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.client.VersionMatcher;
import io.github.headlesshq.headlessmc.platform.server.ServerFinder;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import io.github.headlesshq.headlessmc.platform.server.ServerSupport;
import io.github.headlesshq.headlessmc.platform.util.PlatformCacheService;
import io.github.headlesshq.headlessmc.util.maven.MavenRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.util.TypeLiteral;

import java.net.URI;

/**
 * Helps with constructing ({@link Produces}) Beans
 * required for the {@link Vanilla} platform.
 * Some  Beans need a producer as they inherit Abstract base classes
 * with non-empty constructors and thus cannot be {@link ApplicationScoped}.
 */
@ApplicationScoped
public class VanillaFactory {
    @Produces
    @Vanilla
    @Dependent
    public MavenRepository createMavenRepository() {
        return MavenRepository.of("https://libraries.minecraft.net");
    }

    @Produces
    @Dependent
    @VersionManifest
    public URI createVersionManifest() {
        return URI.create("https://launchermeta.mojang.com/mc/game/version_manifest.json");
    }

    @Vanilla
    @Produces
    @ApplicationScoped
    public VersionMatcher createVersionMatcher(VanillaVersionService vanillaVersionService) {
        return new VanillaVersionMatcher(vanillaVersionService);
    }

    @Produces
    @Vanilla
    @Dependent
    public ClientSupport createClientSupport(@Vanilla VersionMatcher matcher, @Vanilla ClientInstaller installer) {
        return new ClientSupport(matcher, installer);
    }

    @Produces
    @Vanilla
    @Dependent
    public ServerSupport createServerSupport(@Vanilla ServerFinder finder, @Vanilla ServerInstaller installer) {
        return new ServerSupport(finder, installer);
    }

    @Vanilla
    @Default
    @Produces
    @ApplicationScoped
    VanillaVersionService createVersionService(@Vanilla Cache<VanillaManifest> cache) {
        return new VanillaVersionServiceImpl(cache);
    }

    @Vanilla
    @Produces
    @ApplicationScoped
    Cache<VanillaManifest> createBuildDataCache(
        PlatformCacheService platformCacheService,
        @VersionManifest URI url
    ) {
        //noinspection Convert2Diamond
        return platformCacheService.versionCache(
            new TypeLiteral<VanillaManifest>() {},
            Vanilla.PLATFORM_NAME,
            url
        );
    }

}
