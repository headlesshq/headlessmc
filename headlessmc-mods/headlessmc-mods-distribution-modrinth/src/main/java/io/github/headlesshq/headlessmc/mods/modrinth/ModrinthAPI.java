package io.github.headlesshq.headlessmc.mods.modrinth;

import io.github.headlesshq.headlessmc.net.rest.ApiExceptionMapper;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * API client for the ModrinthAPI.
 *
 * @see <a href=https://docs.modrinth.com/api/>https://docs.modrinth.com/api/</a>
 */
@Path("/v2")
@RegisterProvider(ApiExceptionMapper.class)
@RegisterRestClient(baseUri = "https://api.modrinth.com")
interface ModrinthAPI {
    /**
     * Searches for a project.
     *
     * @param query the query to search for
     * @param facets the ({@link Facets#toQuery()}) to search for.
     * @return the search result.
     * @see <a href=https://docs.modrinth.com/api/operations/searchprojects/>
     * https://docs.modrinth.com/api/operations/searchprojects/
     * </a>
     */
    @GET
    @Path("/search")
    @Produces({"application/json"})
    ModrinthSearchResult search(
        @QueryParam("query") String query,
        @QueryParam("facets") String facets
    );

    /**
     * Get a project.
     *
     * @param project The ID or slug of the project
     * @return the project.
     * @see <a href=https://docs.modrinth.com/api/operations/getproject/>
     * https://docs.modrinth.com/api/operations/getproject/
     * </a>
     */
    @GET
    @Path("/project/{id_slug}")
    @Produces({"application/json"})
    ModrinthProject getProject(
        @PathParam("id_slug") String project
    );

    /**
     * Lists project versions.
     *
     * @param project the ID or slug of the project.
     * @param gameVersions The game versions to filter for.
     * @param loaders the types of ModLoaders to search for.
     * @return the search result.
     * @see <a href=https://docs.modrinth.com/api/operations/getprojectversions/>
     * https://docs.modrinth.com/api/operations/getprojectversions/
     * </a>
     */
    @GET
    @Path("/project/{id_slug}/version")
    @Produces({"application/json"})
    List<ModrinthProjectVersion> getProjectVersions(
        @PathParam("id_slug") String project,
        @Nullable @QueryParam("game_versions") String gameVersions,
        @Nullable @QueryParam("loaders") String loaders
    );

}
