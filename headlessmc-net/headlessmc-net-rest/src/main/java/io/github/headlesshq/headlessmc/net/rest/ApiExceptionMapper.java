package io.github.headlesshq.headlessmc.net.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

@Provider
public class ApiExceptionMapper implements ResponseExceptionMapper<ApiException> {
    @Override
    public ApiException toThrowable(Response response) {
        return new ApiException(response);
    }

}
