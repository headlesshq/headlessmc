package io.github.headlesshq.headlessmc.platform.paper;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.net.rest.ApiException;
import io.github.headlesshq.headlessmc.platform.VanillaInstaller;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.paper.api.BuildResponse;
import io.github.headlesshq.headlessmc.platform.paper.api.PaperAPI;
import io.github.headlesshq.headlessmc.platform.server.ServerFinder;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.Version;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

/**
 * Installs jars from the paper fill API to {@link ServerFinder#DEFAULT_JAR}.
 *
 * @see BuildResponse
 */
@Slf4j
@Paper
@ApplicationScoped
@RequiredArgsConstructor
public class PaperInstaller implements ServerInstaller {
    private final String projectName;
    private final VanillaInstaller vanillaInstaller;
    private final DownloadService downloadService;
    private final PaperAPI api;

    @Inject
    public PaperInstaller(DownloadService downloadService, VanillaInstaller vanillaInstaller, @RestClient PaperAPI api) {
        this(PaperAPI.PAPER_PROJECT, vanillaInstaller, downloadService, api);
    }

    @Override
    public Installation installServer(
        VersionID id,
        Path dir,
        TypedMap args
    ) throws HeadlessMcException {
        Version vanillaVersion = vanillaInstaller.getVersion(id.getVersion());
        try {
            BuildResponse response = id.getBuild().isEmpty()
                ? api.getLatest(projectName, id.getVersion().getName())
                : api.getBuild(projectName, id.getVersion().getName(), Integer.parseInt(id.getBuild().get().getName()));

            Path file = dir.resolve(ServerFinder.DEFAULT_JAR);
            // TODO: support other checksums?
            String sha256 = response.getServerDownload().checksums().get("sha256");
            downloadService.download(URI.create(response.getServerDownload().url()))
                .sha256(sha256)
                .progressBar("Downloading Paper")
                .size(response.getServerDownload().size())
                .toFile(file);

            return new Installation(vanillaVersion.requireJavaVersion());
        } catch (ApiException e) {
            if (isNotFound(e)) {
                throw new HeadlessMcException(versionNotFoundMessage(id.getVersion().getName()), e);
            }

            throw new HeadlessMcIOException("Failed to find paper version " + id.getVersion().getName(), e);
        } catch (ProcessingException e) {
            throw new HeadlessMcIOException("Failed to find paper version " + id.getVersion().getName(), e);
        }
    }

    private static boolean isNotFound(ApiException e) {
        return e.getResponse() != null && e.getResponse().getStatus() == 404;
    }

    /**
     * Paper does not have a build for every Minecraft version, e.g. there is a Minecraft version 26.1,
     * but Paper only has builds for 26.1.1 and 26.1.2. In that case we suggest the versions of the group.
     */
    private String versionNotFoundMessage(String version) {
        String message = "Paper has no builds for Minecraft " + version + ".";
        try {
            List<String> group = api.getProject(projectName).versions().get(version);
            if (group != null && !group.isEmpty()) {
                message += " Available versions for " + version + ": " + String.join(", ", group);
            }
        } catch (ApiException | ProcessingException e) {
            log.debug("Failed to get versions of project {}", projectName, e);
        }

        return message;
    }

}
