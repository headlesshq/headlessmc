package io.github.headlesshq.headlessmc.files.cache;

import io.github.headlesshq.headlessmc.util.json.JsonService;
import jakarta.enterprise.util.TypeLiteral;
import lombok.ToString;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;

@ToString
public final class JsonResource<V> extends AbstractJsonSource<V> implements CacheSource<V> {
    private final ClassLoader classLoader;
    private final String name;

    public JsonResource(
        CacheExceptionHandler exceptionHandler,
        JsonService jsonService,
        TypeLiteral<V> type,
        ClassLoader classLoader,
        String name
    ) {
        super(exceptionHandler, jsonService, type);
        this.classLoader = classLoader;
        this.name = name;
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
