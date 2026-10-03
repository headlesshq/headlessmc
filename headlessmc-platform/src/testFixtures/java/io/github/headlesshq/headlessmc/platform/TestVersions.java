package io.github.headlesshq.headlessmc.platform;

/**
 * Test access to the package-private {@link PlatformVersion} constructors.
 */
public final class TestVersions {
    private TestVersions() {
    }

    public static VanillaVersion vanilla(String name) {
        return new VanillaVersion(name);
    }

    public static PlatformVersion version(String platformName, String name) {
        return new PlatformVersion(platformName, name);
    }

    public static BoundPlatformVersion bound(String platformName, String vanillaVersion, String name) {
        return new BoundPlatformVersion(platformName, vanillaVersion, name);
    }

}
