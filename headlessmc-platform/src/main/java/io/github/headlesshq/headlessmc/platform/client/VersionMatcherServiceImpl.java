package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Default implementation of {@link VersionMatcherService}.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class VersionMatcherServiceImpl implements VersionMatcherService {
    private final PlatformService platforms;

    @Override
    public Set<VersionID> match(Version version, VersionProcessor processor) throws HeadlessMcException {
        VersionMatchException exception = new VersionMatchException(
            "Failed to parse version %s (%s)".formatted(version.getId(), version.getInheritsFrom())
        );

        List<Platform> matchingPlatforms = getMatchingPlatforms(exception, version, processor);
        Set<VersionID> result = new HashSet<>();
        for (Platform platform : matchingPlatforms) {
            try {
                Optional<VersionMatcher> matcher = platform.getClientSupport().map(ClientSupport::versionMatcher);
                matcher.ifPresent(m -> result.add(m.match(platforms, version, processor)));
            } catch (HeadlessMcException e) {
                exception.addSuppressed(e);
            }
        }

        if (result.isEmpty()) {
            throw exception;
        }

        return result;
    }

    @Override
    public Optional<MatchResult> match(
        VersionID id,
        Collection<Version> versions,
        VersionProcessor processor
    ) throws HeadlessMcException {
        if (id.isAnyBuild()) {
            SequencedMap<VersionID, Version> ids = new LinkedHashMap<>();
            for (Version version : versions) {
                Set<VersionID> matches = matchOrEmpty(version, processor);
                for (VersionID match : matches) {
                    if (match.isSamePlatformVersion(id)) {
                        Version previous = ids.put(match, version);
                        if (previous != null) {
                            log.warn("{} matches {} and {}", match, previous.getId(), version.getId());
                        }
                    }
                }
            }

            Map.Entry<VersionID, Version> result = ids.firstEntry();
            if (result != null) {
                return Optional.of(new MatchResult(result.getKey(), result.getValue()));
            }
        } else {
            for (Version version : versions) {
                Set<VersionID> ids = matchOrEmpty(version, processor);
                if (ids.stream().anyMatch(match -> isSameVersionIgnoringSide(match, id))) {
                    return Optional.of(new MatchResult(id, version));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Installed versions are not bound to a side, so the {@link VersionID}s matched from them never have one.
     * A side of the VersionID we are looking for must therefore be ignored.
     */
    private boolean isSameVersionIgnoringSide(VersionID match, VersionID id) {
        return match.isSamePlatformVersion(id) && match.getBuild().equals(id.getBuild());
    }

    // TODO: check?
    /**
     * Like {@link #match(Version, VersionProcessor)}, but returns an empty set for versions no platform can match.
     * When searching through versions, e.g. all installed versions, one we do not understand
     * (installed by another launcher, or by a platform that is not available) should not abort the search.
     */
    private Set<VersionID> matchOrEmpty(Version version, VersionProcessor processor) throws HeadlessMcException {
        try {
            return match(version, processor);
        } catch (VersionMatchException e) {
            log.error("Skipping version {}", version.getId(), e);
            return Set.of();
        }
    }

    private List<Platform> getMatchingPlatforms(
        VersionMatchException exception,
        Version version,
        VersionProcessor processor
    ) throws HeadlessMcException {
        List<Platform> allMatches = new ArrayList<>();
        for (Platform platform : platforms.getPlatforms()) {
            try {
                Optional<VersionMatcher> matcher = platform.getClientSupport().map(ClientSupport::versionMatcher);
                if (matcher.isPresent() && matcher.get().canMatch(platforms, version, processor)) {
                    allMatches.add(platform);
                }
            } catch (HeadlessMcException e) {
                exception.addSuppressed(e);
            }
        }

        if (allMatches.isEmpty() && exception.getSuppressed().length > 0) {
            throw exception;
        }

        return solveConflicts(exception, allMatches, version);
    }

    private List<Platform> solveConflicts(VersionMatchException exception, List<Platform> allMatches, Version version) {
        List<Platform> matchingPlatforms;
        if (allMatches.isEmpty()) {
            exception.addSuppressed(new VersionMatchException("Failed to find platform for " + version.getId()));
            return List.of();
        } else if (allMatches.size() == 1) {
            matchingPlatforms = allMatches;
        } else {
            matchingPlatforms = allMatches.stream()
                .filter(platform -> allMatches.stream() // no otherPlatform includes this platform
                    .filter(otherPlatform -> otherPlatform != platform)
                    .noneMatch(otherPlatform -> {
                        Optional<VersionMatcher> matcher = otherPlatform.getClientSupport()
                            .map(ClientSupport::versionMatcher);
                        return matcher.isPresent() && matcher.get().includes(platforms, version, platform);
                    })
                ).toList();
        }

        return matchingPlatforms;
    }

}
