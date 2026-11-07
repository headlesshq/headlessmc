package io.github.headlesshq.headlessmc.files.cache;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.util.json.JsonParseException;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import jakarta.enterprise.util.TypeLiteral;
import lombok.ToString;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * A {@link CacheSource} reading a json resource from a {@link ClassLoader}.
 * <p>
 * A resource is either plain json for the value, or, if this resource has been created with a version,
 * a {@link CacheObject} as written by a {@link JsonCacheFile}: {@code {"version": 0, "object": {...}}}.
 * Versioned resources whose version does not match are treated as missing.
 */
@ToString
public final class JsonResource<V> extends AbstractJsonSource<V> implements CacheSource<V> {
    private final ClassLoader classLoader;
    private final String name;
    private final @Nullable Long version;

    /**
     * Creates a resource for plain json.
     */
    public JsonResource(
        CacheExceptionHandler exceptionHandler,
        JsonService jsonService,
        TypeLiteral<V> type,
        ClassLoader classLoader,
        String name
    ) {
        this(exceptionHandler, jsonService, type, classLoader, name, null);
    }

    /**
     * Creates a resource for a versioned {@link CacheObject}, or for plain json if the version is {@code null}.
     *
     * @param version the version the resource has to have, see {@link Cache#getVersion()}.
     */
    public JsonResource(
        CacheExceptionHandler exceptionHandler,
        JsonService jsonService,
        TypeLiteral<V> type,
        ClassLoader classLoader,
        String name,
        @Nullable Long version
    ) {
        super(exceptionHandler, jsonService, type);
        this.classLoader = classLoader;
        this.name = name;
        this.version = version;
    }

    @Override
    public Optional<V> get() throws HeadlessMcException {
        if (version == null) {
            return super.get();
        }

        try (InputStream inputStream = getInputStream()) {
            if (inputStream == null) {
                exceptionHandler.error(this, "Failed to find input stream for " + this);
                return Optional.empty();
            }

            Optional<V> value = jsonService.parseVersioned(inputStream, type, Math.toIntExact(version));
            if (value.isEmpty()) {
                exceptionHandler.info(
                    this,
                    "%s resource out of date, expected version %d".formatted(this, version)
                );
            }

            return value;
        } catch (IOException | JsonParseException e) {
            exceptionHandler.error(this, "Failed to read cache resource " + this, e);
            return Optional.empty();
        }
    }

    @Override
    protected @Nullable InputStream getInputStream() {
        return classLoader.getResourceAsStream(name);
    }

    public static ClassLoader getDefaultClassLoader() {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        return context == null ? JsonResource.class.getClassLoader() : context;
    }

}
