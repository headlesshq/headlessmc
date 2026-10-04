package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.files.cache.CacheBuilder;
import io.github.headlesshq.headlessmc.platform.BoundPlatformVersion;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.PlatformVersion;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;

import static org.junit.jupiter.api.Assertions.*;

class ForgeVersionServiceTest {
    private final List<PrismIndex.Meta> metas = new ArrayList<>();

    private ForgeVersionService service;

    @BeforeEach
    void setup() {
        metas.add(meta("48.1.0", "1.20.2"));
        metas.add(meta("48.0.0", "1.20.2"));
        metas.add(meta("11.15.1.2318", "1.8.9"));

        Cache<PrismIndex> cache = CacheBuilder.<PrismIndex>create()
            .withSource(() -> Optional.of(new PrismIndex("net.minecraftforge", metas)))
            .build();
        service = new ForgeVersionService(Forge.PLATFORM_NAME, cache);
    }

    private static PrismIndex.Meta meta(String version, String mcVersion) {
        return new PrismIndex.Meta(
            List.of(new PrismIndex.Meta.Requires(mcVersion, "net.minecraft")), "sha", version
        );
    }

    private VanillaVersion vanilla(String name) {
        return new FakeVanillaPlatform(name).getVersionService().getVersion(name).orElseThrow();
    }

    @Test
    void mapsEveryIndexEntryToABoundVersion() {
        SequencedSet<BoundPlatformVersion> versions = service.getVersions();

        assertEquals(
            List.of("48.1.0", "48.0.0", "11.15.1.2318"),
            versions.stream().map(PlatformVersion::getName).toList()
        );
        assertEquals("1.20.2", versions.getFirst().getVanillaVersion());
        assertEquals(Forge.PLATFORM_NAME, service.getPlatformName());
    }

    @Test
    void filtersBuildsByMcVersion() {
        assertEquals(
            List.of("48.1.0", "48.0.0"),
            service.getBuilds(vanilla("1.20.2")).stream().map(PlatformVersion::getName).toList()
        );
        assertEquals(List.of(), service.getBuilds(vanilla("1.7.10")).stream().toList());
    }

    @Test
    void resolvesLatestAndSpecificBuilds() {
        assertEquals("48.1.0", service.getLatestBuild(vanilla("1.20.2")).orElseThrow().getName());
        assertEquals("48.0.0", service.getBuild(vanilla("1.20.2"), "48.0.0").orElseThrow().getName());
        assertEquals(Optional.empty(), service.getBuild(vanilla("1.20.2"), "9.9.9"));
    }

    @Test
    void sortsVersionsByTheirIndexOrder() {
        List<PlatformVersion> shuffled = new ArrayList<>(List.of(
            service.getBuild(vanilla("1.8.9"), "11.15.1.2318").orElseThrow(),
            service.getBuild(vanilla("1.20.2"), "48.1.0").orElseThrow()
        ));

        assertEquals(
            List.of("48.1.0", "11.15.1.2318"),
            service.sort(shuffled).stream().map(PlatformVersion::getName).toList()
        );
    }

    @Test
    void theCacheIsExposedToThePackage() {
        assertTrue(service.getCache().get().isPresent());
    }

}
