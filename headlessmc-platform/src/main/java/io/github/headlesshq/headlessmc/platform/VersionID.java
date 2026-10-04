package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.client.VersionMatchException;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * A VersionID identifies a launchable mc version.
 * It is a type-safe version of {@link VersionArg}.
 * It specifies the name of the vanilla version,
 * potentially the mod-loading platform that it
 * runs on, the build of the mod-loading platform
 * and whether it runs the server or client.
 * Examples:
 * <p>{@code "1.21.1"}
 * <p>{@code "vanilla/1.21.1"}
 * <p>{@code "fabric/1.21.1"}
 * <p>{@code "server/fabric/1.21.1"}
 * <p>{@code "client/fabric/1.21.1"}
 * <p>{@code "fabric/1.21.1/0.18.1"}
 * <p>{@code "server/fabric/1.21.1/0.18.1"}
 *
 * <p>A VersionID serves as a way to verify and
 * safely work with {@link VersionArg}s.
 * A VersionID can only be instantiated
 * by a {@link Platform} that owns it, or
 * via {@link #resolve(PlatformService, VersionArg)}
 * by getting an existing {@link Platform} for
 * {@link VersionArg#platform()} and resolving the
 * {@link VersionArg#version()} against the vanilla
 * {@link VersionService} and {@link VersionArg#build()}
 * against the VersionService of the Platform.
 *
 * @see VersionArg
 */
@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class VersionID {
    private final Optional<Side> side;
    private final Platform platform;
    private final VanillaVersion version;
    private final Optional<PlatformVersion> build;
    @Getter(AccessLevel.NONE)
    private final VanillaPlatform vanillaPlatform;

    public VersionArg asArg() {
        return new VersionArg(side, platform.getName(), version.getName(), build.map(PlatformVersion::getName));
    }

    @Override
    public String toString() {
        return asArg().toString();
    }

    /**
     * @return {@code true} if no {@link Side} has been specified.
     */
    public boolean isAnySide() {
        return side.isEmpty();
    }

    /**
     * @return {@code true} if this VersionID targets the {@code "vanilla"} platform.
     */
    public boolean isVanilla() {
        return Vanilla.PLATFORM_NAME.equalsIgnoreCase(platform.getName());
    }

    /**
     * @return {@code true} if this VersionArg targets any build (latest installed build).
     */
    public boolean isAnyBuild() {
        return build.isEmpty();
    }

    /**
     * Checks if the given VersionID targets the same
     * platform and mc-version as this VersionID.
     *
     * @param other the VersionID to check.
     * @return {@code true} if the given VersionID targets the same
     * platform and mc-version as this VersionArg.
     */
    public boolean isSamePlatformVersion(VersionID other) {
        return platform.getName().equals(other.getPlatform().getName()) && version.equals(other.getVersion());
    }

    /**
     * @return the vanilla version for this version.
     */
    public VersionID asVanillaVersion() {
        return new VersionID(side, vanillaPlatform, version, Optional.empty(), vanillaPlatform);
    }

    /**
     * Resolves the given {@link VersionArg} against the given {@link PlatformService}.
     * Checks if it can find Platform, Version and Build of the arg.
     *
     * @param platformService the platform service to get platforms from.
     * @param id              the raw arg to resolve.
     * @return a resolved VersionID.
     */
    public static VersionID resolve(PlatformService platformService, VersionArg id) {
        Platform platform = platformService.getPlatform(id.platform())
            .orElseThrow(() -> new IllegalArgumentException(
                "Failed to find platform " + id.platform() + " for id " + id)
            );

        try {
            VanillaPlatform vanillaPlatform = platformService.getVanillaPlatform();
            VanillaVersion version = VanillaVersion.resolve(vanillaPlatform.getVersionService(), id.version());
            Optional<PlatformVersion> build = id.build()
                .map(name -> PlatformVersion.resolve(platform.getVersionService(), version, name));

            return new VersionID(id.side(), platform, version, build, vanillaPlatform);
        } catch (HeadlessMcException e) {
            throw new PlatformException("Failed to resolve version " + id, e);
        }
    }

}
