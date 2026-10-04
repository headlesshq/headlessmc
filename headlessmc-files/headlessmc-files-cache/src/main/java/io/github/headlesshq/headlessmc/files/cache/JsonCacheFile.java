package io.github.headlesshq.headlessmc.files.cache;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonParseException;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import jakarta.enterprise.util.TypeLiteral;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@ToString
@RequiredArgsConstructor
public final class JsonCacheFile<V> implements CacheSource<V>, CacheStore<V> {
    private final CacheExceptionHandler exceptionHandler;
    private final JsonService jsonService;
    private final TypeLiteral<V> type;
    private final long version;
    // TODO: make dynamic
    private final Path path;

    @Override
    public void store(CacheObject<V> object) throws HeadlessMcException {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            jsonService.write(path, object, true);
        } catch (IOException | JsonParseException e) {
            exceptionHandler.error(this, "Failed to write " + object + "to cache file " + path, e);
        }
    }

    @Override
    public Optional<V> get() throws HeadlessMcException {
        if (!Files.exists(path)) {
            exceptionHandler.info(this, "Cache file does not exist: " + path);
            return Optional.empty();
        }

        try (InputStream inputStream = Files.newInputStream(path)) {
            Optional<V> value = jsonService.parseVersioned(inputStream, type, Math.toIntExact(version));
            if (value.isEmpty()) {
                exceptionHandler.info(
                    this,
                    "%s resource out of date or missing, expected version %d".formatted(this, version)
                );
            }

            return value;
        } catch (IOException | JsonParseException e) {
            exceptionHandler.error(this, "Failed to read cache resource " + this, e);
            return Optional.empty();
        }
    }

    public static <V extends ReflectionRegistered> JsonCacheFile<V> of(
        CacheExceptionHandler exceptionHandler,
        JsonService jsonService,
        TypeLiteral<V> type,
        long version,
        Path path
    ) {
        return new JsonCacheFile<>(exceptionHandler, jsonService, type, version, path);
    }

}
