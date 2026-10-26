package io.github.headlesshq.headlessmc.updater;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.cache.CacheBuilder;
import io.github.headlesshq.headlessmc.files.cache.CacheExceptionHandler;
import io.github.headlesshq.headlessmc.files.cache.JsonCacheFile;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;

/**
 * Creates the {@link UpdateCheckCache}s of {@link Updater}s,
 * each stored in {@code updater/<name>.json} inside the cache directory.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class UpdateCheckCacheFactory {
    static final String CACHE_DIR = "updater";

    private final AppFiles files;
    private final FileService fileService;
    private final JsonService jsonService;

    /**
     * Creates the cache of an updater.
     *
     * @param name    the {@link Updater#getName()} of the updater.
     * @param version the version of the cached format, cached results of another version are ignored.
     * @param type    the type of {@link UpdateCheck} stored, e.g. {@code new TypeLiteral<UpdateCheck<Value>>() {}}.
     * @param <V>     the type of result stored.
     * @return a new cache.
     */
    public <V> UpdateCheckCache<V> create(String name, long version, TypeLiteral<UpdateCheck<V>> type) {
        return create(fileService.getPath(files.getCacheDir(), CACHE_DIR, name + ".json"), version, type);
    }

    <V> UpdateCheckCache<V> create(Path file, long version, TypeLiteral<UpdateCheck<V>> type) {
        //noinspection Convert2Diamond
        return new UpdateCheckCache<>(CacheBuilder.<UpdateCheck<V>>create()
            .withVersion(version)
            .withSourceStore(new JsonCacheFile<UpdateCheck<V>>(
                CacheExceptionHandler.logging(),
                jsonService,
                type,
                version,
                file
            )).build());
    }

}
