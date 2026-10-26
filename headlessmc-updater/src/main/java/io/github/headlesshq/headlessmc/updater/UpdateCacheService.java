package io.github.headlesshq.headlessmc.updater;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.files.cache.CacheBuilder;
import io.github.headlesshq.headlessmc.files.cache.CacheExceptionHandler;
import io.github.headlesshq.headlessmc.files.cache.JsonCacheFile;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;

import java.util.Optional;

/**
 * Stores the latest release fetched by the {@link UpdateService} in the cache directory.
 */
@ApplicationScoped
public class UpdateCacheService {
    static final String CACHE_FILE = "updater.json";
    static final int CACHE_VERSION = 1;

    private final Cache<UpdateCache> cache;

    @Inject
    public UpdateCacheService(AppFiles files, FileService fileService, JsonService jsonService) {
        //noinspection Convert2Diamond
        this.cache = CacheBuilder.<UpdateCache>create()
            .withVersion(CACHE_VERSION)
            .withSourceStore(new JsonCacheFile<UpdateCache>(
                CacheExceptionHandler.logging(),
                jsonService,
                new TypeLiteral<UpdateCache>() {},
                CACHE_VERSION,
                fileService.getPath(files.getCacheDir(), CACHE_FILE)
            )).build();
    }

    /**
     * @return the cached release, or empty if nothing has been cached yet.
     */
    public Optional<UpdateCache> get() {
        return cache.get();
    }

    /**
     * Replaces the cached release and writes it to disk.
     *
     * @param updateCache the release to cache.
     */
    public void set(UpdateCache updateCache) {
        cache.set(updateCache);
    }

}
