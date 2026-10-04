package io.github.headlesshq.headlessmc.java.distribution.foojay;

import io.github.headlesshq.headlessmc.net.rest.ApiException;
import jakarta.ws.rs.core.Response;
import org.jspecify.annotations.Nullable;

/**
 * Results returned from the foojay disco API
 * are usually wrapped as a JSON Object
 * {@code "result": [...]}.
 *
 * @param result the wrapped result.
 * @param <T>    the type of the wrapped result.
 */
record Result<T>(@Nullable T result) {
    /**
     * Checks if the result is non-null and throws an {@link ApiException} otherwise.
     *
     * @return the unwrapped result.
     * @throws ApiException if the result is {@code null}.
     */
    public T resolve(Response response) throws ApiException {
        T result = result();
        if (result == null) {
            throw new ApiException("No result available in response: " + response);
        }

        return result;
    }

}
