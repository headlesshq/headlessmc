package io.github.headlesshq.headlessmc.auth.yggdrasil;

import io.github.headlesshq.headlessmc.net.rest.ApiExceptionMapper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for the <a href="https://wiki.vg/Authentication">Yggdrasil</a>
 * authentication protocol, as implemented by third party authentication servers
 * such as <a href="https://littleskin.cn">LittleSkin</a>.
 *
 * <p>The base uri is not fixed, as every third party server has its own url.
 * Instances are therefore built via {@link YggdrasilClientFactory}, which reads the
 * configured server url and appends the path the api lives under.</p>
 */
@RegisterProvider(ApiExceptionMapper.class)
@RegisterRestClient
public interface YggdrasilAPI {
    /**
     * Authenticates with a username and password.
     * The response contains all profiles the account has access to.
     */
    @POST
    @Path("/authenticate")
    @Consumes("application/json")
    @Produces("application/json")
    YggdrasilSession.Response authenticate(YggdrasilSession.Authenticate request);

    /**
     * Refreshes an expired session.
     */
    @POST
    @Path("/refresh")
    @Consumes("application/json")
    @Produces("application/json")
    YggdrasilSession.Response refresh(YggdrasilSession.Refresh request);

    /**
     * Checks whether a session is still valid.
     * This returns {@code 204 No Content} when valid and an error status otherwise.
     */
    @POST
    @Path("/validate")
    @Consumes("application/json")
    @Produces("application/json")
    void validate(YggdrasilSession.Validate request);

    /**
     * Invalidates a session, so it cannot be used anymore.
     * This returns {@code 204 No Content} on success.
     */
    @POST
    @Path("/invalidate")
    @Consumes("application/json")
    @Produces("application/json")
    void invalidate(YggdrasilSession.Validate request);

}
