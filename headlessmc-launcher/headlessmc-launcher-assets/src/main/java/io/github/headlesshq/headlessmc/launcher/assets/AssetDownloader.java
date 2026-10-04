package io.github.headlesshq.headlessmc.launcher.assets;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.net.DownloadContext;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.net.NetConfig;
import io.github.headlesshq.headlessmc.net.hash.HashService;
import io.github.headlesshq.headlessmc.net.parallel.ParallelTaskService;
import io.github.headlesshq.headlessmc.progressbar.ProgressBar;
import io.github.headlesshq.headlessmc.progressbar.ProgressbarService;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.version.Version;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AssetDownloader {
    final ProgressbarService progressbarService;
    final DownloadService downloadService;
    final FileService fileService;
    final HashService hashService;
    final DummyAssets dummyAssets;
    final Holder<AssetsConfig> config;
    final McFiles mcFiles;
    final JsonService jsonService;

    public AssetsLocation download(Version.AssetIndex assetIndex) throws HeadlessMcException {
        Objects.requireNonNull(assetIndex.getId(), "AssetIndex id was null");
        Objects.requireNonNull(assetIndex.getUrl(), "AssetIndex url was null");
        Path index = fileService.getPath(mcFiles.getAssetsDir(), "indexes", assetIndex.getId() + ".json");
        AssetsConfig config = new Http1AssetsConfig(this.config.get());
        NetConfig downloadConfig = config.net();

        try (DownloadContext downloadContext = downloadService.context(downloadConfig)) {
            if (!Files.exists(index)) {
                downloadContext.download(URI.create(assetIndex.getUrl()))
                    .sha1(assetIndex.getSha1())
                    .size(assetIndex.getSize())
                    .toFile(index);
            }

            long totalSize = 0L;
            AssetIndex assetIndexJson = jsonService.parse(index, AssetIndex.class);
            // TODO: check map_to_resource/virtual
            List<AssetDownloadTask> tasks = new ArrayList<>(assetIndexJson.objects().size());
            for (Map.Entry<String, AssetIndex.Download> entry : assetIndexJson.objects().entrySet()) {
                String fileName = entry.getKey();
                AssetIndex.Download download = entry.getValue();
                totalSize += download.size();
                
                AssetDownloadTask task = downloadTask(downloadContext, assetIndex, assetIndexJson, fileName, download);
                if (task.needsDownload()) {
                    tasks.add(task);
                } else {  // no download needed, just run local part
                    task.copyToVirtual();
                    task.mapToResources();
                }
            }

            Long expectedSize = assetIndex.getTotalSize();
            if (expectedSize != null && !expectedSize.equals(totalSize)) {
                log.error("Expected asset index to be {} bytes but was {}: {}", expectedSize, totalSize, assetIndex);
            }

            if (!tasks.isEmpty()) {
                download(config, tasks, totalSize);
            }

            return new AssetsLocation(
                assetIndex.getId(),
                assetIndexJson.isVirtual()
                    ? getVirtualDir(assetIndex)
                    : assetIndexJson.mapToResources()
                        ? mcFiles.getResourcesDir()
                        : mcFiles.getAssetsDir()
            );
        }
    }

    Path getVirtualDir(Version.AssetIndex assetIndex) {
        return fileService.getPath(
            mcFiles.getAssetsDir(),
            "virtual",
            Objects.requireNonNull(assetIndex.getId(), "Version AssetIndex id was null")
        );
    }

    private void download(AssetsConfig config, List<AssetDownloadTask> tasks, long totalSize) {
        try (
            ParallelTaskService<AssetDownloadTask> taskService = new ParallelTaskService<>(config.parallel(), tasks);
            ProgressBar progressBar = progressbarService.displayProgressBar(new ProgressBar.Configuration(
                "Downloading Assets",
                totalSize,
                ProgressBar.Configuration.Unit.MB
            ))
        ) {
            AtomicInteger finishedTasks = new AtomicInteger(0);
            taskService.run(task -> {
                if (progressBar.isDummy()) {
                    log.info("Downloaded asset {}/{}", finishedTasks.incrementAndGet(), tasks.size());
                }

                progressBar.stepBy(task.getSize());
            });

            progressBar.stepTo(totalSize);
        }
    }

    private AssetDownloadTask downloadTask(
        DownloadContext context,
        Version.AssetIndex versionAssetIndex,
        AssetIndex assetIndex,
        String fileName,
        AssetIndex.Download download
    ) {
        String hash = download.hash();
        String firstTwo = hash.substring(0, 2);
        Path file = fileService.getPath(mcFiles.getAssetsDir(), "objects", firstTwo, hash);
        URI url = URI.create(config.get().url() + "/" + firstTwo + "/" + hash);
        return new AssetDownloadTask(versionAssetIndex, context, download, assetIndex, this, fileName, file, url);
    }

}
