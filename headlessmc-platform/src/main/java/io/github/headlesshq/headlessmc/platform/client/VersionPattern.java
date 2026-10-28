package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.Version;

import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * A format in which a {@link Platform} names the versions it installs,
 * e.g. {@code fabric-loader-<loader>-<mc-version>} for Fabric.
 * The {@link #pattern()} has to match the entire {@link Version#getId()},
 * its groups are then passed to the {@link #resolver()}.
 *
 * @param pattern  the pattern a version id has to match exactly.
 * @param resolver resolves the groups of the pattern to a {@link VersionID}.
 * @see PatternVersionMatcher
 */
public record VersionPattern(Pattern pattern, Resolver resolver) {
    /**
     * Resolves the groups a {@link VersionPattern} matched to a {@link VersionID}.
     */
    @FunctionalInterface
    public interface Resolver {
        /**
         * @param platformService used to resolve versions.
         * @param version         the version whose id matched the pattern.
         * @param match           the result of matching the pattern against the version id.
         * @return the {@link VersionID} of the version, or none if the matched groups
         * do not name a version that exists on the platform.
         * @throws HeadlessMcException if something goes wrong.
         */
        Optional<VersionID> resolve(PlatformService platformService, Version version, MatchResult match)
            throws HeadlessMcException;
    }

}
