package io.github.headlesshq.headlessmc.platform.neoforge;

import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.files.cache.CacheBuilder;
import io.github.headlesshq.headlessmc.platform.BundledVersions;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.FakeVanillaVersionService;
import io.github.headlesshq.headlessmc.platform.FakeVersionService;
import io.github.headlesshq.headlessmc.platform.McVersions;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.VersionService;
import io.github.headlesshq.headlessmc.platform.client.VersionMatchException;
import io.github.headlesshq.headlessmc.platform.forge.PrismIndex;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionProcessor;
import io.github.headlesshq.headlessmc.version.service.FakeVersionJsonService;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NeoForgeVersionMatcherTest {
    private static final String NEOFORGE_VERSION = "--fml.neoForgeVersion";
    private static final String FORGE_VERSION = "--fml.forgeVersion";

    static NeoForgeVersionService bundledVersionService() {
        PrismIndex index = BundledVersions.cacheObject("neoforge/neoforge-versions.json", PrismIndex.class);
        Cache<PrismIndex> cache = CacheBuilder.<PrismIndex>create().withSource(() -> Optional.of(index)).build();
        return new NeoForgeVersionService(NeoForge.PLATFORM_NAME, cache);
    }

    private static Matcher matcher(FakeVanillaVersionService vanillaVersionService, VersionService versionService) {
        PlatformService platformService = new FakePlatformService(
            new FakeVanillaPlatform(vanillaVersionService),
            new FakePlatform(NeoForge.PLATFORM_NAME, versionService)
        );

        return new Matcher(new NeoForgeVersionMatcher(vanillaVersionService, versionService), platformService);
    }

    private final Matcher matcher = matcher(BundledVersions.vanillaVersionService(), bundledVersionService());

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
        neoforge-21.1.240                    | 1.21.1          | 240
        neoforge-21.10.64                    | 1.21.10         | 64
        neoforge-21.0.167                    | 1.21            | 0.167
        neoforge-20.2.3-beta                 | 1.20.2          | 3-beta
        neoforge-26.2.0.25-beta              | 26.2            | 0.25-beta
        neoforge-26.1.2.82                   | 26.1.2          | 82
        neoforge-20.2.12-beta                | 1.20.2          | 12-beta
        neoforge-20.6.21-beta                | 1.20.6          | 21-beta
        neoforge-20.6.1-beta                 | 1.20.6          | 1-beta
        neoforge-26.1.0.1-beta               | 26.1            | 0.1-beta
        neoforge-26.1.2.71                   | 26.1.2          | 71
        neoforge-0.25w14craftmine.3-beta     | 25w14craftmine  | 0.25w14craftmine.3-beta
        neoforge-26.1.0.0-alpha.1+snapshot-1 | 26.1-snapshot-1 | 26.1.0.0-alpha.1+snapshot-1
        neoforge-26.1.0.0-alpha.15+pre-3     | 26.1-pre-3      | 26.1.0.0-alpha.15+pre-3
        1.20.1-forge-47.1.100                | 1.20.1          | 47.1.100
        1.20.1-forge-47.1.5                  | 1.20.1          | 47.1.5
        """)
    void matchesNeoForgeVersions(String id, String mcVersion, String build) {
        assertTrue(matcher.canMatch(id), id);

        VersionID versionId = matcher.match(id);
        assertEquals(NeoForge.PLATFORM_NAME, versionId.getPlatform().getName());
        assertEquals(mcVersion, versionId.getVersion().getName());
        assertEquals(build, versionId.getBuild().orElseThrow().getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "neoforge-21.1.2400",        // the default matcher would find 21.1.240
        "neoforge-21.1",
        "neoforge-99.1.240",
        "neoforge-21.1.240-beta",
        "neoforge-21.1.240 ",
        "neoforge-21.1.240-",
        "neoforge-",
        "neoforge-beta",
        "neoforge-21.1.240 beta",
        "1.20.1-forge-47.0.1",       // a Forge build
        "1.12.2-forge-14.23.5.2852", // a Forge build of another mc version
    })
    void neoForgeVersionsThatDoNotExistAreNotMatched(String id) {
        assertFalse(matcher.canMatch(id), id);
        assertThrows(VersionMatchException.class, () -> matcher.match(id));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "1.21.1",
        "fabric-loader-0.17.3-1.21.10",
        "Forge8.9.0.753",
        "1.7.10-Forge10.13.2.1300-1.7.10",
        "1.20.2-forge-47.1.100", // the legacy format only exists for 1.20.1
    })
    void versionsOfOtherPlatformsAreNotMatched(String id) {
        assertFalse(matcher.canMatch(id), id);
    }

    @Test
    void longerBuildNamesOfOtherMcVersionsAreNotMatched() {
        // neoforge-21.10.64 contains 0.64, the build name of NeoForge 26.2.0.64
        FakeVersionService versions = new FakeVersionService(NeoForge.PLATFORM_NAME)
            .withBuilds("26.2", "0.64")
            .withBuilds("1.21.10", "64");
        Matcher matcher = matcher(new FakeVanillaVersionService("26.2", "1.21.10"), versions);

        assertEquals("1.21.10", matcher.match("neoforge-21.10.64").getVersion().getName());
        assertEquals("26.2", matcher.match("neoforge-26.2.0.64").getVersion().getName());
    }

    /**
     * Every NeoForge version of the bundled prism index has to resolve to its own build,
     * without help from {@link Version#getInheritsFrom()}.
     * This covers legacy versions, where NeoForge cuts off the {@code 1.} of the mc version
     * ({@code 21.1.240} for {@code 1.21.1}, {@code 21.0.167} for {@code 1.21}),
     * new versions that keep the full mc version ({@code 26.1.2.82} for {@code 26.1.2}),
     * and versions that do not contain their mc version at all ({@code 47.1.100} for {@code 1.20.1}).
     */
    @TestFactory
    Stream<DynamicTest> everyBundledNeoForgeVersionResolvesToItsBuild() {
        PrismIndex index = BundledVersions.cacheObject("neoforge/neoforge-versions.json", PrismIndex.class);
        return index.versions().stream().map(meta -> DynamicTest.dynamicTest(meta.version(), () -> {
            String id = meta.getMcVersion().equals("1.20.1")
                ? "1.20.1-forge-" + meta.version()
                : "neoforge-" + meta.version();
            VersionID versionId = matcher.match(id);
            assertEquals(meta.getMcVersion(), versionId.getVersion().getName());
            assertEquals(
                NeoForgeVersionService.getBuildName(meta.version(), meta.getMcVersion()),
                versionId.getBuild().orElseThrow().getName()
            );
        }));
    }

    @Test
    void legacyVersionsDoNotConfuseMcVersionsThatArePrefixesOfEachOther() {
        // 21.10.64 starts with 21.1, 1.21.1 must not take it as build 0.64
        FakeVersionService versions = new FakeVersionService(NeoForge.PLATFORM_NAME)
            .withBuilds("1.21.10", "64")
            .withBuilds("1.21.1", "0.64");
        Matcher matcher = matcher(new FakeVanillaVersionService("1.21.10", "1.21.1"), versions);

        assertEquals("1.21.10", matcher.match("neoforge-21.10.64").getVersion().getName());
        assertEquals("1.21.1", matcher.match("neoforge-21.1.0.64").getVersion().getName());
    }

    @Test
    void newVersionsDoNotConfuseMcVersionsThatArePrefixesOfEachOther() {
        // 26.10.0.5 starts with 26.1, 26.1 must not take it as build 0.0.5
        FakeVersionService versions = new FakeVersionService(NeoForge.PLATFORM_NAME)
            .withBuilds("26.10", "0.5")
            .withBuilds("26.1", "0.0.5");
        Matcher matcher = matcher(new FakeVanillaVersionService("26.10", "26.1"), versions);

        assertEquals("26.10", matcher.match("neoforge-26.10.0.5").getVersion().getName());
        assertEquals("26.1", matcher.match("neoforge-26.1.0.0.5").getVersion().getName());
    }

    @Test
    void versionsThatCouldBeNewOrLegacyAreDecidedByInheritsFrom() {
        // 26.1.0.1-beta is build 0.1-beta of 26.1, or of 1.26.1 if the 1. had been cut off
        FakeVersionService versions = new FakeVersionService(NeoForge.PLATFORM_NAME)
            .withBuilds("26.1", "0.1-beta")
            .withBuilds("1.26.1", "0.1-beta");
        Matcher matcher = matcher(new FakeVanillaVersionService("26.1", "1.26.1"), versions);

        assertFalse(matcher.canMatch("neoforge-26.1.0.1-beta"));
        Version version = new FakeVersion("neoforge-26.1.0.1-beta").withInheritsFrom("26.1");
        assertEquals("26.1", matcher.match(version).getVersion().getName());
    }

    @Test
    void ambiguousNeoForgeVersionsAreDecidedByInheritsFrom() {
        // 26.1.2.0 could be build 0 for 26.1.2, or build 2.0 for 26.1
        FakeVersionService versions = new FakeVersionService(NeoForge.PLATFORM_NAME)
            .withBuilds("26.1.2", "0")
            .withBuilds("26.1", "2.0");
        Matcher matcher = matcher(new FakeVanillaVersionService("26.1.2", "26.1"), versions);

        assertFalse(matcher.canMatch("neoforge-26.1.2.0"));
        Version version = new FakeVersion("neoforge-26.1.2.0").withInheritsFrom("26.1");
        assertEquals("2.0", matcher.match(version).getBuild().orElseThrow().getName());
    }

    @Test
    void theVanillaParentDecidesBetweenNeoForgeVersions() {
        Version version = new FakeVersion("neoforge-21.1.240").withInheritsFrom("1.21.10");

        // 240 does not exist for 1.21.10
        assertFalse(matcher.canMatch(version));
    }

    @Test
    void malformedNeoForgeIdsDoNotFallBackToTheDefaultMatcher() {
        Version version = new FakeVersion("neoforge-21.1.240 ").withInheritsFrom("1.21.1");

        assertFalse(matcher.canMatch(version));
        assertThrows(VersionMatchException.class, () -> matcher.match(version));
    }

    @Test
    void customNamesFallBackToTheDefaultMatcher() {
        Version version = new FakeVersion("my-neoforge-pack-240").withInheritsFrom("1.21.1");

        assertTrue(matcher.canMatch(version));
        VersionID id = matcher.match(version);
        assertEquals("1.21.1", id.getVersion().getName());
        assertEquals("240", id.getBuild().orElseThrow().getName());
    }

    /**
     * Every NeoForge version in mc-versions has to match exactly the mc version it inherits from,
     * with a build that is the NeoForge version given to fml, without the mc version.
     * Forge versions must not match, except the few 1.20.1 builds that exist on both platforms.
     */
    @TestFactory
    Stream<DynamicTest> matchesAllVersionsOfMcVersions() {
        Optional<McVersions> mcVersions = McVersions.find();
        assumeTrue(mcVersions.isPresent(), "mc-versions has not been cloned next to HeadlessMc");
        Matcher matcher = matcher(mcVersions.get().vanillaVersionService(), bundledVersionService());

        return mcVersions.get().versions().stream().map(mcVersion -> DynamicTest.dynamicTest(mcVersion.id(), () -> {
            boolean isNeoForge = mcVersion.libraries().stream().anyMatch(library -> library.startsWith("net.neoforged"));
            if (!isNeoForge) {
                if (matcher.canMatch(mcVersion.toVersion())) {
                    assertTrue(mcVersion.id().startsWith("1.20.1-forge-"), mcVersion.id());
                    assertEquals("1.20.1", matcher.match(mcVersion.toVersion()).getVersion().getName());
                }

                return;
            }

            String neoForgeVersion = mcVersion.gameArgument(NEOFORGE_VERSION)
                .or(() -> mcVersion.gameArgument(FORGE_VERSION))
                .orElseThrow();
            assertTrue(matcher.canMatch(mcVersion.toVersion()), mcVersion.id());
            VersionID id = matcher.match(mcVersion.toVersion());
            String build = id.getBuild().orElseThrow().getName();
            assertEquals(NeoForge.PLATFORM_NAME, id.getPlatform().getName());
            assertEquals(mcVersion.inheritsFrom(), id.getVersion().getName());
            assertTrue(neoForgeVersion.equals(build) || neoForgeVersion.endsWith("." + build),
                build + " is not the build of " + neoForgeVersion);
        }));
    }

    private record Matcher(NeoForgeVersionMatcher matcher, PlatformService platformService) {
        private static final VersionProcessor PROCESSOR = new FakeVersionJsonService();

        boolean canMatch(String id) {
            return canMatch(new FakeVersion(id));
        }

        boolean canMatch(Version version) {
            return matcher.canMatch(platformService, version, PROCESSOR);
        }

        VersionID match(String id) {
            return match(new FakeVersion(id));
        }

        VersionID match(Version version) {
            return matcher.match(platformService, version, PROCESSOR);
        }
    }

}
