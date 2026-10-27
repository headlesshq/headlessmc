package io.github.headlesshq.headlessmc.mods.curseforge;

import io.github.headlesshq.headlessmc.net.rest.ApiExceptionMapper;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * API client for the CurseForge Core API.
 *
 * @see <a href=https://docs.curseforge.com/rest-api/>https://docs.curseforge.com/rest-api/</a>
 */
@Path("/v1")
@RegisterProvider(ApiExceptionMapper.class)
@RegisterRestClient(baseUri = "https://api.curseforge.com")
interface CurseForgeAPI {
    /**
     * The header the api key has to be sent in.
     */
    String API_KEY_HEADER = "x-api-key";

    /**
     * Searches for mods.
     *
     * @param apiKey        the api key.
     * @param gameId        the id of the game, {@link CurseForgeIds#MINECRAFT}.
     * @param classId       the class (mods, resource packs, ...) to search in.
     * @param searchFilter  free text to search for.
     * @param slug          the slug to search for, unique only together with a {@code classId}.
     * @param authorId      the id of an author whose mods to search for.
     * @param gameVersion   the game version the mods should support.
     * @param modLoaderType the {@link CurseForgeIds} mod loader the mods should support.
     * @param pageSize      the amount of results to return, at most 50.
     * @return the found mods.
     * @see <a href=https://docs.curseforge.com/rest-api/#search-mods>https://docs.curseforge.com/rest-api/#search-mods</a>
     */
    @GET
    @Path("/mods/search")
    @Produces({"application/json"})
    CurseForgeResponse<List<CurseForgeMod>> search(
        @HeaderParam(API_KEY_HEADER) String apiKey,
        @QueryParam("gameId") int gameId,
        @Nullable @QueryParam("classId") Integer classId,
        @Nullable @QueryParam("searchFilter") String searchFilter,
        @Nullable @QueryParam("slug") String slug,
        @Nullable @QueryParam("authorId") Integer authorId,
        @Nullable @QueryParam("gameVersion") String gameVersion,
        @Nullable @QueryParam("modLoaderType") Integer modLoaderType,
        @Nullable @QueryParam("pageSize") Integer pageSize
    );

    /**
     * Gets a mod by its id.
     *
     * @param apiKey the api key.
     * @param modId  the numeric id of the mod.
     * @return the mod.
     * @see <a href=https://docs.curseforge.com/rest-api/#get-mod>https://docs.curseforge.com/rest-api/#get-mod</a>
     */
    @GET
    @Path("/mods/{modId}")
    @Produces({"application/json"})
    CurseForgeResponse<CurseForgeMod> getMod(
        @HeaderParam(API_KEY_HEADER) String apiKey,
        @PathParam("modId") String modId
    );

    /**
     * Lists the files of a mod.
     *
     * @param apiKey        the api key.
     * @param modId         the id of the mod.
     * @param gameVersion   the game version the files should support.
     * @param modLoaderType the {@link CurseForgeIds} mod loader the files should support.
     * @return the files of the mod.
     * @see <a href=https://docs.curseforge.com/rest-api/#get-mod-files>https://docs.curseforge.com/rest-api/#get-mod-files</a>
     */
    @GET
    @Path("/mods/{modId}/files")
    @Produces({"application/json"})
    CurseForgeResponse<List<CurseForgeFile>> getFiles(
        @HeaderParam(API_KEY_HEADER) String apiKey,
        @PathParam("modId") long modId,
        @Nullable @QueryParam("gameVersion") String gameVersion,
        @Nullable @QueryParam("modLoaderType") Integer modLoaderType
    );

}
