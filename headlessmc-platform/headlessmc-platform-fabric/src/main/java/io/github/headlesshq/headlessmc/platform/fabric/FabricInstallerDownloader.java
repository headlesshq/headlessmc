package io.github.headlesshq.headlessmc.platform.fabric;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.VanillaVersionService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helps with downloading the Fabric installer.
 * If the mc version is older than 1.14,
 * we use legacy fabric:
 * <a href=https://legacyfabric.net/>
 * https://legacyfabric.net/</a>
 *
 * @see InstallerArtifact
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
class FabricInstallerDownloader {
    private final VanillaVersionService vanillaVersionService;
    private final DownloadService downloadService;
    private final FileService fileService;
    private final AppFiles appFiles;
    private final InstallerArtifactService installerArtifactService;

    public Path download(VersionID id, @Nullable URI customInstallerURI) throws HeadlessMcException {
        if (customInstallerURI != null) {
            Path path = fileService.getPath(appFiles.getCacheDir(), "fabric-custom-installer.jar");
            return downloadService.download(customInstallerURI).toFile(path);
        }

        VanillaVersion v1_14 = vanillaVersionService.getVersion("1.14")
            .orElseThrow(() -> new IllegalStateException("Failed to find vanilla version 1.14"));

        // TODO: args to force new/legacy
        InstallerArtifact installer = vanillaVersionService.isOlderThan(id.getVersion(), v1_14)
            ? installerArtifactService.getLegacyInstaller()
            : installerArtifactService.getInstaller();

        return download(installer);
    }

    @VisibleForTesting
    Path download(InstallerArtifact installer) {
        Path installerPath = getPath(installer);
        if (Files.exists(installerPath)) {
            return installerPath;
        }

        return downloadService.download(installer.getURL())
            .sha256(installer.sha256())
            .size(installer.size())
            .toFile(installerPath);
    }

    private Path getPath(InstallerArtifact artifact) {
        Path base = appFiles.getCacheDir().resolve("fabric").resolve("installer");
        return artifact.artifact().jar(base);
    }

    // purgeOldInstallers?
    // maybe haven something like a local MavenRepository Service that handles File based MavenRepositories
    // and allows to delete artifacts

}
