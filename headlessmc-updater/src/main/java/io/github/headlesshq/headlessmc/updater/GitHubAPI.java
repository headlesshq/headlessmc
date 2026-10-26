package io.github.headlesshq.headlessmc.updater;

import io.github.headlesshq.headlessmc.HeadlessMc;
import io.github.headlesshq.headlessmc.net.rest.ApiExceptionMapper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;

/**
 * API client for the GitHub releases API.
 *
 * @see <a href=https://docs.github.com/en/rest/releases/releases>https://docs.github.com/en/rest/releases/releases</a>
 */
@Path("/repos")
@RegisterProvider(ApiExceptionMapper.class)
@ClientHeaderParam(name = "User-Agent", value = HeadlessMc.DEFAULT_USER_AGENT)
@ClientHeaderParam(name = "Accept", value = "application/vnd.github+json")
interface GitHubAPI {
    /**
     * The base uri of the GitHub API.
     */
    String API_URL = "https://api.github.com";

    /**
     * Gets the latest release of a repository,
     * which is the most recent release that is neither a draft nor a prerelease.
     *
     * @param owner the owner of the repository.
     * @param repo  the name of the repository.
     * @return the latest release of the repository.
     * @see <a href=https://docs.github.com/en/rest/releases/releases#get-the-latest-release>
     * https://docs.github.com/en/rest/releases/releases#get-the-latest-release
     * </a>
     */
    @GET
    @Path("/{owner}/{repo}/releases/latest")
    @Produces({"application/json"})
    GitHubRelease getLatestRelease(
        @PathParam("owner") String owner,
        @PathParam("repo") String repo
    );

}
