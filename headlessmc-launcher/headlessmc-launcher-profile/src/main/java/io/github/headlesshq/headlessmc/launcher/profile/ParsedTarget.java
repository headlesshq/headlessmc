package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.version.arg.Side;

import java.util.Optional;

/**
 * The result of {@link LaunchTarget#parse(TargetArgs)}: arguments a launch target was able to resolve.
 *
 * @param handle   the handle for the arguments, not resolved yet.
 * @param side     the {@link Profile#side() side} a profile created for the arguments gets.
 *                 Empty if what is launched is neither a client nor a server.
 * @param fallback {@code true} if the target can launch the arguments, but they are not what it is primarily for.
 *                 When the launch target for arguments is detected, a fallback is only used if no other target
 *                 can launch the arguments, see {@link LaunchTargetService#parse(String, TargetArgs)}.
 *                 E.g. {@code 1.21.1} is a Minecraft version, and only then the id of an installed version.json.
 *                 If the target has been named explicitly this makes no difference.
 */
public record ParsedTarget(LaunchTargetHandle handle, Optional<Side> side, boolean fallback) {
    /**
     * Arguments the target is primarily for.
     *
     * @param handle the handle for the arguments, not resolved yet.
     * @param side   the {@link Profile#side() side} a profile created for the arguments gets.
     */
    public ParsedTarget(LaunchTargetHandle handle, Optional<Side> side) {
        this(handle, side, false);
    }

}
