package io.github.headlesshq.headlessmc.platform;

public abstract class AbstractVersionService implements VersionService {

    // accessors to the package-private constructors of VanillaVersion/PlatformVersion/BoundPlatformVersion
    // this is to allow VersionServices to make authorized instances of these classes

    protected final VanillaVersion createVanillaVersion(String name) {
        if (!Vanilla.PLATFORM_NAME.equals(getPlatformName())) {
            throw new IllegalArgumentException("Platform " + getPlatformName() + " cannot create vanilla versions");
        }

        return new VanillaVersion(name);
    }

    protected final PlatformVersion createVersion(String name) {
        return new PlatformVersion(getPlatformName(), name);
    }

    protected final BoundPlatformVersion createVersion(String name, String vanillaName) {
        return new BoundPlatformVersion(getPlatformName(), vanillaName, name);
    }

}
