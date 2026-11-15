package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.version.arg.VersionArg;

import java.nio.file.Path;
import java.util.Optional;

/**
 * What is known about the {@link LaunchTarget} of a {@link Profile} without resolving it,
 * see {@link LaunchTarget#describe(Profile)}.
 * Everything is optional: a target does not have to launch a Minecraft version at all,
 * and a target that has not been resolved yet does not know what it resolves to.
 *
 * @param specified the version that has been specified for the profile, e.g. {@code fabric/26.3/latest}.
 * @param current   the version the profile currently launches, e.g. {@code fabric/26.3/0.19.5}.
 *                  Empty until the profile has been resolved.
 * @param file      the file the profile is pinned to, e.g. a version.json or a server jar.
 */
public record TargetInfo(Optional<VersionArg> specified, Optional<VersionArg> current, Optional<Path> file) {
    /**
     * Nothing is known about the target.
     */
    public static final TargetInfo EMPTY = new TargetInfo(Optional.empty(), Optional.empty(), Optional.empty());

    /**
     * The best known version of the target: the current version if the target has been resolved,
     * otherwise the specified version.
     * The specified version can be vague, e.g. {@code fabric/latest}, check {@link #isVersionKnown()}
     * before relying on the Minecraft version.
     *
     * @return the current version, or else the specified version.
     */
    public Optional<VersionArg> version() {
        return current.or(() -> specified);
    }

    /**
     * @return {@code true} if {@link #version()} names a concrete Minecraft version,
     * and not {@code latest} or {@code stable}.
     */
    public boolean isVersionKnown() {
        return version().filter(TargetInfo::isKnown).isPresent();
    }

    /**
     * @param version a version.
     * @return {@code true} if the version names a concrete Minecraft version, and not {@code latest} or {@code stable},
     * which are something else tomorrow.
     */
    public static boolean isKnown(VersionArg version) {
        return !VersionArg.LATEST.equalsIgnoreCase(version.version())
            && !VersionArg.STABLE.equalsIgnoreCase(version.version());
    }

}
