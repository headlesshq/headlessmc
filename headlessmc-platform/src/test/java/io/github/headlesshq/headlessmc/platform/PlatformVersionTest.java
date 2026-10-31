package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.exceptions.BadArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"AssertBetweenInconvertibleTypes"})
class PlatformVersionTest {
    @Test
    @DisplayName("VanillaVersion.toString() should return only the version name")
    void vanillaToStringShouldReturnOnlyName() {
        VanillaVersion version = new VanillaVersion("1.20.1");
        assertEquals("1.20.1", version.toString());
    }

    @Test
    @DisplayName("BoundPlatformVersion.toString() should return platform/name")
    void boundPlatformToStringShouldReturnPlatformAndName() {
        BoundPlatformVersion version = new BoundPlatformVersion("paper", "1.20.1", "build-123");
        assertEquals("paper/1.20.1/build-123", version.toString());
    }

    @Test
    @DisplayName("VanillaVersion equality: same values should be equal")
    void vanillaVersionsWithSameValuesShouldBeEqual() {
        VanillaVersion a = new VanillaVersion("1.20.1");
        VanillaVersion b = new VanillaVersion("1.20.1");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("VanillaVersion equality: different names should not be equal")
    void vanillaVersionsWithDifferentValuesShouldNotBeEqual() {
        VanillaVersion a = new VanillaVersion("1.20.1");
        VanillaVersion b = new VanillaVersion("1.20.2");

        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("BoundPlatformVersion equality: same values should be equal")
    void boundPlatformVersionsWithSameValuesShouldBeEqual() {
        BoundPlatformVersion a = new BoundPlatformVersion("paper", "1.20.1", "build-123");
        BoundPlatformVersion b = new BoundPlatformVersion("paper", "1.20.1", "build-123");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("BoundPlatformVersion equality: different vanillaVersion should not be equal")
    void boundPlatformVersionsWithDifferentVanillaVersionShouldNotBeEqual() {
        BoundPlatformVersion a = new BoundPlatformVersion("paper", "1.20.1", "build-123");
        BoundPlatformVersion b = new BoundPlatformVersion("paper", "1.20.2", "build-123");

        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("BoundPlatformVersion equality: different build name should not be equal")
    void boundPlatformVersionsWithDifferentBuildNameShouldNotBeEqual() {
        BoundPlatformVersion a = new BoundPlatformVersion("paper", "1.20.1", "build-123");
        BoundPlatformVersion b = new BoundPlatformVersion("paper", "1.20.1", "build-124");

        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("VanillaVersion should never be equal to BoundPlatformVersion")
    void vanillaAndBoundPlatformShouldNeverBeEqual() {
        VanillaVersion vanilla = new VanillaVersion("1.20.1");
        BoundPlatformVersion bound = new BoundPlatformVersion("Vanilla", "1.20.1", "1.20.1");

        assertNotEquals(vanilla, bound);
        assertNotEquals(bound, vanilla);
    }

    @Test
    @DisplayName("PlatformVersion base instance should not equal subclass instance")
    void baseAndSubclassShouldNotBeEqual() {
        PlatformVersion base = new PlatformVersion("paper", "build-123");
        BoundPlatformVersion bound = new BoundPlatformVersion("paper", "1.20.1", "build-123");

        assertNotEquals(base, bound);
        assertNotEquals(bound, base);
    }

    @Test
    @DisplayName("Equality should be reflexive")
    void equalityShouldBeReflexive() {
        VanillaVersion vanilla = new VanillaVersion("1.20.1");
        BoundPlatformVersion bound = new BoundPlatformVersion("paper", "1.20.1", "build-123");

        assertEquals(vanilla, vanilla);
        assertEquals(bound, bound);
    }

    @Test
    @DisplayName("Equality should handle null correctly")
    void equalityShouldHandleNull() {
        VanillaVersion vanilla = new VanillaVersion("1.20.1");
        assertNotEquals(null, vanilla);
    }

    @Test
    @DisplayName("Different runtime classes should never compare equal even with similar field values")
    void differentRuntimeClassesShouldNeverCompareEqual() {
        VanillaVersion vanilla = new VanillaVersion("1.20.1");
        PlatformVersion base = new PlatformVersion(Vanilla.PLATFORM_NAME, "1.20.1");
        BoundPlatformVersion bound = new BoundPlatformVersion(Vanilla.PLATFORM_NAME, "1.20.1", "1.20.1");

        assertNotEquals(vanilla, base);
        assertNotEquals(base, vanilla);
        assertNotEquals(bound, vanilla);
        assertNotEquals(vanilla, bound);
        assertNotEquals(bound, base);
        assertNotEquals(base, bound);
    }

    @Test
    @DisplayName("hashCode should be consistent for equal VanillaVersion instances")
    void hashCodeShouldBeConsistentForEqualVanillaVersions() {
        VanillaVersion a = new VanillaVersion("1.20.1");
        VanillaVersion b = new VanillaVersion("1.20.1");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("hashCode should be consistent for equal BoundPlatformVersion instances")
    void hashCodeShouldBeConsistentForEqualBoundPlatformVersions() {
        BoundPlatformVersion a = new BoundPlatformVersion("paper", "1.20.1", "build-123");
        BoundPlatformVersion b = new BoundPlatformVersion("paper", "1.20.1", "build-123");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("VanillaVersion.resolve should resolve latest to the latest version and stable to the latest release")
    void vanillaVersionShouldResolveLatestAndStable() {
        FakeVanillaVersionService service = new FakeVanillaVersionService("1.21.2-pre1", "1.21.1", "1.21")
            .withReleases("1.21.1", "1.21");

        assertEquals(new VanillaVersion("1.21.2-pre1"), VanillaVersion.resolve(service, "latest"));
        assertEquals(new VanillaVersion("1.21.1"), VanillaVersion.resolve(service, "stable"));
        assertEquals(new VanillaVersion("1.21"), VanillaVersion.resolve(service, "1.21"));
    }

    @Test
    @DisplayName("PlatformVersion.resolve should resolve latest and stable builds")
    void platformVersionShouldResolveLatestAndStable() {
        FakeVersionService service = new FakeVersionService("forge")
            .withBuilds("1.21.1", "52.1.21", "52.1.20", "52.1.19")
            .withStableBuilds("1.21.1", "52.1.20", "52.1.19");
        VanillaVersion vanilla = new VanillaVersion("1.21.1");

        assertEquals("52.1.21", PlatformVersion.resolve(service, vanilla, "latest").getName());
        assertEquals("52.1.20", PlatformVersion.resolve(service, vanilla, "stable").getName());
        assertEquals("52.1.19", PlatformVersion.resolve(service, vanilla, "52.1.19").getName());
    }

    @Test
    @DisplayName("PlatformVersion.resolve should throw if there is no stable build")
    void platformVersionShouldThrowWithoutStableBuild() {
        FakeVersionService service = new FakeVersionService("forge").withBuilds("1.21.1", "52.1.21");

        assertThrows(
            BadArgumentException.class,
            () -> PlatformVersion.resolve(service, new VanillaVersion("1.21.1"), "stable")
        );
    }

}
