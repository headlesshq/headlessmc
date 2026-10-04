package io.github.headlesshq.headlessmc.launcher.assets;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.exceptions.VerificationException;
import io.github.headlesshq.headlessmc.net.DownloadContext;
import io.github.headlesshq.headlessmc.net.DownloadRetry;
import io.github.headlesshq.headlessmc.net.parallel.ParallelTask;
import io.github.headlesshq.headlessmc.version.Version;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@RequiredArgsConstructor
final class AssetDownloadTask implements ParallelTask {
    final Version.AssetIndex versionAssetIndex;
    final DownloadContext downloadContext;
    final AssetIndex.Download download;
    final AssetIndex assetIndex;
    final AssetDownloader ctx;
    final String fileName;
    final Path file;
    final URI url;

    @Override
    public void run() throws HeadlessMcException, IOException {
        // needsDownload has been called before, so the file is either missing or failed verification
        download();
        copyToVirtual();
        mapToResources();
    }

    @Override
    public long getSize() {
        return download.size();
    }

    boolean needsDownload() throws HeadlessMcException {
        if (!Files.exists(file)) {
            return true;
        }

        if (ctx.config.get().dummy()) {
            return false;
        }

        try {
            ctx.hashService.verifier()
                .sha1(download.hash())
                .size(download.size())
                .verify(file);
            return false;
        } catch (VerificationException e) {
            log.info("Asset {} at {} failed verification", fileName, file, e);
            return true;
        }
    }

    void download() throws IOException {
        if (ctx.config.get().dummy()) {
            try (InputStream inputStream = ctx.dummyAssets.getResource(fileName)) {
                if (inputStream != null) {
                    ctx.fileService.create(file, inputStream);
                    return;
                }
            }
        }

        downloadContext.download(url)
            .retry(DownloadRetry.none()) // the ParallelTaskService does this for us
            .sha1(download.hash())
            .size(download.size())
            .toFile(file);
    }

    void copyToVirtual() throws HeadlessMcIOException {
        if (!assetIndex.isVirtual()) {
            return;
        }

        // currently this is only .minecraft/assets/virtual/legacy
        // but the Ubuntu official Mc launcher starts 1.4.5 with
        // --assetsDir .minecraft/assets/virtual/pre-1.6
        // which could be a bug, as that dir is empty and for pre-1.6
        // we map_to_resources.
        try {
            Path legacy = ctx.fileService.getPath(ctx.getVirtualDir(versionAssetIndex), fileName);
            Files.copy(file, legacy, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new HeadlessMcIOException(e);
        }
    }

    void mapToResources() throws HeadlessMcIOException {
        if (!assetIndex.mapToResources()) {
            return;
        }

        try {
            // TODO: this should probably be the game dir?
            Path resources = ctx.fileService.getPath(ctx.mcFiles.getResourcesDir(), fileName);
            Files.copy(file, resources, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new HeadlessMcIOException(e);
        }
    }

}
