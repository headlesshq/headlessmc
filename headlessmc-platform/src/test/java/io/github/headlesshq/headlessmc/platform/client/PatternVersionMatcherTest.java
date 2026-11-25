package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.FakeVanillaVersionService;
import io.github.headlesshq.headlessmc.platform.FakeVersionService;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VanillaVersionService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.VersionService;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionProcessor;
import io.github.headlesshq.headlessmc.version.service.FakeVersionJsonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class PatternVersionMatcherTest {
    private static final String PLATFORM = "test";

    private FakeVanillaVersionService vanillaVersions;
    private FakeVersionService versions;
    private PlatformService platformService;
    private VersionProcessor processor;
    private TestMatcher matcher;

    @BeforeEach
    void setup() {
        vanillaVersions = new FakeVanillaVersionService("1.21.1", "1.21", "1.20.4", "1.2");
        versions = new FakeVersionService(PLATFORM)
            .withBuilds("1.21.1", "1.0.10", "1.0.1")
            .withBuilds("1.21", "1.0.1")
            .withBuilds("1.20.4", "0.9.0");
        platformService = new FakePlatformService(
            new FakeVanillaPlatform("1.21.1", "1.21", "1.20.4", "1.2"),
            new FakePlatform(PLATFORM, versions)
        );
        processor = new FakeVersionJsonService();
        matcher = new TestMatcher(vanillaVersions, versions);
    }

    private VersionID match(String id) {
        return matcher.match(platformService, new FakeVersion(id), processor);
    }

    private boolean canMatch(String id) {
        return matcher.canMatch(platformService, new FakeVersion(id), processor);
    }

    private static void assertVersionId(VersionID id, String mcVersion, String build) {
        assertEquals(PLATFORM, id.getPlatform().getName());
        assertEquals(mcVersion, id.getVersion().getName());
        assertEquals(build, id.getBuild().orElseThrow().getName());
    }

    @Test
    void matchesTheGroupsOfThePatternExactly() {
        assertTrue(canMatch("test-1.0.1-1.21"));
        assertVersionId(match("test-1.0.1-1.21"), "1.21", "1.0.1");
    }

    @Test
    void substringsOfTheGroupsAreNotConsidered() {
        // the id contains 1.0.1, 1.0.10, 1.2, 1.21 and 1.21.1, only the groups count
        assertVersionId(match("test-1.0.10-1.21.1"), "1.21.1", "1.0.10");
        assertVersionId(match("test-1.0.1-1.21.1"), "1.21.1", "1.0.1");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "custom-test-1.0.1-1.21",
        "xtest-1.0.1-1.21",
        " test-1.0.1-1.21",
    })
    void thePatternHasToMatchTheEntireId(String id) {
        assertFalse(matcher.matchPatterns(platformService, new FakeVersion(id)).matchedPattern());
        assertEquals(List.of(), matcher.resolved);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "test-9.9.9-1.21",   // unknown build
        "test-1.0.1-9.99",   // unknown mc version
        "test-1.0.10-1.21",  // build exists, but not for this mc version
        "test-0.9.0-1.21.1", // build exists, but not for this mc version
    })
    void idsMatchingAPatternThatDoNotResolveAreNotMatched(String id) {
        assertFalse(canMatch(id));
        VersionMatchException e = assertThrows(VersionMatchException.class, () -> match(id));
        assertTrue(e.getMessage().contains("does not name an existing test version"), e.getMessage());
    }

    @Test
    void idsMatchingAPatternDoNotFallBackToTheDefaultMatcher() {
        // the default matcher would find 1.0.1 and 1.21.1 in the id
        assertFalse(canMatch("test-1.0.1x-1.21.1"));
        assertThrows(VersionMatchException.class, () -> match("test-1.0.1x-1.21.1"));
    }

    @Test
    void idsMatchingNoPatternFallBackToTheDefaultMatcher() {
        Version version = new FakeVersion("my-test-pack-0.9.0").withInheritsFrom("1.20.4");

        assertTrue(matcher.canMatch(platformService, version, processor));
        assertVersionId(matcher.match(platformService, version, processor), "1.20.4", "0.9.0");
    }

    @Test
    void idsMatchingNoPatternAndNotContainingThePlatformNameAreNotMatched() {
        assertFalse(canMatch("1.21.1"));
        assertFalse(canMatch("something-else"));
    }

    @Test
    void laterPatternsAreTriedIfAnEarlierOneDoesNotResolve() {
        // the first pattern splits at the first -, resolving the unknown mc version beta-1.21
        versions.withBuilds("1.21", "1.0.1-beta");
        matcher.withPattern(Pattern.compile("test-(?<build>.+)-(?<mc>.+)"));

        assertVersionId(match("test-1.0.1-beta-1.21"), "1.21", "1.0.1-beta");
        assertEquals(List.of("test-(?<build>[^-]+)-(?<mc>.+)", "test-(?<build>.+)-(?<mc>.+)"), matcher.resolved);
    }

    @Test
    void theFirstPatternThatResolvesWins() {
        matcher.withPattern(Pattern.compile("test-(?<build>.+)-(?<mc>.+)"));

        assertVersionId(match("test-1.0.1-1.21"), "1.21", "1.0.1");
        assertEquals(List.of("test-(?<build>[^-]+)-(?<mc>.+)"), matcher.resolved);
    }

    @Test
    void resolveBuildFindsTheUniqueBuildWithThatName() {
        Optional<VersionID> id = matcher.resolveBuild(platformService, new FakeVersion("x"), "0.9.0");

        assertVersionId(id.orElseThrow(), "1.20.4", "0.9.0");
    }

    @Test
    void resolveBuildDoesNotGuessBetweenMcVersions() {
        // 1.0.1 exists for 1.21.1 and 1.21
        assertTrue(matcher.resolveBuild(platformService, new FakeVersion("x"), "1.0.1").isEmpty());
    }

    @Test
    void resolveBuildUsesInheritsFromToDecideBetweenMcVersions() {
        Version version = new FakeVersion("x").withInheritsFrom("1.21");

        assertVersionId(matcher.resolveBuild(platformService, version, "1.0.1").orElseThrow(), "1.21", "1.0.1");
    }

    @Test
    void resolveBuildWithInheritsFromOfAnotherMcVersionResolvesNothing() {
        Version version = new FakeVersion("x").withInheritsFrom("1.20.4");

        assertTrue(matcher.resolveBuild(platformService, version, "1.0.1").isEmpty());
    }

    @Test
    void resolveBuildOfAnUnknownBuildResolvesNothing() {
        assertTrue(matcher.resolveBuild(platformService, new FakeVersion("x"), "4.2.0").isEmpty());
    }

    @Test
    void resolveBuildOnlyConsidersBuildsOfTheVanillaParent() {
        // 0.9.0 only exists for 1.20.4
        Version version = new FakeVersion("x").withInheritsFrom("1.21.1");

        assertTrue(matcher.resolveBuild(platformService, version, "0.9.0").isEmpty());
    }

    @Test
    void resolveBuildIgnoresParentsThatAreNotVanillaVersions() {
        Version version = new FakeVersion("x").withInheritsFrom("some-modpack");

        assertVersionId(matcher.resolveBuild(platformService, version, "0.9.0").orElseThrow(), "1.20.4", "0.9.0");
    }

    @Test
    void theVanillaParentIsPreferredOverTheMcVersionInTheId() {
        Version version = new FakeVersion("test-1.0.1-1.21.1").withInheritsFrom("1.21");

        assertVersionId(matcher.match(platformService, version, processor), "1.21", "1.0.1");
    }

    @Test
    void theMcVersionInTheIdDoesNotHaveToExistIfTheVanillaParentDoes() {
        Version version = new FakeVersion("test-0.9.0-custom").withInheritsFrom("1.20.4");

        assertVersionId(matcher.match(platformService, version, processor), "1.20.4", "0.9.0");
    }

    @Test
    void buildsThatDoNotExistForTheVanillaParentAreNotMatched() {
        // the id names 0.9.0 for 1.20.4, which exists, but the version is launched with 1.21
        Version version = new FakeVersion("test-0.9.0-1.20.4").withInheritsFrom("1.21");

        assertFalse(matcher.canMatch(platformService, version, processor));
    }

    @Test
    void parentsThatAreNotVanillaVersionsAreIgnored() {
        Version version = new FakeVersion("test-1.0.1-1.21").withInheritsFrom("some-modpack");

        assertVersionId(matcher.match(platformService, version, processor), "1.21", "1.0.1");
    }

    @Test
    void canFallBackPreventsTheDefaultMatcher() {
        TestMatcher strict = new TestMatcher(vanillaVersions, versions) {
            @Override
            protected boolean canFallBack(Version version) {
                return !version.getId().startsWith("test-");
            }
        };
        Version reserved = new FakeVersion("test-1.0.1").withInheritsFrom("1.21");
        Version custom = new FakeVersion("my-test-1.0.1").withInheritsFrom("1.21");

        assertFalse(strict.canMatch(platformService, reserved, processor));
        VersionMatchException e = assertThrows(VersionMatchException.class,
            () -> strict.match(platformService, reserved, processor));
        assertTrue(e.getMessage().contains("does not match the format of test versions"), e.getMessage());
        assertTrue(strict.canMatch(platformService, custom, processor));
    }

    @Test
    void platformNameComesFromTheVersionService() {
        assertEquals(PLATFORM, matcher.getPlatformName());
    }

    private static class TestMatcher extends PatternVersionMatcher {
        private final List<VersionPattern> patterns = new ArrayList<>();
        private final List<String> resolved = new ArrayList<>();

        TestMatcher(VanillaVersionService vanillaVersionService, VersionService versionService) {
            super(vanillaVersionService, versionService);
            withPattern(Pattern.compile("test-(?<build>[^-]+)-(?<mc>.+)"));
        }

        TestMatcher withPattern(Pattern pattern) {
            patterns.add(new VersionPattern(pattern, (platformService, version, match) -> {
                resolved.add(pattern.pattern());
                return resolve(platformService, version, match.group("mc"), match.group("build"));
            }));
            return this;
        }

        @Override
        protected List<VersionPattern> getPatterns() {
            return patterns;
        }
    }

}
