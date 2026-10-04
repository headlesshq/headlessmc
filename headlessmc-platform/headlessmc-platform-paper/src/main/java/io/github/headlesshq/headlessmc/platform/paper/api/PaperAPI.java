package io.github.headlesshq.headlessmc.platform.paper.api;

import io.github.headlesshq.headlessmc.net.rest.ApiException;
import io.github.headlesshq.headlessmc.net.rest.ApiExceptionMapper;
import io.github.headlesshq.headlessmc.platform.paper.Paper;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * This represents a REST client for the Paper api
 * at <a href=https://fill.papermc.io>https://fill.papermc.io</a>.
 * It has been modeled after the swagger documentation
 * at <a href=https://fill.papermc.io/swagger-ui/index.html#/>
 * https://fill.papermc.io/swagger-ui/index.html#/
 * </a>
 *
 * @see <a href=https://fill.papermc.io/openapi.yaml>
 * https://fill.papermc.io/openapi.yaml
 * </a>
 */
@Path("/v3")
@RegisterProvider(ApiExceptionMapper.class)
@RegisterRestClient(baseUri = "https://fill.papermc.io")
public interface PaperAPI {
    /**
     * The Paper API distinguishes between projects.
     * {@code "paper"} represents the PaperMC server project,
     * but there's also {@code "folia"}, {@code "velocity"}, etc.
     */
    String PAPER_PROJECT = Paper.PLATFORM_NAME;

    /**
     * Get details of a specific project, including its versions grouped by version family.
     *
     * @param project The key of the project.
     */
    @GET
    @Path("/projects/{project}")
    @Produces({"application/json"})
    ProjectResponse getProject(
        @PathParam("project") String project
    ) throws ApiException, ProcessingException;

    /**
     * Get a list of versions for a specific project.
     *
     * @param project The key of the project.
     */
    @GET
    @Path("/projects/{project}/versions")
    @Produces({"application/json"})
    VersionsResponse getVersions(
        @PathParam("project") String project
    ) throws ApiException, ProcessingException;

    /**
     * Get details of a specific version for a project.
     *
     * @param project The key of the project.
     * @param version The key of the version.
     */
    @GET
    @Path("/projects/{project}/versions/{version}")
    @Produces({"application/json"})
    VersionResponse getVersion(
        @PathParam("project") String project,
        @PathParam("version") String version
    ) throws ApiException, ProcessingException;

    /**
     * Get details of a specific build for a version of a project.
     *
     * @param project The key of the project.
     * @param version The key of the version.
     * @param build The number of the build.
     */
    @GET
    @Path("/projects/{project}/versions/{version}/builds/{build}")
    @Produces({"application/json"})
    BuildResponse getBuild(
        @PathParam("project") String project,
        @PathParam("version") String version,
        @PathParam("build") Integer build
    ) throws ApiException, ProcessingException;

    /**
     * Get details of the latest build for a version of a project.
     *
     * @param project The key of the project.
     * @param version The key of the version.
     */
    @GET
    @Path("/projects/{project}/versions/{version}/builds/latest")
    @Produces({"application/json"})
    BuildResponse getLatest(
        @PathParam("project") String project,
        @PathParam("version") String version
    ) throws ApiException, ProcessingException;

    /**
     * Get a list of builds for a specific version of a project.
     *
     * @param project The key of the project.
     * @param version The key of the version.
     * @param channel Filter builds by one or more channels
     *                Available values : ALPHA, BETA, STABLE, RECOMMENDED
     */
    @GET
    @Path("/projects/{project}/versions/{version}/builds")
    @Produces({"application/json"})
    List<BuildResponse> getBuilds(
        @PathParam("project") String project,
        @PathParam("version") String version,
        @Nullable @QueryParam("channel") List<Channel> channel
    ) throws ApiException, ProcessingException;

}
