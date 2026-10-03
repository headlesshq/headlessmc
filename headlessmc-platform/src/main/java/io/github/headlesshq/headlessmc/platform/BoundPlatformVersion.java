package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.exceptions.BadArgumentException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public final class BoundPlatformVersion extends PlatformVersion {
    private final String vanillaVersion;

    BoundPlatformVersion(String platformName, String vanillaVersion, String name) {
        super(platformName, name);
        this.vanillaVersion = vanillaVersion;
    }

    @Override
    public String toString() {
        return getPlatformName() + "/" + getVanillaVersion() + "/" + getName();
    }

    public VanillaVersion getVanillaVersion(VanillaVersionService versionService) throws BadArgumentException {
        return versionService.getVersion(vanillaVersion)
            .orElseThrow(() -> new BadArgumentException(
                "Failed to find vanilla version " + vanillaVersion + " for bound platform version " + this
            ));
    }

}
