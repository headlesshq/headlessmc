package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionProcessor;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * This service handles the {@link VersionMatcher}s of all {@link Platform}s.
 * It parses {@link VersionID}s from {@link Version}s
 * and allows you to resolve a {@link VersionID} to a {@link Version}.
 * (see the documentation of {@link VersionMatcher}).
 *
 * @see VersionMatcher
 */
public interface VersionMatcherService {
    /**
     * Finds all {@link VersionID}s that match the given Version,
     * by calling {@link VersionMatcher#match(PlatformService, Version, VersionProcessor)}
     * for all platforms.
     *
     * @param version   the version to match.
     * @param processor used to resolve parent versions if necessary.
     * @return a set of all {@link VersionID}s that match the given version.
     * @throws HeadlessMcException if something goes wrong.
     */
    Set<VersionID> match(Version version, VersionProcessor processor)
        throws HeadlessMcException;

    /**
     * The reverse of {@link #match(Version, VersionProcessor)},
     * attempts to find a {@link Version} in a list of versions
     * that match the given {@link VersionID}.
     * Also resolves the given {@link VersionID} if it e.g. points
     * to the {@code latest} build instead of specifying a certain build.
     * The {@link VersionID#getSide()} of the given id is ignored,
     * as installed versions are not bound to a side.
     *
     * @param id        the id of the version to match.
     * @param versions  the versions to search through.
     * @param processor used to resolve parent versions if necessary.
     * @return a {@link MatchResult} containing a resolved {@link VersionID}
     * and a {@link Version} or none if no version matched.
     * @throws HeadlessMcException if something goes wrong.
     */
    Optional<MatchResult> match(VersionID id, Collection<Version> versions, VersionProcessor processor)
        throws HeadlessMcException;

    /**
     * The result of {@link #match(VersionID, Collection, VersionProcessor)}.
     * Contains the resolved {@link VersionID}
     * (e.g. if the given VersionID pointed to the {@code latest} version).
     *
     * @param resolvedId the resolved {@link VersionID}.
     * @param version    the version that was matched.
     */
    record MatchResult(VersionID resolvedId, Version version) {}

}
