package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.files.cache.CacheBuilder;
import io.github.headlesshq.headlessmc.files.cache.CacheExceptionHandler;
import io.github.headlesshq.headlessmc.files.cache.JsonCacheFile;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionProcessor;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link VersionMatcherService}.
 * Remembers the {@link VersionID}s matched for a {@link Version} in a cache file,
 * as matching can be expensive, e.g. if a platform needs to look up builds.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class VersionMatcherServiceImpl implements VersionMatcherService {
    static final String CACHE_FILE = "version-matches.json";

    private final PlatformService platforms;
    private final Cache<VersionMatches> cache;

    @Inject
    public VersionMatcherServiceImpl(
        PlatformService platforms,
        AppFiles files,
        FileService fileService,
        JsonService jsonService
    ) {
        this(platforms, fileCache(jsonService, fileService.getPath(files.getCacheDir(), CACHE_FILE)));
    }

    /**
     * Creates a VersionMatcherServiceImpl that only remembers matches in memory.
     *
     * @param platforms the platforms to match versions for.
     */
    @VisibleForTesting
    public VersionMatcherServiceImpl(PlatformService platforms) {
        this(platforms, CacheBuilder.<VersionMatches>create()
            .withVersion(0)
            .withInitialValue(new VersionMatches(new ConcurrentHashMap<>()))
            .build()
        );
    }

    @VisibleForTesting
    static Cache<VersionMatches> fileCache(JsonService jsonService, Path file) {
        //noinspection Convert2Diamond
        return CacheBuilder.<VersionMatches>create()
            .withVersion(0)
            .withInitialValue(new VersionMatches(new ConcurrentHashMap<>()))
            .withSourceStore(new JsonCacheFile<VersionMatches>(
                CacheExceptionHandler.logging(),
                jsonService,
                new TypeLiteral<VersionMatches>() {},
                0,
                file
            )).build();
    }

    @Override
    public Set<VersionID> match(Version version, VersionProcessor processor) throws HeadlessMcException {
        Optional<Set<VersionID>> cached = getCached(version);
        if (cached.isPresent()) {
            return cached.get();
        }

        Set<VersionID> result = matchUncached(version, processor);
        VersionMatch match = new VersionMatch(
            version.getInheritsFrom(),
            result.stream().map(VersionID::asArg).sorted(Comparator.comparing(VersionArg::toString)).toList()
        );

        cache.maybeModify(matches -> !match.equals(matches.matches().put(version.getId(), match)));
        return result;
    }

    private Optional<Set<VersionID>> getCached(Version version) throws HeadlessMcException {
        VersionMatch match = cache.get().map(matches -> matches.matches().get(version.getId())).orElse(null);
        if (match == null || !Objects.equals(match.inheritsFrom(), version.getInheritsFrom())) {
            return Optional.empty();
        }

        try {
            Set<VersionID> result = new HashSet<>();
            for (VersionArg id : match.ids()) {
                result.add(VersionID.resolve(platforms, id));
            }

            return result.isEmpty() ? Optional.empty() : Optional.of(result);
        } catch (HeadlessMcException | IllegalArgumentException e) {
            // e.g. a platform that is no longer available, the version will be matched again
            log.debug("Failed to resolve cached matches {} for {}", match.ids(), version.getId(), e);
            return Optional.empty();
        }
    }

    private Set<VersionID> matchUncached(Version version, VersionProcessor processor) throws HeadlessMcException {
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

    /**
     * The contents of the cache file, mapping {@link Version#getId()}s to their {@link VersionMatch}.
     *
     * @param matches the cached matches.
     */
    @RegisterForReflection
    record VersionMatches(Map<String, VersionMatch> matches) implements ReflectionRegistered {
        VersionMatches {
            // matches are read outside the lock of the cache
            matches = new ConcurrentHashMap<>(matches);
        }
    }

    /**
     * The {@link VersionID}s matched for a {@link Version}.
     *
     * @param inheritsFrom {@link Version#getInheritsFrom()}, if it changes the version has been replaced.
     * @param ids          the matched {@link VersionID}s as {@link VersionArg}s.
     */
    @RegisterForReflection
    record VersionMatch(@Nullable String inheritsFrom, List<VersionArg> ids) implements ReflectionRegistered {

    }

}
