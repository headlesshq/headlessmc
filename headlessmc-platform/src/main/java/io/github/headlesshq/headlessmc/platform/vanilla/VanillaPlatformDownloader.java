package io.github.headlesshq.headlessmc.platform.vanilla;

import io.github.headlesshq.headlessmc.commons.DownloadService;
import io.github.headlesshq.headlessmc.platform.AbstractPlatformDownloader;
import io.github.headlesshq.headlessmc.platform.PlatformDownloadOptions;
import io.github.headlesshq.headlessmc.platform.PlatformDownloader;
import io.github.headlesshq.headlessmc.version.Download;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.service.VersionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Path;

@Vanilla
@ApplicationScoped
public class VanillaPlatformDownloader extends AbstractPlatformDownloader implements PlatformDownloader {
    @Inject
    public VanillaPlatformDownloader(VanillaVersionManager vanillaVersionManager,
                                     DownloadService downloadService,
                                     VersionService versionService) {
        super(vanillaVersionManager, downloadService, versionService);
    }

    @Override
    public Path download(PlatformDownloadOptions options) throws IOException {
        Version version = versionService.get(options.getVersionID().asVanillaVersion());
        if (version != null && !options.getVersionID().isServer()) {
            return versionService.getVersionJsonFile(version);
        }

        return super.download(options);
    }

    @Override
    protected Path download(Version version, PlatformDownloadOptions options, Path versionJson) throws IOException {
        return options.getVersionID().isServer() ? downloadServer(version, options) : versionJson;
    }

    private Path downloadServer(Version version, PlatformDownloadOptions options) throws IOException {
        Download download = version.getServerDownload();
        if (download == null || download.getUrl() == null) {
            throw new IOException("Failed to find server download for " + version.getName());
        }

        Path path = options.getPathProvider().getPath(options.getVersionID());
        downloadService.download(download.getUrl(), path, download.getSize(), download.getSha1());
        return path;
    }

}
