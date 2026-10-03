package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.FakeVanillaVersionService;
import io.github.headlesshq.headlessmc.platform.FakeVersionService;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionProcessor;
import io.github.headlesshq.headlessmc.version.service.FakeVersionJsonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultVersionMatcherTest {
    private FakeVanillaVersionService vanillaVersions;
    private FakeVersionService fabricVersions;
    private PlatformService platformService;
    private DefaultVersionMatcher matcher;
    private FakeVersionJsonService versions;

    @BeforeEach
    void setup() {
        vanillaVersions = new FakeVanillaVersionService("1.21.1", "1.20.4");
        fabricVersions = new FakeVersionService("fabric")
            .withBuilds("1.21.1", "0.16.9")
            .withBuilds("1.20.4", "0.15.0");

        FakeVanillaPlatform vanilla = new FakeVanillaPlatform("1.21.1", "1.20.4");
        FakePlatform fabric = new FakePlatform("fabric", fabricVersions);
        platformService = new FakePlatformService(vanilla, fabric);
        matcher = new DefaultVersionMatcher(vanillaVersions, fabricVersions);
        versions = new FakeVersionJsonService();
    }

    private VersionProcessor processor() {
        return versions;
    }

    @Test
    void platformNameComesFromVersionService() {
        assertEquals("fabric", matcher.getPlatformName());
    }

    @Test
    void canMatchOnlyVersionsContainingThePlatformName() {
        assertTrue(matcher.canMatch(platformService, new FakeVersion("fabric-loader-0.16.9-1.21.1"), processor()));
        assertFalse(matcher.canMatch(platformService, new FakeVersion("1.21.1"), processor()));
    }

    @Test
    void matchesBoundPlatformVersion() {
        VersionID id = matcher.match(platformService, new FakeVersion("fabric-loader-0.16.9-1.21.1"), processor());

        assertEquals("fabric", id.getPlatform().getName());
        assertEquals("1.21.1", id.getVersion().getName());
        assertEquals("0.16.9", id.getBuild().orElseThrow().getName());
    }

    @Test
    void unmatchedVersionThrows() {
        VersionMatchException e = assertThrows(VersionMatchException.class,
            () -> matcher.match(platformService, new FakeVersion("fabric-loader-9.9.9-1.21.1"), processor()));
        assertTrue(e.getMessage().contains("Failed to find platform version"));
    }

    @Test
    void ambiguousMatchIsResolvedByVanillaVersion() {
        // both builds have the same name length and are contained in the id
        fabricVersions.withBuilds("1.21.1", "0.16.9").withBuilds("1.20.4", "0.16.9");

        VersionID id = matcher.match(platformService, new FakeVersion("fabric-loader-0.16.9-1.21.1"), processor());
        assertEquals("1.21.1", id.getVersion().getName());
    }

    @Test
    void ambiguousMatchWithoutUniqueVanillaVersionThrows() {
        fabricVersions.withBuilds("1.21.1", "0.16.9").withBuilds("1.20.4", "0.16.9");

        VersionMatchException e = assertThrows(VersionMatchException.class,
            () -> matcher.match(platformService, new FakeVersion("fabric-loader-0.16.9-1.19.4"), processor()));
        assertTrue(e.getMessage().contains("Failed to determine vanilla version"));
    }

    @Test
    void vanillaVersionIsTakenFromInheritsFrom() {
        fabricVersions.withBuilds("1.21.1", "0.16.9").withBuilds("1.20.4", "0.16.9");
        Version version = new FakeVersion("fabric-loader-0.16.9").withInheritsFrom("1.21.1");

        VersionID id = matcher.match(platformService, version, processor());
        assertEquals("1.21.1", id.getVersion().getName());
    }

    @Test
    void vanillaVersionIsTakenFromParentHierarchy() {
        fabricVersions.withBuilds("1.21.1", "0.16.9").withBuilds("1.20.4", "0.16.9");

        Version parent = new FakeVersion("base-pack").withInheritsFrom("1.21.1");
        Version child = new FakeVersion("fabric-loader-0.16.9-modpack").withInheritsFrom("base-pack");
        versions.add(new FakeVersion("1.21.1")).add(parent).add(child);

        VersionID id = matcher.match(platformService, child, processor());
        assertEquals("1.21.1", id.getVersion().getName());
    }

    @Test
    void matchesVanillaVersionDirectly() {
        DefaultVersionMatcher vanillaMatcher = new DefaultVersionMatcher(vanillaVersions, vanillaVersions);

        VersionID id = vanillaMatcher.match(platformService, new FakeVersion("1.21.1"), processor());
        assertEquals("vanilla", id.getPlatform().getName());
        assertEquals("1.21.1", id.getVersion().getName());
        assertTrue(id.isAnyBuild());
    }

    @Test
    void emptyNameMatchResultIsNeitherValidNorAmbiguous() {
        AbstractVersionMatcher.NameMatchResult<VanillaVersion> empty =
            new AbstractVersionMatcher.NameMatchResult<>(List.of());

        assertFalse(empty.isValid());
        assertFalse(empty.isAmbiguous());
    }

    @Test
    void singleNameMatchResultIsValid() {
        AbstractVersionMatcher.NameMatchResult<VanillaVersion> single =
            new AbstractVersionMatcher.NameMatchResult<>(List.copyOf(vanillaVersions.getVersions()).subList(0, 1));

        assertTrue(single.isValid());
        assertFalse(single.isAmbiguous());
    }

}
