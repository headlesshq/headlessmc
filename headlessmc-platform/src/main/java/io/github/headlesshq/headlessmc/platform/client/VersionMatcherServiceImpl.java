package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.files.cache.CacheBuilder;
import io.github.headlesshq.headlessmc.files.cache.CacheExceptionHandler;
import io.github.headlesshq.headlessmc.files.cache.CacheObject;
import io.github.headlesshq.headlessmc.files.cache.CacheSource;
import io.github.headlesshq.headlessmc.files.cache.CacheStore;
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
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Default implementation of {@link VersionMatcherService}.
 * Remembers the {@link VersionID}s matched for a {@link Version} in a cache file,
 * as matching can be expensive, e.g. if a platform needs to look up builds.
 *
 * <p>A version is not identified by its {@link Version#getId() id} alone: the same id can be installed in
 * several {@code .minecraft} directories, and other launchers name their instances freely, so that a
 * {@code 1.21.1} in one directory is vanilla and a {@code 1.21.1} in another one comes with a mod loader.
 * The cache therefore keeps one match for every {@link Version#asFile() file} an id has been read from.
 * A match is never used for another file than the one it has been made for.</p>
 */
@Slf4j
@ApplicationScoped
public class VersionMatcherServiceImpl implements VersionMatcherService {
    static final String CACHE_FILE = "version-matches.json";
    /**
     * The format of the cache file:
     * <ul>
     *     <li>1: one match for every {@link Version#getId()}, see {@link LegacyVersionMatches}.</li>
     *     <li>2: several matches for every {@link Version#getId()}, one for every file, see {@link VersionMatches}.
     *     A file of format 1 is still read, see {@link LegacyVersionMatches#migrate()}.</li>
     * </ul>
     */
    static final int CACHE_VERSION = 2;
    static final int LEGACY_CACHE_VERSION = 1;

    private final PlatformService platforms;
    private final Cache<VersionMatches> cache;
    /**
     * Whether a match without a file, as read from a cache file of format 1, may be used for a version file.
     */
    private final Predicate<Path> legacyLocation;

    @Inject
    public VersionMatcherServiceImpl(
        PlatformService platforms,
        AppFiles files,
        FileService fileService,
        JsonService jsonService,
        McFilesService mcFilesService
    ) {
        this(
            platforms,
            fileCache(jsonService, fileService.getPath(files.getCacheDir(), CACHE_FILE)),
            // the old cache has been written when the primary directory was the only one
            file -> McFiles.normalize(file).startsWith(McFiles.normalize(mcFilesService.getPrimary().getMcDir()))
        );
    }

    /**
     * Creates a VersionMatcherServiceImpl that only remembers matches in memory.
     *
     * @param platforms the platforms to match versions for.
     */
    @VisibleForTesting
    public VersionMatcherServiceImpl(PlatformService platforms) {
        this(
            platforms,
            CacheBuilder.<VersionMatches>create()
                .withVersion(CACHE_VERSION)
                .withInitialValue(new VersionMatches(new ConcurrentHashMap<>()))
                .build(),
            file -> false
        );
    }

    /**
     * @param platforms      the platforms to match versions for.
     * @param cache          the cache to remember matches in.
     * @param legacyLocation whether a match without a file may be used for the given version file.
     *                       A cache file of format 1 does not say which file a match has been made for.
     *                       It has been written when HeadlessMc only knew one {@code .minecraft} directory,
     *                       so its matches belong to the files in that directory, and to no other file.
     */
    @VisibleForTesting
    VersionMatcherServiceImpl(PlatformService platforms, Cache<VersionMatches> cache, Predicate<Path> legacyLocation) {
        this.platforms = platforms;
        this.cache = cache;
        this.legacyLocation = legacyLocation;
    }

    @VisibleForTesting
    static Cache<VersionMatches> fileCache(JsonService jsonService, Path file) {
        //noinspection Convert2Diamond
        return CacheBuilder.<VersionMatches>create()
            .withVersion(CACHE_VERSION)
            .withInitialValue(new VersionMatches(new ConcurrentHashMap<>()))
            .withSourceStore(new CacheFile(
                new JsonCacheFile<VersionMatches>(
                    CacheExceptionHandler.logging(),
                    jsonService,
                    new TypeLiteral<VersionMatches>() {},
                    CACHE_VERSION,
                    file
                ),
                new JsonCacheFile<LegacyVersionMatches>(
                    CacheExceptionHandler.logging(),
                    jsonService,
                    new TypeLiteral<LegacyVersionMatches>() {},
                    LEGACY_CACHE_VERSION,
                    file
                )
            )).build();
    }

    @Override
    public Set<VersionID> match(Version version, VersionProcessor processor) throws HeadlessMcException {
        Optional<Set<VersionID>> cached = getCached(version);
        if (cached.isPresent()) {
            return cached.get();
        }

        Set<VersionID> result = matchUncached(version, processor);
        remember(version, new VersionMatch(
            getFile(version),
            version.getInheritsFrom(),
            result.stream().map(VersionID::asArg).sorted(Comparator.comparing(VersionArg::toString)).toList()
        ));

        return result;
    }

    private Optional<Set<VersionID>> getCached(Version version) throws HeadlessMcException {
        List<VersionMatch> matches = cache.get().map(all -> all.matches().get(version.getId())).orElse(null);
        if (matches == null) {
            return Optional.empty();
        }

        @Nullable String file = getFile(version);
        @Nullable VersionMatch match = find(matches, file);
        boolean legacy = false;
        if (match == null && ownsLegacyMatch(version)) {
            match = find(matches, null);
            legacy = match != null;
        }

        if (match == null || !Objects.equals(match.inheritsFrom(), version.getInheritsFrom())) {
            return Optional.empty();
        }

        Set<VersionID> result = new HashSet<>();
        try {
            for (VersionArg id : match.ids()) {
                result.add(VersionID.resolve(platforms, id));
            }
        } catch (HeadlessMcException | IllegalArgumentException e) {
            // e.g. a platform that is no longer available, the version will be matched again
            log.debug("Failed to resolve cached matches {} for {}", match.ids(), version.getId(), e);
            return Optional.empty();
        }

        if (result.isEmpty()) {
            return Optional.empty();
        }

        if (legacy) {
            // from now on the match belongs to this file, and to no other file with the same id
            remember(version, new VersionMatch(file, match.inheritsFrom(), match.ids()));
        }

        return Optional.of(result);
    }

    /**
     * Remembers a match for the file of a version, replacing the match that file had before.
     * Matches for files with the same id that do not exist anymore are forgotten.
     */
    private void remember(Version version, VersionMatch match) throws HeadlessMcException {
        boolean replacesLegacy = ownsLegacyMatch(version);
        cache.maybeModify(all -> {
            List<VersionMatch> before = all.matches().getOrDefault(version.getId(), List.of());
            List<VersionMatch> after = new ArrayList<>(before.size() + 1);
            for (VersionMatch other : before) {
                boolean replaced = Objects.equals(other.file(), match.file()) || other.file() == null && replacesLegacy;
                if (!replaced && (other.file() == null || exists(other.file()))) {
                    after.add(other);
                }
            }

            after.add(match);
            if (after.equals(before)) {
                return false;
            }

            all.matches().put(version.getId(), List.copyOf(after));
            return true;
        });
    }

    private static @Nullable VersionMatch find(List<VersionMatch> matches, @Nullable String file) {
        return matches.stream().filter(match -> Objects.equals(match.file(), file)).findFirst().orElse(null);
    }

    /**
     * @return the file the match of a version is remembered for, {@code null} for a version without a file.
     */
    private static @Nullable String getFile(Version version) {
        return version.asFile().map(file -> file.toAbsolutePath().normalize().toString()).orElse(null);
    }

    /**
     * @return {@code true} if a match without a file, from a cache file of format 1, is the match of this version.
     */
    private boolean ownsLegacyMatch(Version version) {
        return version.asFile().filter(legacyLocation).isPresent();
    }

    /**
     * A cached file can be anything: it may have been deleted, be on a disk that is not mounted,
     * or not even be a valid path on this system if the cache has been copied from another one.
     * None of that is an error, the match is just not kept.
     */
    private static boolean exists(String file) {
        try {
            return Files.exists(Path.of(file));
        } catch (InvalidPathException | SecurityException e) {
            log.debug("Forgetting the match of {}", file, e);
            return false;
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

    /**
     * Like {@link #match(Version, VersionProcessor)}, but returns an empty set for versions no platform can match.
     * When searching through versions, e.g. all installed versions, one we do not understand
     * (installed by another launcher, or by a platform that is not available) should not abort the search.
     * Such versions are expected, e.g. a version installed under a custom name, so this only logs on debug.
     */
    private Set<VersionID> matchOrEmpty(Version version, VersionProcessor processor) throws HeadlessMcException {
        try {
            return match(version, processor);
        } catch (VersionMatchException e) {
            log.debug("Skipping version {}, it does not match any platform", version.getId(), e);
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
     * The contents of the cache file: the matches of every {@link Version#getId()},
     * one for every file a version with that id has been read from.
     *
     * @param matches the cached matches by id. The lists are immutable, they are replaced as a whole.
     */
    @RegisterForReflection
    record VersionMatches(Map<String, List<VersionMatch>> matches) implements ReflectionRegistered {
        @SuppressWarnings("ConstantValue")
        VersionMatches {
            // matches are read outside the lock of the cache, and a cache file can contain anything
            Map<String, List<VersionMatch>> copy = new ConcurrentHashMap<>();
            if (matches != null) {
                matches.forEach((id, list) -> {
                    if (id != null && list != null) {
                        copy.put(id, list.stream().filter(Objects::nonNull).toList());
                    }
                });
            }

            matches = copy;
        }
    }

    /**
     * The {@link VersionID}s matched for a {@link Version}.
     *
     * @param file         the absolute, normalized {@link Version#asFile() file} of the version, as a string, so
     *                     that a path this system cannot parse does not make the whole cache unreadable.
     *                     {@code null} for a version that has not been read from a file, and for matches read
     *                     from a cache file of format 1, which did not remember the file.
     * @param inheritsFrom {@link Version#getInheritsFrom()}, if it changes the version has been replaced.
     * @param ids          the matched {@link VersionID}s as {@link VersionArg}s.
     */
    @RegisterForReflection
    record VersionMatch(
        @Nullable String file,
        @Nullable String inheritsFrom,
        List<VersionArg> ids
    ) implements ReflectionRegistered {
        @SuppressWarnings("ConstantValue")
        VersionMatch {
            ids = ids == null ? List.of() : List.copyOf(ids);
        }
    }

    /**
     * The contents of a cache file of format 1, which had one match for every {@link Version#getId()}.
     *
     * @param matches the cached matches by id.
     */
    @RegisterForReflection
    record LegacyVersionMatches(@Nullable Map<String, LegacyVersionMatch> matches) implements ReflectionRegistered {
        /**
         * @return the matches in the current format. They are not bound to a file yet, the first version in a
         * location these matches have been made for takes its match over, see {@link #legacyLocation}.
         */
        VersionMatches migrate() {
            Map<String, List<VersionMatch>> result = new HashMap<>();
            if (matches != null) {
                matches.forEach((id, match) -> {
                    if (id != null && match != null) {
                        result.put(id, List.of(new VersionMatch(null, match.inheritsFrom(), match.ids())));
                    }
                });
            }

            return new VersionMatches(result);
        }
    }

    /**
     * A match of a cache file of format 1.
     *
     * @param inheritsFrom {@link Version#getInheritsFrom()}.
     * @param ids          the matched {@link VersionID}s as {@link VersionArg}s.
     */
    @RegisterForReflection
    record LegacyVersionMatch(
        @Nullable String inheritsFrom,
        @Nullable List<VersionArg> ids
    ) implements ReflectionRegistered {

    }

    /**
     * The cache file. It is written in the current format, and read in the current format or,
     * if it has been written by an older version of HeadlessMc, in the format that version used.
     *
     * @param current the file in the current format.
     * @param legacy  the same file in format 1.
     */
    private record CacheFile(
        JsonCacheFile<VersionMatches> current,
        JsonCacheFile<LegacyVersionMatches> legacy
    ) implements CacheSource<VersionMatches>, CacheStore<VersionMatches> {
        @Override
        public Optional<VersionMatches> get() throws HeadlessMcException {
            Optional<VersionMatches> matches = current.get();
            return matches.isPresent() ? matches : legacy.get().map(LegacyVersionMatches::migrate);
        }

        @Override
        public void store(CacheObject<VersionMatches> object) throws HeadlessMcException {
            current.store(object);
        }
    }

}
