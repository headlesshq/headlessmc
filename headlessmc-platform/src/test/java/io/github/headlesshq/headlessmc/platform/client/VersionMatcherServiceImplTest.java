package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.FakeVanillaVersionService;
import io.github.headlesshq.headlessmc.platform.FakeVersionService;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionProcessor;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.github.headlesshq.headlessmc.version.service.FakeVersionJsonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VersionMatcherServiceImplTest {
    /** A matcher that always fails, to exercise the suppressed-exception paths. */
    private static final class FailingMatcher implements VersionMatcher {
        @Override
        public VersionID match(PlatformService platformService, Version version, VersionProcessor processor) {
            throw new VersionMatchException("nope");
        }

        @Override
        public boolean canMatch(PlatformService platformService, Version version, VersionProcessor processor) {
            throw new VersionMatchException("cannot match");
        }

        @Override
        public String getPlatformName() {
            return "broken";
        }
    }

    private FakeVanillaVersionService vanillaVersions;
    private FakeVanillaPlatform vanilla;
    private FakePlatform fabric;
    private FakePlatformService platformService;
    private VersionMatcherServiceImpl service;
    private FakeVersionJsonService versions;

    @BeforeEach
    void setup() {
        vanillaVersions = new FakeVanillaVersionService("1.21.1", "1.20.4");
        vanilla = new FakeVanillaPlatform("1.21.1", "1.20.4");
        vanilla.withClientSupport(new ClientSupport(
            new DefaultVersionMatcher(vanillaVersions, vanillaVersions) {
                @Override
                public boolean canMatch(PlatformService service, Version version, VersionProcessor processor) {
                    return true;
                }
            },
            (id, mcDir, args) -> new FakeVersion(id.getVersion().getName())
        ));

        FakeVersionService fabricVersions = new FakeVersionService("fabric")
            .withBuilds("1.21.1", "0.16.9")
            .withBuilds("1.20.4", "0.15.0");
        fabric = new FakePlatform("fabric", fabricVersions);
        fabric.withClientSupport(new ClientSupport(
            new DefaultVersionMatcher(vanillaVersions, fabricVersions),
            (id, mcDir, args) -> new FakeVersion(id.getVersion().getName())
        ));

        platformService = new FakePlatformService(vanilla, fabric);
        service = new VersionMatcherServiceImpl(platformService);
        versions = new FakeVersionJsonService();
    }

    @Test
    void matchesVanillaVersion() {
        Set<VersionID> ids = service.match(new FakeVersion("1.21.1"), versions);

        assertEquals(1, ids.size());
        assertEquals("vanilla", ids.iterator().next().getPlatform().getName());
    }

    @Test
    void matchesModdedVersionOnBothPlatforms() {
        Set<VersionID> ids = service.match(new FakeVersion("fabric-loader-0.16.9-1.21.1"), versions);

        assertEquals(
            Set.of("vanilla", "fabric"),
            ids.stream().map(id -> id.getPlatform().getName()).collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void unmatchableVersionThrows() {
        VersionMatchException e = assertThrows(
            VersionMatchException.class, () -> service.match(new FakeVersion("something-else"), versions)
        );
        assertTrue(e.getMessage().contains("Failed to parse version"));
    }

    @Test
    void failingCanMatchIsSuppressedAndReported() {
        FakePlatform broken = new FakePlatform("broken", new FakeVersionService("broken"));
        broken.withClientSupport(new ClientSupport(new FailingMatcher(), (id, dir, args) -> new FakeVersion("x")));
        VersionMatcherServiceImpl brokenService = new VersionMatcherServiceImpl(
            new FakePlatformService(new FakeVanillaPlatform("1.21.1"), broken)
        );

        HeadlessMcException e = assertThrows(
            HeadlessMcException.class, () -> brokenService.match(new FakeVersion("1.21.1"), versions)
        );
        assertEquals(1, e.getSuppressed().length);
    }

    @Test
    void platformWithoutClientSupportIsSkipped() {
        FakePlatform noSupport = new FakePlatform("nosupport", new FakeVersionService("nosupport"));
        VersionMatcherServiceImpl withoutSupport = new VersionMatcherServiceImpl(
            new FakePlatformService(vanilla, noSupport)
        );

        assertEquals(1, withoutSupport.match(new FakeVersion("1.21.1"), versions).size());
    }

    @Test
    void reverseMatchFindsExactVersionId() {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("fabric", "1.21.1", "0.16.9"));
        Version version = new FakeVersion("fabric-loader-0.16.9-1.21.1");

        Optional<VersionMatcherService.MatchResult> result = service.match(id, List.of(version), versions);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().resolvedId());
        assertEquals(version, result.get().version());
    }

    @Test
    void reverseMatchWithAnyBuildResolvesTheBuild() {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("fabric", "1.21.1"));
        Version version = new FakeVersion("fabric-loader-0.16.9-1.21.1");

        Optional<VersionMatcherService.MatchResult> result = service.match(id, List.of(version), versions);

        assertTrue(result.isPresent());
        assertEquals("0.16.9", result.get().resolvedId().getBuild().orElseThrow().getName());
    }

    @Test
    void reverseMatchWithAnyBuildIgnoresOtherPlatformVersions() {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("fabric", "1.20.4"));
        Version version = new FakeVersion("fabric-loader-0.16.9-1.21.1");

        assertEquals(Optional.empty(), service.match(id, List.of(version), versions));
    }

    @Test
    void reverseMatchSkipsUnmatchableVersions() {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("fabric", "1.21.1", "0.16.9"));
        Version version = new FakeVersion("fabric-loader-0.16.9-1.21.1");

        Optional<VersionMatcherService.MatchResult> result =
            service.match(id, List.of(new FakeVersion("something-else"), version), versions);

        assertTrue(result.isPresent());
        assertEquals(version, result.get().version());
    }

    @Test
    void reverseMatchWithAnyBuildSkipsUnmatchableVersions() {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("fabric", "1.21.1"));
        Version version = new FakeVersion("fabric-loader-0.16.9-1.21.1");

        Optional<VersionMatcherService.MatchResult> result =
            service.match(id, List.of(new FakeVersion("something-else"), version), versions);

        assertTrue(result.isPresent());
        assertEquals(version, result.get().version());
    }

    @Test
    void reverseMatchWithOnlyUnmatchableVersionsIsEmpty() {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("fabric", "1.21.1"));
        assertEquals(Optional.empty(), service.match(id, List.of(new FakeVersion("something-else")), versions));
    }

    @Test
    void reverseMatchWithoutCandidatesIsEmpty() {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("fabric", "1.21.1", "0.16.9"));
        assertEquals(Optional.empty(), service.match(id, List.of(), versions));
    }

    @Test
    void includesPrefersTheContainingPlatform() {
        Platform forge = new FakePlatform("forge", new FakeVersionService("forge"));
        VersionMatcher neoforge = new DefaultVersionMatcher(vanillaVersions, new FakeVersionService("neoforge"));

        assertTrue(neoforge.includes(platformService, new FakeVersion("neoforge-1.21.1"), forge));
        assertFalse(neoforge.includes(platformService, new FakeVersion("forge-1.21.1"), forge));
    }

}
