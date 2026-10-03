package io.github.headlesshq.headlessmc.platform.neoforge;

import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.files.cache.CacheBuilder;
import io.github.headlesshq.headlessmc.platform.BoundPlatformVersion;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.FakeVanillaVersionService;
import io.github.headlesshq.headlessmc.platform.FakeVersionService;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.PlatformVersion;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.client.DefaultVersionMatcher;
import io.github.headlesshq.headlessmc.platform.client.VersionMatcher;
import io.github.headlesshq.headlessmc.platform.forge.Forge;
import io.github.headlesshq.headlessmc.platform.forge.PrismIndex;
import io.github.headlesshq.headlessmc.util.maven.Artifact;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionProcessor;
import io.github.headlesshq.headlessmc.version.service.FakeVersionJsonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NeoForgeVersionsTest {
    private final List<PrismIndex.Meta> metas = new ArrayList<>();

    private FakeVanillaVersionService vanillaVersions;
    private PlatformService platformService;
    private VersionProcessor processor;

    @BeforeEach
    void setup() {
        vanillaVersions = new FakeVanillaVersionService("1.21.10", "1.20.4", "1.20.1");
        processor = new FakeVersionJsonService();
    }

    private static PrismIndex.Meta meta(String version, String mcVersion) {
        return new PrismIndex.Meta(
            List.of(new PrismIndex.Meta.Requires(mcVersion, "net.minecraft")), "sha", version
        );
    }

    private NeoForgeVersionService service() {
        Cache<PrismIndex> cache = CacheBuilder.<PrismIndex>create()
            .withSource(() -> Optional.of(new PrismIndex("net.neoforged", metas)))
            .build();
        return new NeoForgeVersionService(NeoForge.PLATFORM_NAME, cache);
    }

    @Test
    void theMcVersionPrefixIsStrippedFromBuildNames() {
        metas.add(meta("21.10.62-beta", "1.21.10"));

        BoundPlatformVersion version = service().getVersions().getFirst();

        assertEquals("62-beta", version.getName());
        assertEquals("1.21.10", version.getVanillaVersion());
    }

    @Test
    void theShortenedMcVersionPrefixIsAlsoStripped() {
        // the neoforge version 20.4.195 belongs to mc 1.20.4
        metas.add(meta("20.4.195", "1.20.4"));

        assertEquals("195", service().getVersions().getFirst().getName());
    }

    @Test
    void unrelatedVersionNamesAreKeptAsIs() {
        metas.add(meta("47.1.104", "1.20.1"));

        assertEquals("47.1.104", service().getVersions().getFirst().getName());
    }

    @Test
    void legacyArtifactsUseTheForgeName() {
        PrismIndex index = new PrismIndex("net.neoforged", List.of(meta("47.1.104", "1.20.1")));

        Artifact artifact = new NeoForgeArtifactResolver()
            .resolve(NeoForge.PLATFORM_NAME, index, index.versions().getFirst(), "installer");

        assertEquals(new Artifact("net.neoforged", Forge.PLATFORM_NAME, "1.20.1-47.1.104", "installer"), artifact);
    }

    @Test
    void modernArtifactsUseTheNeoforgeNameAndVersion() {
        PrismIndex index = new PrismIndex("net.neoforged", List.of(meta("20.4.195", "1.20.4")));

        Artifact artifact = new NeoForgeArtifactResolver()
            .resolve(NeoForge.PLATFORM_NAME, index, index.versions().getFirst(), "installer");

        assertEquals(new Artifact("net.neoforged", NeoForge.PLATFORM_NAME, "20.4.195", "installer"), artifact);
    }

    // ------------------------------------------------- NeoForgeVersionMatcher

    private NeoForgeVersionMatcher matcher(FakeVersionService versions) {
        FakePlatform neoforge = new FakePlatform(NeoForge.PLATFORM_NAME, versions);
        platformService = new FakePlatformService(
            new FakeVanillaPlatform("1.21.10", "1.20.4", "1.20.1"), neoforge
        );
        return new NeoForgeVersionMatcher(
            new DefaultVersionMatcher(vanillaVersions, versions), NeoForge.PLATFORM_NAME
        );
    }

    @Test
    void platformNameAndDelegateAreExposed() {
        NeoForgeVersionMatcher matcher = matcher(new FakeVersionService(NeoForge.PLATFORM_NAME));

        assertEquals(NeoForge.PLATFORM_NAME, matcher.getPlatformName());
        assertInstanceOf(VersionMatcher.class, matcher.getDefaultVersionMatcher());
    }

    @Test
    void legacyForgeStyleVersionsAreMatchedIfTheyResolve() {
        FakeVersionService versions = new FakeVersionService(NeoForge.PLATFORM_NAME)
            .withBuilds("1.20.1", "47.1.104");
        NeoForgeVersionMatcher matcher = matcher(versions);
        Version version = new FakeVersion("1.20.1-forge-47.1.104");

        assertTrue(matcher.canMatch(platformService, version, processor));

        VersionID id = matcher.match(platformService, version, processor);
        assertEquals("1.20.1", id.getVersion().getName());
        assertEquals("47.1.104", id.getBuild().orElseThrow().getName());
    }

    @Test
    void legacyForgeStyleVersionsOfOtherPlatformsAreNotMatched() {
        NeoForgeVersionMatcher matcher = matcher(new FakeVersionService(NeoForge.PLATFORM_NAME));

        assertFalse(matcher.canMatch(platformService, new FakeVersion("1.20.1-forge-47.1.104"), processor));
    }

    @Test
    void otherVersionsUseTheDefaultMatcher() {
        FakeVersionService versions = new FakeVersionService(NeoForge.PLATFORM_NAME)
            .withBuilds("1.21.10", "62-beta");
        NeoForgeVersionMatcher matcher = matcher(versions);

        assertTrue(matcher.canMatch(platformService, new FakeVersion("neoforge-62-beta"), processor));
        assertFalse(matcher.canMatch(platformService, new FakeVersion("1.21.10"), processor));
    }

    @Test
    void versionsAreSortedByTheirIndexOrder() {
        metas.add(meta("21.10.62-beta", "1.21.10"));
        metas.add(meta("20.4.195", "1.20.4"));
        NeoForgeVersionService service = service();

        List<PlatformVersion> shuffled = new ArrayList<>(List.of(
            service.getVersions().getLast(), service.getVersions().getFirst()
        ));

        assertEquals(
            List.of("62-beta", "195"),
            service.sort(shuffled).stream().map(PlatformVersion::getName).toList()
        );
    }

}
