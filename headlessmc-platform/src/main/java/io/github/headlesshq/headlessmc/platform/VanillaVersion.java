package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.exceptions.BadArgumentException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public final class VanillaVersion extends PlatformVersion {
    VanillaVersion(String name) {
        super(Vanilla.PLATFORM_NAME, name);
    }

    public static VanillaVersion resolve(
        VanillaVersionService vanillaVersionService,
        String name
    ) throws HeadlessMcException {
        if (VersionArg.LATEST.equalsIgnoreCase(name)) {
            return vanillaVersionService.getLatest();
        }

        if (vanillaVersionService.hasVersion(name)) {
            return new VanillaVersion(name);
        }

        throw new BadArgumentException("Failed to find vanilla version " + name);
    }

}
