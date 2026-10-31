package io.github.headlesshq.headlessmc.quilt;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.net.hash.HashService;
import io.github.headlesshq.headlessmc.util.json.JsonParseException;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.quarkus.runtime.annotations.RegisterResources;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Downloads the Quilt installer and verifies it against {@link InstallerArtifact#RESOURCE}.
 * The resource is registered, so that it is included when HeadlessMc is compiled into a native image
 * with this plugin ({@code plugin compile}).
 */
@ApplicationScoped
@RegisterResources(globs = InstallerArtifact.RESOURCE)
class QuiltInstallerDownloader {
    private final DownloadService downloadService;
    private final FileService fileService;
    private final AppFiles appFiles;
    private final InstallerArtifact installer;

    @Inject
    QuiltInstallerDownloader(DownloadService downloadService, FileService fileService, AppFiles appFiles, JsonService jsonService) {
        this.downloadService = downloadService;
        this.fileService = fileService;
        this.appFiles = appFiles;
        this.installer = load(jsonService);
    }

    public Path download(@Nullable URI customInstallerURI) throws HeadlessMcException {
        if (customInstallerURI != null) {
            Path path = fileService.getPath(appFiles.getCacheDir(), "quilt-custom-installer.jar");
            return downloadService.download(customInstallerURI).toFile(path);
        }

        Path path = appFiles.getCacheDir().resolve(Quilt.PLATFORM_NAME).resolve("installer").resolve(installer.fileName());
        if (Files.exists(path)) {
            return path;
        }

        return downloadService.download(installer.getURL())
            .hash(HashService.SHA256, installer.sha256())
            .size(installer.size())
            .toFile(path);
    }

    private static InstallerArtifact load(JsonService jsonService) {
        try (InputStream stream = QuiltInstallerDownloader.class.getClassLoader().getResourceAsStream(InstallerArtifact.RESOURCE)) {
            Objects.requireNonNull(stream, InstallerArtifact.RESOURCE + " not found");
            return jsonService.parse(stream, InstallerArtifact.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + InstallerArtifact.RESOURCE, e);
        } catch (JsonParseException e) {
            throw new IllegalStateException("Failed to parse " + InstallerArtifact.RESOURCE, e);
        }
    }

}
