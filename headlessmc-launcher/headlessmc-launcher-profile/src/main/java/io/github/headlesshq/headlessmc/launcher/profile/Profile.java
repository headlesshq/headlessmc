package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.With;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;

/**
 * A profile describes something that can be launched, and how it is launched.
 *
 * @param name                    the name of the profile, also the name of its file.
 * @param side                    whether this profile launches a client or a server.
 *                                It is decided once, by the {@link LaunchTarget} when the profile is created,
 *                                and never changes afterwards.
 *                                It is empty for profiles that are neither, e.g. a profile for an arbitrary jar.
 *                                An empty side is a state of its own, it must never be treated as client.
 * @param target                  what this profile launches, see {@link LaunchTarget}.
 * @param path                    the game directory. It is decided when the profile is created and only
 *                                changed by moving the profile, see {@link ProfileMover}.
 * @param options                 options for launching clients.
 * @param patchers                the names of the patchers to apply.
 * @param systemProperties        system properties for the launched JVM.
 * @param vmArgs                  arguments for the launched JVM.
 * @param gameArgs                arguments for the launched game.
 * @param javaVersion             the Java version to launch with, {@code null} to use the one required by the version.
 * @param eulaStatus              whether the server of this profile has a eula.txt.
 * @param hasDefaultClientJvmArgs whether the default jvm arguments of the version are already part of {@link #vmArgs()}.
 * @param hmcVersion              the {@link #PROFILE_VERSION format} this profile has been read in.
 */
@With
@RegisterForReflection
public record Profile(
    String name,
    Optional<Side> side,
    LaunchTargetHandle target,
    Path path,
    LaunchOptions options,
    List<String> patchers,
    SequencedMap<String, @Nullable String> systemProperties,
    List<String> vmArgs,
    @Nullable List<String> gameArgs,
    @Nullable Integer javaVersion,
    // TODO: @Nullable String account,
    EulaStatus eulaStatus,
    boolean hasDefaultClientJvmArgs,
    // TODO: library overrides?
    // mods?, resourcepacks? etc?
    // TODO: config? HeadlessMc config to set? specify java?
    // override template strings?
    int hmcVersion // versionIdentifier to check if we are outdated,
) implements ReflectionRegistered {
    /**
     * The current profile format version, older HeadlessMc versions ignore profiles with a newer version.
     * <ul>
     *     <li>0: initial format.</li>
     *     <li>1: {@link #path()} is written as a relative path object instead of a plain string.</li>
     *     <li>2: {@code version} and {@code currentVersion} have been replaced by {@link #side()} and
     *     {@link #target()}. Profiles of older formats are converted when they are read,
     *     see {@link LegacyProfile}.</li>
     * </ul>
     */
    public static final int PROFILE_VERSION = 2;

    /**
     * Validates everything that must not be missing in a profile file,
     * so that a broken file is reported when it is read, and not somewhere during a launch.
     */
    @SuppressWarnings({"OptionalAssignedToNull", "ConstantValue", "OptionalUsedAsFieldOrParameterType"})
    public Profile {
        Objects.requireNonNull(name, "Profile is missing 'name'");
        Objects.requireNonNull(target, "Profile " + name + " is missing 'target'");
        Objects.requireNonNull(path, "Profile " + name + " is missing 'path'");
        // a missing side and "side": null both mean that the profile has no side
        side = side == null ? Optional.empty() : side;
        options = options == null ? LaunchOptions.empty() : options;
        patchers = patchers == null ? List.of() : patchers;
        systemProperties = systemProperties == null ? new LinkedHashMap<>() : systemProperties;
        vmArgs = vmArgs == null ? List.of() : vmArgs;
        eulaStatus = eulaStatus == null ? EulaStatus.UNKNOWN : eulaStatus;
    }

    /**
     * Creates a new profile with default settings.
     *
     * @param name   the name of the profile.
     * @param side   the side of the profile, empty if it is neither client nor server.
     * @param target what the profile launches.
     * @param path   the game directory.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public Profile(String name, Optional<Side> side, LaunchTargetHandle target, Path path) {
        this(
            name,
            side,
            target,
            path,
            LaunchOptions.empty(),
            List.of(),
            new LinkedHashMap<>(),
            List.of(),
            List.of(),
            null,
            EulaStatus.UNKNOWN,
            false,
            Profile.PROFILE_VERSION
        );
    }

    /**
     * Creates a new profile for a version, with the {@value VersionHandle#VERSION} target for clients,
     * or the {@value VersionHandle#SERVER} target if the version is for {@link Side#SERVER}.
     * The profile is not resolved yet.
     *
     * @param name    the name of the profile.
     * @param version the version to launch, without a side it is a client.
     * @param path    the game directory.
     */
    public Profile(String name, VersionArg version, Path path) {
        this(
            name,
            Optional.of(version.side().orElse(Side.CLIENT)),
            VersionHandle.of(version).asTarget(),
            path,
            LaunchOptions.empty(),
            List.of(),
            new LinkedHashMap<>(),
            List.of(),
            // if server doesn't display a gui by default, maybe to-do --ui flag in launch command to remove this arg?
            Side.SERVER.equals(version.side().orElse(Side.CLIENT)) ? List.of("nogui") : List.of(),
            null,
            EulaStatus.UNKNOWN,
            false,
            Profile.PROFILE_VERSION
        );
    }

    /**
     * Whether this profile launches a client.
     * A profile without a side is not a client.
     *
     * @return {@code true} if {@link #side()} is {@link Side#CLIENT}.
     */
    public boolean isClient() {
        return side.filter(Side::isClient).isPresent();
    }

    /**
     * Whether this profile launches a server.
     * A profile without a side is not a server.
     *
     * @return {@code true} if {@link #side()} is {@link Side#SERVER}.
     */
    public boolean isServer() {
        return side.filter(Side::isServer).isPresent();
    }

}
