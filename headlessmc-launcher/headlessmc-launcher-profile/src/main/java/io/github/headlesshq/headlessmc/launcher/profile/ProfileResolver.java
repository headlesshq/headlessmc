package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Finds the {@link Profile} the arguments of a command refer to:
 * <ol>
 *     <li>If no launch target is named and there is a single argument that is the name of a stored profile,
 *     that profile. A stored profile always wins over everything else its name could mean.</li>
 *     <li>Otherwise the arguments are given to the {@link LaunchTargetService}, to find the launch target
 *     that can launch them, e.g. {@code fabric 1.21.1}. If a profile for exactly that is stored,
 *     it is returned.</li>
 *     <li>Otherwise a new profile is created for the arguments, which is not stored.
 *     Commands that only work with servers do not get such a profile, a server has to be added first.</li>
 * </ol>
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ProfileResolver {
    private final ProfileService profileService;
    private final LaunchTargetService launchTargetService;

    /**
     * Finds a profile for arguments, detecting the launch target.
     *
     * @param args  the arguments, the name of a profile or arguments for a launch target.
     * @param sides the sides the command allows.
     * @return the profile for the arguments, it may not be stored.
     * @throws IllegalArgumentException if no profile can be found or created for the arguments.
     */
    public Profile resolve(List<String> args, Set<Side> sides) throws IllegalArgumentException {
        return resolve(null, args, sides, new TypedMapImpl());
    }

    /**
     * Finds a profile for arguments.
     *
     * @param target  the name of the launch target the arguments are for, {@code null} to detect it.
     * @param args    the arguments, the name of a profile or arguments for a launch target.
     * @param sides   the sides the command allows.
     * @param options options for creating a profile, see {@link LaunchTarget#create}.
     * @return the profile for the arguments, it may not be stored.
     * @throws IllegalArgumentException if no profile can be found or created for the arguments.
     */
    public Profile resolve(@Nullable String target, List<String> args, Set<Side> sides, TypedMap options)
        throws IllegalArgumentException {
        TargetArgs targetArgs = new TargetArgs(args, sides);
        if (target == null && args.size() == 1) {
            Profile profile = profileService.getProfile(args.getFirst()).orElse(null);
            if (profile != null) {
                if (!targetArgs.allows(profile.side())) {
                    throw new IllegalArgumentException(
                        "Found profile for name " + args.getFirst() + " but it was for " + describe(profile)
                    );
                }

                return profile;
            }
        }

        if (args.isEmpty()) {
            throw new IllegalArgumentException("Please specify a profile or what to launch.");
        }

        LaunchTargetService.TargetMatch match;
        try {
            match = launchTargetService.parse(target, targetArgs);
        } catch (IllegalArgumentException e) {
            if (!sides.contains(Side.CLIENT)) {
                throw new IllegalArgumentException("Failed to find profile/server <" + String.join(" ", args) + ">", e);
            }

            if (target == null && args.size() == 1) {
                throw new IllegalArgumentException(
                    "Failed to find profile '" + args.getFirst() + "'. " + e.getMessage(), e
                );
            }

            throw e;
        }

        Optional<Profile> stored = launchTargetService.findProfile(match);
        if (stored.isPresent()) {
            return stored.get();
        }

        // a server has to be installed into a directory before it can be used, it is not created on the fly
        if (!sides.contains(Side.CLIENT)) {
            throw new IllegalArgumentException("Failed to find profile/server <" + String.join(" ", args) + ">");
        }

        return launchTargetService.createTransient(match, options);
    }

    /**
     * @param profile a profile.
     * @return the Minecraft version the profile launches, see {@link LaunchTargetService#requireVersion(Profile)}.
     * @throws IllegalArgumentException if the Minecraft version of the profile is not known.
     */
    public VersionArg requireVersion(Profile profile) throws IllegalArgumentException {
        return launchTargetService.requireVersion(profile);
    }

    /**
     * @param profile a profile.
     * @return the platform of the version the profile launches, see {@link LaunchTargetService#requirePlatform(Profile)}.
     * @throws IllegalArgumentException if the profile does not launch a version.
     */
    public String requirePlatform(Profile profile) throws IllegalArgumentException {
        return launchTargetService.requirePlatform(profile);
    }

    private static String describe(Profile profile) {
        return profile.side()
            .map(side -> side.isServer() ? "server" : "client")
            .orElse("neither client nor server");
    }

}
