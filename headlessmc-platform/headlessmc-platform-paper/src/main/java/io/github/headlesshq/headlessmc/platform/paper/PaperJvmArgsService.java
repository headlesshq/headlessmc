package io.github.headlesshq.headlessmc.platform.paper;

import io.github.headlesshq.headlessmc.net.rest.ApiException;
import io.github.headlesshq.headlessmc.platform.paper.api.PaperAPI;
import io.github.headlesshq.headlessmc.platform.paper.api.VersionResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;
import java.util.Optional;

/**
 * Provides the jvm args that Paper recommends for running a server of a Minecraft version.
 * These are also used for Paper forks like Purpur.
 *
 * @see VersionResponse.Java#flags()
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class PaperJvmArgsService {
    /**
     * The key of the recommended flags in {@link VersionResponse.Java#flags()}.
     */
    public static final String RECOMMENDED = "recommended";

    private final String projectName;
    private final PaperAPI api;

    @Inject
    public PaperJvmArgsService(@RestClient PaperAPI api) {
        this(PaperAPI.PAPER_PROJECT, api);
    }

    /**
     * Gets the jvm args recommended by Paper for the given Minecraft version.
     * This is best effort, if the Paper API fails or has no recommendations an empty list is returned.
     *
     * @param version the Minecraft version, e.g. {@code 1.21.1}.
     * @return the recommended jvm args, or an empty list.
     */
    public List<String> getRecommendedJvmArgs(String version) {
        try {
            return Optional.ofNullable(api.getVersion(projectName, version))
                .map(VersionResponse::version)
                .map(VersionResponse.Version::java)
                .map(VersionResponse.Java::flags)
                .map(flags -> flags.get(RECOMMENDED))
                .map(List::copyOf)
                .orElse(List.of());
        } catch (ApiException | ProcessingException e) {
            log.debug("Failed to get recommended jvm args for {} {}", projectName, version, e);
            return List.of();
        }
    }

}
