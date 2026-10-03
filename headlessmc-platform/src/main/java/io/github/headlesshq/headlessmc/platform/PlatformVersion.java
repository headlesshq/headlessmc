package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.exceptions.BadArgumentException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Represents a version of mc ({@link VanillaVersion} or
 * a build of a platform, for a mc version ({@link BoundPlatformVersion}),
 * imagine e.g. {@code 1.12.2}, or a build like Fabric {@code 0.19.1},
 * or a build bound to a Mc version, like Forge {@code 10.12.2.1121},
 * which is bound to Mc version {@code 1.7.2}.
 * PlatformVersions have a package-private constructor and can
 * only be instantiated from the outside via
 * {@link #resolve(VersionService, VanillaVersion, String)},
 * guaranteeing, that if you have a PlatformVersion, that version
 * exists and was handed out by a corresponding {@link VersionService}.
 */
@Data
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public sealed class PlatformVersion permits VanillaVersion, BoundPlatformVersion {
    private final String platformName;
    private final String name;

    @Override
    public String toString() {
        return Vanilla.PLATFORM_NAME.equalsIgnoreCase(platformName)
            ? name
            : platformName + "/" + name;
    }

    public static PlatformVersion resolve(
        VersionService versionService, VanillaVersion vanillaVersion, String name
    ) throws HeadlessMcException {
        Optional<? extends PlatformVersion> platformVersion = VersionArg.LATEST.equalsIgnoreCase(name)
            ? versionService.getLatestBuild(vanillaVersion)
            : versionService.getBuild(vanillaVersion, name);

        return platformVersion.orElseThrow(() -> new BadArgumentException(
            "Failed to find version %s/%s/%s".formatted(
                versionService.getPlatformName(),
                vanillaVersion,
                name
            )
        ));
    }

}
