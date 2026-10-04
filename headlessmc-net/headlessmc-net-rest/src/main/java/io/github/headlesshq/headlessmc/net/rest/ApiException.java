package io.github.headlesshq.headlessmc.net.rest;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import jakarta.ws.rs.core.Response;
import lombok.Getter;
import lombok.experimental.StandardException;
import org.jspecify.annotations.Nullable;

import java.io.Serial;

@StandardException
public class ApiException extends HeadlessMcException {
    @Serial
    private static final long serialVersionUID = 1L;
    @Getter
    private @Nullable Response response;

    public ApiException(Response response) {
        super("API returned " + response.getStatus());
        this.response = response;
    }

}
