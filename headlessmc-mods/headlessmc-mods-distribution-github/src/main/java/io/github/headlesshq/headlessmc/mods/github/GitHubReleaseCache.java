package io.github.headlesshq.headlessmc.mods.github;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.files.cache.CacheBuilder;
import io.github.headlesshq.headlessmc.files.cache.CacheExceptionHandler;
import io.github.headlesshq.headlessmc.files.cache.JsonCacheFile;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Caches the {@link GitHubRelease}s fetched by the {@link GitHubReleaseService},
 * stored in {@code github/releases.json} inside the cache directory.
 */
@ApplicationScoped
public class GitHubReleaseCache {
    static final int CACHE_VERSION = 1;

    private final Cache<Releases> cache;

    @Inject
    public GitHubReleaseCache(AppFiles files, FileService fileService, JsonService jsonService) {
        //noinspection Convert2Diamond
        this(CacheBuilder.<Releases>create()
            .withVersion(CACHE_VERSION)
            .withSourceStore(new JsonCacheFile<Releases>(
                CacheExceptionHandler.logging(),
                jsonService,
                new TypeLiteral<Releases>() {},
                CACHE_VERSION,
                fileService.getPath(files.getCacheDir(), "github", "releases.json")
            )).build());
    }

    /**
     * Constructs a new GitHubReleaseCache, which stores its releases in the given cache.
     *
     * @param cache the cache to store the releases in.
     */
    public GitHubReleaseCache(Cache<Releases> cache) {
        this.cache = cache;
    }

    /**
     * @param key the key the release has been cached for.
     * @return the cached release, or empty if the release has not been cached.
     */
    public synchronized Optional<CachedRelease> get(String key) {
        return cache.get().map(releases -> releases.releases().get(key));
    }

    /**
     * Caches a release that has just been fetched and writes it to disk.
     *
     * @param key     the key to cache the release for.
     * @param release the cached release.
     */
    public synchronized void set(String key, CachedRelease release) {
        Map<String, CachedRelease> releases = new HashMap<>(cache.get().map(Releases::releases).orElse(Map.of()));
        releases.put(key, release);
        cache.set(new Releases(releases));
    }

    /**
     * All cached releases.
     *
     * @param releases the cached releases by their key.
     */
    @RegisterForReflection
    public record Releases(Map<String, CachedRelease> releases) implements ReflectionRegistered {

    }

    /**
     * A cached release.
     *
     * @param fetchedAt the time the release was fetched, in milliseconds since the epoch.
     * @param release   the release.
     */
    @RegisterForReflection
    public record CachedRelease(long fetchedAt, GitHubRelease release) implements ReflectionRegistered {

    }

}
