package io.github.headlesshq.headlessmc.platform.fabric;

import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.files.cache.CacheBuilder;
import io.github.headlesshq.headlessmc.platform.BundledVersions;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.FakeVanillaVersionService;
import io.github.headlesshq.headlessmc.platform.McVersions;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.client.VersionMatchException;
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
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FabricVersionMatcherTest {
    private static final String LOADER_LIBRARY = "net.fabricmc:fabric-loader:";

    private final VersionProcessor processor = new FakeVersionJsonService();

    static FabricVersionService bundledVersionService() {
        List<BuildData> builds = new ArrayList<>();
        for (JsonNode build : BundledVersions.cacheObject("fabric/fabric-versions.json")) {
            builds.add(new BuildData(
                build.get("separator").asString(),
                build.get("build").asInt(),
                build.get("maven").asString(),
                build.get("version").asString(),
                build.get("stable").asBoolean()
            ));
        }

        Cache<List<BuildData>> cache = CacheBuilder.<List<BuildData>>create()
            .withSource(() -> Optional.of(builds))
            .build();
        return new FabricVersionService(cache);
    }

    private static Matcher matcher(FakeVanillaVersionService vanillaVersionService) {
        FabricVersionService versionService = bundledVersionService();
        PlatformService platformService = new FakePlatformService(
            new FakeVanillaPlatform(vanillaVersionService),
            new FakePlatform(Fabric.PLATFORM_NAME, versionService)
        );

        return new Matcher(new FabricVersionMatcher(vanillaVersionService, versionService), platformService);
    }

    private final Matcher matcher = matcher(BundledVersions.vanillaVersionService());

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
        fabric-loader-0.17.3-1.21.10                 | 1.21.10            | 0.17.3
        fabric-loader-0.16.10-1.21.1                 | 1.21.1             | 0.16.10
        fabric-loader-0.16.1-1.21                    | 1.21               | 0.16.1
        fabric-loader-0.18.1-1.3                     | 1.3                | 0.18.1
        fabric-loader-0.18.1-1.3.1                   | 1.3.1              | 0.18.1
        fabric-loader-0.18.1-1.14.3-pre1             | 1.14.3-pre1        | 0.18.1
        fabric-loader-0.18.1-1.16.2-rc1              | 1.16.2-rc1         | 0.18.1
        fabric-loader-0.18.1-1.14 Pre-Release 1      | 1.14 Pre-Release 1 | 0.18.1
        fabric-loader-0.18.1-1.14.1 Pre-Release 1    | 1.14.1 Pre-Release 1 | 0.18.1
        fabric-loader-0.18.1-13w47a                  | 13w47a             | 0.18.1
        fabric-loader-0.18.1-20w14infinite           | 20w14infinite      | 0.18.1
        fabric-loader-0.18.1-23w13a_or_b             | 23w13a_or_b        | 0.18.1
        fabric-loader-0.18.1-3D Shareware v1.34      | 3D Shareware v1.34 | 0.18.1
        fabric-loader-0.19.3-26.1-snapshot-1         | 26.1-snapshot-1    | 0.19.3
        fabric-loader-0.19.3-26.1-pre-1              | 26.1-pre-1         | 0.19.3
        fabric-loader-0.19.3-26.1.1-rc-1             | 26.1.1-rc-1        | 0.19.3
        fabric-loader-0.4.8+build.155-1.14           | 1.14               | 0.4.8+build.155
        fabric-loader-0.14.21-1.8.9                  | 1.8.9              | 0.14.21
        """)
    void matchesFabricVersions(String id, String mcVersion, String build) {
        assertTrue(matcher.canMatch(id), id);

        VersionID versionId = matcher.match(id);
        assertEquals(Fabric.PLATFORM_NAME, versionId.getPlatform().getName());
        assertEquals(mcVersion, versionId.getVersion().getName());
        assertEquals(build, versionId.getBuild().orElseThrow().getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "fabric-loader-9.9.9-1.21.1",      // unknown loader
        "fabric-loader-0.17.3-9.99",       // unknown mc version
        "fabric-loader-0.17.3-1.21.10-",   // trailing junk is part of the mc version
        "fabric-loader-0.17.3-1.21.10 ",
        "fabric-loader-0.17.3x-1.21.10",
        "fabric-loader-0.17-1.21.10",      // substring of a loader version
        "fabric-loader-0.17.3-1.21.1000",  // the default matcher would find 0.17.3 and 1.21.10
        "fabric-loader-0.17.3- 1.21.10",
        "fabric-loader-0.17.3",            // malformed ids of the installer do not fall back
        "fabric-loader-0.17.3-",
        "fabric-loader--1.21.10",
        "fabric-loader-v0.17.3-1.21.10",
        "fabric-loader-0.17.3-beta-1.21.10",
    })
    void fabricVersionsThatDoNotExistAreNotMatched(String id) {
        assertFalse(matcher.canMatch(id), id);
        assertThrows(VersionMatchException.class, () -> matcher.match(id));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "1.21.1",
        "neoforge-21.1.240",
        "1.20.1-forge-47.1.100",
        "1.12.2-forge-14.23.5.2852",
        "Forge8.9.0.753",
        "rd-132211",
    })
    void versionsOfOtherPlatformsAreNotMatched(String id) {
        assertFalse(matcher.canMatch(id), id);
    }

    @Test
    void customNamesFallBackToTheDefaultMatcher() {
        Version version = new FakeVersion("my-fabric-pack-0.17.3").withInheritsFrom("1.21.10");

        assertTrue(matcher.matcher.canMatch(matcher.platformService, version, processor));
        VersionID id = matcher.matcher.match(matcher.platformService, version, processor);
        assertEquals("1.21.10", id.getVersion().getName());
        assertEquals("0.17.3", id.getBuild().orElseThrow().getName());
    }

    @Test
    void malformedFabricIdsDoNotFallBackToTheDefaultMatcher() {
        Version version = new FakeVersion("fabric-loader-0.17.3").withInheritsFrom("1.21.10");

        assertFalse(matcher.canMatch(version));
        assertThrows(VersionMatchException.class, () -> matcher.match(version));
    }

    @Test
    void theVanillaParentDecidesTheMcVersion() {
        Version version = new FakeVersion("fabric-loader-0.17.3-1.21.10").withInheritsFrom("1.21.1");

        assertEquals("1.21.1", matcher.match(version).getVersion().getName());
    }

    @Test
    void platformNameIsFabric() {
        assertEquals(Fabric.PLATFORM_NAME, matcher.matcher.getPlatformName());
    }

    /**
     * Every Fabric version in mc-versions has to match exactly the mc version it inherits from
     * and the loader it has as a library, every other version must not match.
     */
    @TestFactory
    Stream<DynamicTest> matchesAllVersionsOfMcVersions() {
        Optional<McVersions> mcVersions = McVersions.find();
        assumeTrue(mcVersions.isPresent(), "mc-versions has not been cloned next to HeadlessMc");
        Matcher matcher = matcher(mcVersions.get().vanillaVersionService());

        return mcVersions.get().versions().stream().map(mcVersion -> DynamicTest.dynamicTest(mcVersion.id(), () -> {
            Optional<String> loader = mcVersion.library(LOADER_LIBRARY);
            if (loader.isEmpty()) {
                assertFalse(matcher.canMatch(mcVersion.toVersion()), mcVersion.id());
                return;
            }

            assertTrue(matcher.canMatch(mcVersion.toVersion()), mcVersion.id());
            VersionID id = matcher.match(mcVersion.toVersion());
            assertEquals(Fabric.PLATFORM_NAME, id.getPlatform().getName());
            assertEquals(mcVersion.inheritsFrom(), id.getVersion().getName());
            assertEquals(loader.get().substring(LOADER_LIBRARY.length()), id.getBuild().orElseThrow().getName());
        }));
    }

    private record Matcher(FabricVersionMatcher matcher, PlatformService platformService) {
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
