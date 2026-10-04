package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Finds a {@link Profile} for a arguments passed to a command.
 * If the args specify a version e.g. {@code "fabric 26.1"},
 * a default profile ({@link ProfileService#getProfile(VersionArg)})
 * is returned.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ProfileResolver {
    private final PlatformService platformService;
    private final ProfileService profileService;

    public Profile resolve(List<String> args, Set<Side> sides) {
        Profile profile = null;
        if (args.size() == 1) {
            profile = profileService.getProfile(args.getFirst()).orElse(null);
        }

        if (profile != null) {
            if (!sides.contains(profile.side())) {
                throw new IllegalArgumentException(
                    "Found profile for name " + args.getFirst() + " but it was for "
                        + profile.side().toString().toLowerCase(Locale.ENGLISH)
                );
            }

            // TODO: log feedback to console?
            return profile;
        }

        if (!sides.contains(Side.CLIENT)) {
            throw new IllegalArgumentException("Failed to find profile/server <" + String.join(" ", args) + ">");
        }

        // TODO: log feedback to console?
        VersionArg versionArg = VersionArg.parse(args);
        if (sides.size() == 1) {
            versionArg = versionArg.withSide(sides.iterator().next());
        }

        VersionID id = VersionID.resolve(platformService, versionArg);
        return profileService.getProfile(id.asArg());
    }

}
