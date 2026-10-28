package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.files.cache.CacheBuilder;
import io.github.headlesshq.headlessmc.platform.BoundPlatformVersion;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ForgeVersionMatcherTest {
    private static final List<String> FORGE_LIBRARIES = List.of(
        "net.minecraftforge:forge:",
        "net.minecraftforge:fmlloader:",
        "net.minecraftforge:minecraftforge:"
    );

    static ForgeVersionService bundledVersionService() {
        PrismIndex index = BundledVersions.cacheObject("forge/forge-versions.json", PrismIndex.class);
        Cache<PrismIndex> cache = CacheBuilder.<PrismIndex>create().withSource(() -> Optional.of(index)).build();
        return new ForgeVersionService(Forge.PLATFORM_NAME, cache);
    }

    private static Matcher matcher(FakeVanillaVersionService vanillaVersionService) {
        ForgeVersionService versionService = bundledVersionService();
        PlatformService platformService = new FakePlatformService(
            new FakeVanillaPlatform(vanillaVersionService),
            new FakePlatform(Forge.PLATFORM_NAME, versionService)
        );

        return new Matcher(new ForgeVersionMatcher(vanillaVersionService, versionService), platformService);
    }

    private final Matcher matcher = matcher(BundledVersions.vanillaVersionService());

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
        Forge8.9.0.753                                | 1.6.1       | 8.9.0.753
        Forge9.10.0.776                               | 1.6.2       | 9.10.0.776
        1.5.2-Forge7.8.0.684                          | 1.5.2       | 7.8.0.684
        1.8-Forge11.14.0.1296                         | 1.8         | 11.14.0.1296
        1.8-Forge11.14.0.1237-1.8                     | 1.8         | 11.14.0.1237
        1.7.10-Forge10.13.2.1300-1.7.10               | 1.7.10      | 10.13.2.1300
        1.7.10-Forge10.13.1.1210-new                  | 1.7.10      | 10.13.1.1210
        1.7.10-Forge10.13.3.1389-1710ls               | 1.7.10      | 10.13.3.1389
        1.7.2-Forge10.12.2.1154-mc172                 | 1.7.2       | 10.12.2.1154
        1.7.10-pre4-Forge10.12.2.1137-prerelease      | 1.7.10-pre4 | 10.12.2.1137
        1.10.2-forge1.10.2-12.18.0.2008               | 1.10.2      | 12.18.0.2008
        1.10.2-forge1.10.2-12.18.0.2001-1.10.0        | 1.10.2      | 12.18.0.2001
        1.10.2-forge1.10.2-12.18.1.2015-failtests     | 1.10.2      | 12.18.1.2015
        1.9-forge1.9-12.16.0.1766-1.9                 | 1.9         | 12.16.0.1766
        1.9.4-forge1.9.4-12.17.0.1919-EHUnit          | 1.9.4       | 12.17.0.1919
        1.11-forge1.11-13.19.0.2130                   | 1.11        | 13.19.0.2130
        1.11-forge1.11-13.19.0.2126-1.11.x            | 1.11        | 13.19.0.2126
        1.11.2-forge1.11.2-13.20.0.2245-3630          | 1.11.2      | 13.20.0.2245
        1.12.2-forge-14.23.5.2852                     | 1.12.2      | 14.23.5.2852
        1.13.2-forge-25.0.10                          | 1.13.2      | 25.0.10
        1.15-forge-29.0.0                             | 1.15        | 29.0.0
        1.20.1-forge-47.0.1                           | 1.20.1      | 47.0.1
        1.20.3-forge49.0.1                            | 1.20.3      | 49.0.1
        26.1.2-forge-64.0.12                          | 26.1.2      | 64.0.12
        """)
    void matchesAllForgeFormats(String id, String mcVersion, String build) {
        assertTrue(matcher.canMatch(id), id);

        VersionID versionId = matcher.match(id);
        assertEquals(Forge.PLATFORM_NAME, versionId.getPlatform().getName());
        assertEquals(mcVersion, versionId.getVersion().getName());
        assertEquals(build, versionId.getBuild().orElseThrow().getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "1.20.1-forge-47.1.100",            // a NeoForge build
        "1.12.2-forge-99.9.9",              // unknown build
        "1.12.2-forge-14.23.5",             // substring of a build
        "1.12.2-forge-14.23.5.28520",       // the default matcher would find 14.23.5.2852
        "1.12.1-forge-14.23.5.2852",        // build of another mc version
        "9.99-forge-14.23.5.2852",          // unknown mc version
        "1.10.2-forge1.10.2-99.18.0.2008",
        "1.10.2-forge1.10-12.18.0.2008",    // the repeated mc version differs
        "1.5.2-Forge7.8.0.68",
        "Forge8.9.0.75",
        "Forge99.9.0.753",
    })
    void forgeVersionsThatDoNotExistAreNotMatched(String id) {
        assertFalse(matcher.canMatch(id), id);
        assertThrows(VersionMatchException.class, () -> matcher.match(id));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "1.21.1",
        "fabric-loader-0.17.3-1.21.10",
        "rd-132211",
    })
    void versionsOfOtherPlatformsAreNotMatched(String id) {
        assertFalse(matcher.canMatch(id), id);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "neoforge-21.1.240",
        "neoforge-20.2.12-beta",
    })
    void neoForgeVersionsDoNotMatchAForgePattern(String id) {
        // they still contain forge and are left to VersionMatcher.includes
        for (Pattern pattern : List.of(
            ForgeVersionMatcher.MODERN,
            ForgeVersionMatcher.REPEATED_MC,
            ForgeVersionMatcher.LEGACY,
            ForgeVersionMatcher.BUILD_ONLY
        )) {
            assertFalse(pattern.matcher(id).matches(), pattern.pattern());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "1.12.2-Forge-14.23.5.2852",
        "1.13.2-FORGE-25.0.10",
        "forge8.9.0.753",
        "FORGE8.9.0.753",
        "1.12.2 -forge-14.23.5.2852",
        "1.7.10-Forge10.13.2.1300-1.7 10",
    })
    void theFormatsAreCaseAndWhitespaceSensitive(String id) {
        for (Pattern pattern : List.of(
            ForgeVersionMatcher.MODERN,
            ForgeVersionMatcher.REPEATED_MC,
            ForgeVersionMatcher.LEGACY,
            ForgeVersionMatcher.BUILD_ONLY
        )) {
            assertFalse(pattern.matcher(id).matches(), pattern.pattern());
        }
    }

    @Test
    void theVanillaParentDecidesTheMcVersion() {
        // 14.23.5.2852 is a build for 1.12.2, not 1.12.1
        Version version = new FakeVersion("1.12.2-forge-14.23.5.2852").withInheritsFrom("1.12.1");

        assertFalse(matcher.canMatch(version));
    }

    @Test
    void customNamesFallBackToTheDefaultMatcher() {
        Version version = new FakeVersion("my-forge-pack-14.23.5.2852").withInheritsFrom("1.12.2");

        assertTrue(matcher.canMatch(version));
        VersionID id = matcher.match(version);
        assertEquals("1.12.2", id.getVersion().getName());
        assertEquals("14.23.5.2852", id.getBuild().orElseThrow().getName());
    }

    @Test
    void platformNameIsForge() {
        assertEquals(Forge.PLATFORM_NAME, matcher.matcher.getPlatformName());
    }

    /**
     * Every Forge version in mc-versions has to match exactly the mc version and the build
     * that its version.json names, every other version must not match.
     * The mc version is taken from the {@code --fml.mcVersion} argument, the parent, the forge library,
     * or, for legacy versions without any of these, from {@code forge_legacy_versions.json} or the Prism index.
     */
    @TestFactory
    Stream<DynamicTest> matchesAllVersionsOfMcVersions() {
        Optional<McVersions> mcVersions = McVersions.find();
        assumeTrue(mcVersions.isPresent(), "mc-versions has not been cloned next to HeadlessMc");
        Matcher matcher = matcher(mcVersions.get().vanillaVersionService());
        Map<String, String> legacyVersions = legacyVersions(mcVersions.get());
        Map<String, String> indexVersions = bundledVersionService().getVersions().stream()
            .collect(Collectors.toMap(BoundPlatformVersion::getName, BoundPlatformVersion::getVanillaVersion));

        return mcVersions.get().versions().stream().map(mcVersion -> DynamicTest.dynamicTest(mcVersion.id(), () -> {
            Optional<String> library = FORGE_LIBRARIES.stream()
                .map(mcVersion::library)
                .flatMap(Optional::stream)
                .findFirst();
            if (library.isEmpty()) {
                if (!mcVersion.id().contains("neoforge")) {
                    assertFalse(matcher.canMatch(mcVersion.toVersion()), mcVersion.id());
                }

                return;
            }

            String libraryVersion = library.get().split(":")[2];
            String[] parts = libraryVersion.split("-");
            String build = mcVersion.gameArgument("--fml.forgeVersion")
                .orElse(parts.length == 1 ? libraryVersion : parts[1]);
            String mc = mcVersion.gameArgument("--fml.mcVersion")
                .or(() -> Optional.ofNullable(mcVersion.inheritsFrom()))
                .orElseGet(() -> parts.length == 1
                    ? legacyVersions.getOrDefault(build, indexVersions.get(build))
                    : parts[0].replace('_', '-'));

            assertTrue(matcher.canMatch(mcVersion.toVersion()), mcVersion.id());
            VersionID id = matcher.match(mcVersion.toVersion());
            assertEquals(Forge.PLATFORM_NAME, id.getPlatform().getName());
            assertEquals(mc, id.getVersion().getName());
            assertEquals(build, id.getBuild().orElseThrow().getName());
        }));
    }

    private static Map<String, String> legacyVersions(McVersions mcVersions) {
        JsonNode legacy = mcVersions.readJson("forge_legacy_versions.json");
        return legacy.propertyNames().stream()
            .collect(Collectors.toMap(build -> build, build -> legacy.get(build).asString()));
    }

    private record Matcher(ForgeVersionMatcher matcher, PlatformService platformService) {
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
