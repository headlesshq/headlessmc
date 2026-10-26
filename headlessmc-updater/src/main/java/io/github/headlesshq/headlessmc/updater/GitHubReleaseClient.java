package io.github.headlesshq.headlessmc.updater;

import io.github.headlesshq.headlessmc.net.rest.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Fetches the latest release of a repository from GitHub.
 */
@Slf4j
@ApplicationScoped
public class GitHubReleaseClient {
    /**
     * Fetches the latest release, which is the most recent release that is neither a draft nor a prerelease.
     * This never fails, if GitHub cannot be reached it is just logged.
     *
     * @param updaterConfig the config holding the repository and the timeout.
     * @return the latest release, or empty if it could not be fetched.
     */
    public Optional<GitHubRelease> getLatestRelease(UpdaterConfig updaterConfig) {
        String[] repository = updaterConfig.repository().split("/");
        if (repository.length != 2) {
            log.warn("Invalid repository for the updater, expected owner/repo, but got {}", updaterConfig.repository());
            return Optional.empty();
        }

        try {
            return Optional.of(createApi(updaterConfig).getLatestRelease(repository[0], repository[1]));
        } catch (ApiException | ProcessingException e) {
            log.debug("Failed to fetch the latest release of {}", updaterConfig.repository(), e);
            return Optional.empty();
        }
    }

    private GitHubAPI createApi(UpdaterConfig updaterConfig) {
        return RestClientBuilder.newBuilder()
            .baseUri(URI.create(GitHubAPI.API_URL))
            .connectTimeout(updaterConfig.timeoutMs(), TimeUnit.MILLISECONDS)
            .readTimeout(updaterConfig.timeoutMs(), TimeUnit.MILLISECONDS)
            .build(GitHubAPI.class);
    }

}
