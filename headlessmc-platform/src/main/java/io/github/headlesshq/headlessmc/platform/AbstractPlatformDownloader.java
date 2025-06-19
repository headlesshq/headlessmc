package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.commons.DownloadService;
import io.github.headlesshq.headlessmc.platform.vanilla.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.vanilla.VanillaVersionManager;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.service.VersionService;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Path;

@RequiredArgsConstructor(onConstructor_ = {@Inject})
public abstract class AbstractPlatformDownloader implements PlatformDownloader {
    protected final VanillaVersionManager vanillaVersionManager;
    protected final DownloadService downloadService;
    protected final VersionService versionService;

    protected abstract Path download(Version vanilla, PlatformDownloadOptions options, Path versionJson) throws IOException;

    @Override
    public Path download(PlatformDownloadOptions options) throws IOException {
        Version version = versionService.get(options.getVersionID());
        if (version != null) {
            return versionService.getVersionJsonFile(version);
        }

        VanillaVersion vanillaVersion = vanillaVersionManager.getVersion(options.getVersionID().getVersion());
        if (vanillaVersion == null) {
            throw new IOException("Failed to find vanilla version for " + options.getVersionID());
        }

        Path path = options.getVanillaPathProvider().getPath(options.getVersionID().asVanillaVersion());
        downloadService.download(vanillaVersion.getUrl(), path);
        version = versionService.addOrGet(path);
        return download(version, options, path);
    }

}
