package io.github.headlesshq.headlessmc.version.arg;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VersionArgPropertiesTest {
    @Test
    void platformDefaultsToVanillaAndIsLowerCased() {
        assertEquals(VersionArg.PLATFORM_VANILLA, VersionArg.builder().version("1.21.1").build().platform());
        assertEquals("fabric", VersionArg.builder().platform("FABRIC").version("1.21.1").build().platform());
    }

    @Test
    void versionIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> VersionArg.builder().build());
    }

    @Test
    void sideFlags() {
        VersionArg anySide = VersionArg.builder().version("1.21.1").build();

        assertTrue(anySide.isAnySide());
        assertFalse(anySide.withSide(Side.SERVER).isAnySide());
        assertEquals(Optional.of(Side.SERVER), anySide.withSide(Side.SERVER).side());
        assertTrue(anySide.withSide(Side.SERVER).withSide(null).isAnySide());
    }

    @Test
    void platformFlags() {
        assertTrue(VersionArg.parse("1.21.1").isVanilla());
        assertFalse(VersionArg.parse("fabric", "1.21.1").isVanilla());
    }

    @Test
    void buildFlags() {
        VersionArg anyBuild = VersionArg.parse("fabric", "1.21.1");
        VersionArg latest = anyBuild.withBuild(VersionArg.LATEST);
        VersionArg specific = anyBuild.withBuild("0.16.9");

        assertTrue(anyBuild.isAnyBuild());
        assertFalse(anyBuild.isLatest());
        assertFalse(anyBuild.isSpecificBuild());

        assertTrue(latest.isLatest());
        assertFalse(latest.isAnyBuild());
        assertFalse(latest.isSpecificBuild());

        assertTrue(specific.isSpecificBuild());
        assertEquals(Optional.of("0.16.9"), specific.build());
        assertTrue(specific.withBuild(null).isAnyBuild());
    }

    @Test
    void samePlatformVersionIgnoresSideAndBuild() {
        VersionArg first = VersionArg.parse("fabric", "1.21.1", "0.16.9");
        VersionArg second = VersionArg.parse("server", "fabric", "1.21.1");

        assertTrue(first.isSamePlatformVersion(second));
        assertFalse(first.isSamePlatformVersion(VersionArg.parse("fabric", "1.20.4")));
        assertFalse(first.isSamePlatformVersion(VersionArg.parse("forge", "1.21.1")));
    }

    @Test
    void asVanillaVersionDropsPlatformAndBuild() {
        VersionArg vanilla = VersionArg.parse("server", "fabric", "1.21.1", "0.16.9").asVanillaVersion();

        assertTrue(vanilla.isVanilla());
        assertTrue(vanilla.isAnyBuild());
        assertEquals(Optional.of(Side.SERVER), vanilla.side());
    }

    @Test
    void toStringOmitsTheVanillaPlatform() {
        assertEquals("1.21.1", VersionArg.parse("1.21.1").toString());
        assertEquals("fabric/1.21.1", VersionArg.parse("fabric", "1.21.1").toString());
        assertEquals("fabric/1.21.1/0.16.9", VersionArg.parse("fabric", "1.21.1", "0.16.9").toString());
        assertEquals("server/fabric/1.21.1", VersionArg.parse("server", "fabric", "1.21.1").toString());
        assertEquals("server-1.21.1", VersionArg.parse("server", "1.21.1").toString("-"));
    }

    @Test
    void parseAcceptsListsAndVarargs() {
        assertEquals(VersionArg.parse("fabric", "1.21.1"), VersionArg.parse(List.of("fabric", "1.21.1")));
    }

    @Test
    void theBuilderHasEmptyDefaults() {
        VersionArg arg = VersionArg.builder().version("1.21.1").onSide(null).withBuild(null).build();

        assertTrue(arg.isAnySide());
        assertTrue(arg.isAnyBuild());
        assertEquals(Optional.of(Side.CLIENT), VersionArg.builder()
            .version("1.21.1").onSide(Side.CLIENT).withBuild("0.16.9").build().side());
    }

}
