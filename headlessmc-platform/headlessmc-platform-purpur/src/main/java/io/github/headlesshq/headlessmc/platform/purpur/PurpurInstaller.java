package io.github.headlesshq.headlessmc.platform.purpur;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.platform.VanillaInstaller;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.purpur.api.BuildResponse;
import io.github.headlesshq.headlessmc.platform.purpur.api.PurpurAPI;
import io.github.headlesshq.headlessmc.platform.purpur.api.VersionResponse;
import io.github.headlesshq.headlessmc.platform.server.ServerFinder;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.Version;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.nio.file.Path;

@Purpur
@ApplicationScoped
@RequiredArgsConstructor
public class PurpurInstaller implements ServerInstaller {
    private final String projectName;
    private final String url;
    private final VanillaInstaller vanillaInstaller;
    private final DownloadService downloadService;
    private final PurpurAPI api;

    @Inject
    public PurpurInstaller(DownloadService downloadService, VanillaInstaller vanillaInstaller, @RestClient PurpurAPI api) {
        this(PurpurAPI.PURPUR_PROJECT, PurpurAPI.V2_URL, vanillaInstaller, downloadService, api);
    }

    @Override
    public Installation installServer(
        VersionID id,
        Path dir,
        TypedMap args
    ) throws HeadlessMcException {
        Version vanillaVersion = vanillaInstaller.getVersion(id.getVersion());
        BuildResponse build = getBuild(id);

        Path file = dir.resolve(ServerFinder.DEFAULT_JAR);
        downloadService.download(build.getDownloadUrl(url))
            .hash("MD5", build.md5())
            .toFile(file);

        return new Installation(vanillaVersion.requireJavaVersion());
    }

    private BuildResponse getBuild(VersionID id) {
        if (id.getBuild().isEmpty()) {
            VersionResponse response = api.getVersion(projectName, id.getVersion().getName());
            return api.getBuild(projectName, id.getVersion().getName(), response.builds().latest());
        }

        return api.getBuild(projectName, id.getVersion().getName(), id.getBuild().get().getName());
    }

}
