package io.github.headlesshq.headlessmc.platform.purpur.api;

import io.github.headlesshq.headlessmc.net.rest.ApiException;
import io.github.headlesshq.headlessmc.net.rest.ApiExceptionMapper;
import io.github.headlesshq.headlessmc.platform.purpur.Purpur;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * This represents a REST client for the Purpur API
 * at <a href=https://api.purpurmc.org>https://api.purpurmc.org</a>.
 * It has been modeled after the swagger documentation
 * at <a href=https://api.purpurmc.org/docs/swagger-ui/index.html#/>
 * https://api.purpurmc.org/docs/swagger-ui/index.html#/
 * </a>
 *
 * @see <a href=https://api.purpurmc.org/openapi>
 * https://api.purpurmc.org/openapi
 * </a>
 */
@Path("/v2")
@RegisterProvider(ApiExceptionMapper.class)
@RegisterRestClient(baseUri = PurpurAPI.API_URL)
public interface PurpurAPI {
    String API_URL = "https://api.purpurmc.org";
    String V2_URL = "https://api.purpurmc.org/v2";
    /**
     * The Purpur API distinguishes between projects.
     * {@code "purpur"} represents the Purpur server project,
     * but there's also {@code "performance"}.
     */
    String PURPUR_PROJECT = Purpur.PLATFORM_NAME;

    /**
     * Get a project.
     *
     * @param project The key of the project.
     */
    @GET
    @Path("/{project}")
    @Produces({"application/json"})
    ProjectResponse getProject(
        @PathParam("project") String project
    ) throws ApiException, ProcessingException;

    /**
     * Get a project's version.
     *
     * @param project The key of the project.
     * @param version The key of the version.
     */
    @GET
    @Path("/{project}/{version}")
    @Produces({"application/json"})
    VersionResponse getVersion(
        @PathParam("project") String project,
        @PathParam("version") String version
    ) throws ApiException, ProcessingException;

    /**
     * Get a versions' build.
     *
     * @param project The key of the project.
     * @param version The key of the version.
     * @param build The number of the build.
     */
    @GET
    @Path("/{project}/{version}/{build}")
    @Produces({"application/json"})
    BuildResponse getBuild(
        @PathParam("project") String project,
        @PathParam("version") String version,
        @PathParam("build") String build
    ) throws ApiException, ProcessingException;

}
