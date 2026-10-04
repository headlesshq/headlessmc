package io.github.headlesshq.headlessmc.files.cache;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.util.json.JsonParseException;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import jakarta.enterprise.util.TypeLiteral;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@RequiredArgsConstructor
public abstract class AbstractJsonSource<V> implements CacheSource<V> {
    protected final CacheExceptionHandler exceptionHandler;
    protected final JsonService jsonService;
    protected final TypeLiteral<V> type;

    protected abstract @Nullable InputStream getInputStream() throws IOException;

    @Override
    public abstract String toString();

    @Override
    public Optional<V> get() throws HeadlessMcException {
        try (InputStream inputStream = getInputStream()) {
            if (inputStream == null) {
                exceptionHandler.error(this, "Failed to find input stream for " + this);
                return Optional.empty();
            }

            return Optional.of(jsonService.parseRegisteredForReflection(inputStream, type));
        } catch (IOException | JsonParseException e) {
            exceptionHandler.error(this, "Failed to read cache resource " + this, e);
            return Optional.empty();
        }
    }

}
