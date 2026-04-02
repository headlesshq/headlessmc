package io.github.headlesshq.headlessmc.launcher.download;

import com.google.gson.JsonObject;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.val;
import io.github.headlesshq.headlessmc.api.command.line.CommandLine;
import io.github.headlesshq.headlessmc.api.command.line.Progressbar;
import io.github.headlesshq.headlessmc.api.config.HasConfig;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.files.FileManager;
import io.github.headlesshq.headlessmc.launcher.util.JsonUtil;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@CustomLog
@RequiredArgsConstructor
public class AssetsDownloader {
    private static final String URL = "https://resources.download.minecraft.net/";

    private final ChecksumService checksumService = new ChecksumService();
    private final DummyAssets dummyAssets = new DummyAssets();

    private final CommandLine commandLine;
    private final DownloadService downloadService;
    private final HasConfig config;
    private final FileManager files;
    private final String url;
    private final String id;

    @Setter
    protected boolean shouldLog = true;

    public void download() throws IOException {
        Path index = files.getDir("assets").toPath().resolve("indexes").resolve(id + ".json");
        // Why does this file always corrupt on CheerpJ?
        if (config.getConfig().get(LauncherProperties.ALWAYS_DOWNLOAD_ASSETS_INDEX, false) || !Files.exists(index)) {
            log.info("Downloading assets from " + url);
            downloadService.download(url, index.toAbsolutePath());
        }

        JsonObject objects = JsonUtil.getObject(JsonUtil.fromFile(index.toFile()), "objects");
        if (objects == null || !objects.isJsonObject()) {
            throw new IOException("Couldn't read contents of " + index.toAbsolutePath());
        }

        ParallelIOService ioService = new ParallelIOService(
                config.getConfig().get(LauncherProperties.ASSETS_DELAY, 2L),
                Math.max(1, config.getConfig().get(LauncherProperties.ASSETS_RETRIES, 3L).intValue()),
                config.getConfig().get(LauncherProperties.ASSETS_PARALLEL, true),
                config.getConfig().get(LauncherProperties.ASSETS_BACKOFF, true)
        );

        // TODO: provide better ETA, later assets take longer
        try (Progressbar progressbar = commandLine.displayProgressBar(new Progressbar.Configuration("Downloading Assets", objects.size()))) {
            ioService.setShouldLog(progressbar.isDummy());
            shouldLog = progressbar.isDummy();

            objects.getAsJsonObject().entrySet().forEach(entry -> ioService.addTask(progress -> {
                JsonObject jo = entry.getValue().getAsJsonObject();
                downloadAsset(
                        progress,
                        entry.getKey(),
                        jo.get("hash").getAsString(),
                        jo.get("size") == null ? null : jo.get("size").getAsLong(),
                        jo.get("map_to_resources") != null && jo.get("map_to_resources").getAsBoolean()
                );

                progressbar.step();
            }));

            ioService.execute();
        }
    }

    protected void downloadAsset(String progress, String name, String hash, @Nullable Long size, boolean mapToResources) throws IOException {
        val firstTwo = hash.substring(0, 2);
        val to = files.getDir("assets").toPath().resolve("objects").resolve(firstTwo).resolve(hash);
        Path file = getAssetsFile(name, to, hash, size);
        if (!Files.exists(file)) {
            byte[] bytes = null;
            if (config.getConfig().get(LauncherProperties.DUMMY_ASSETS, false)) {
                log.debug("Using dummy asset for " + name);
                bytes = dummyAssets.getResource(name);
            }

            if (bytes == null) {
                bytes = download(firstTwo, hash, progress, name, to, size);
            }

            if (bytes != null) {
                Files.createDirectories(to.getParent());
                try (OutputStream os = Files.newOutputStream(to)) {
                    os.write(bytes);
                }
            }
        }

        copyToLegacy(name, file, hash, size, true);
        mapToResources(name, file, mapToResources, hash, size, true);
    }

    protected byte @Nullable [] download(String firstTwo, String hash, String progress, String name, Path to, @Nullable Long size) throws IOException {
        val from = URL + firstTwo + "/" + hash;
        if (shouldLog) {
            log.info(progress + " Downloading: " + name + " from " + from + " to " + to);
        }

        boolean checkHash = config.getConfig().get(LauncherProperties.ASSETS_CHECK_HASH, true);
        boolean checkSize = checkHash || config.getConfig().get(LauncherProperties.ASSETS_CHECK_SIZE, true);
        Long expectedSize = checkSize ? size : null;
        String expectedHash = checkHash ? hash : null;
        return downloadService.download(new URL(from), expectedSize, expectedHash);
    }

    protected Path getAssetsFile(String name, Path file, @Nullable String hash, @Nullable Long size) throws IOException {
        integrityCheck("Asset (" + name + ")", file, hash, size);
        return file;
    }

    protected boolean shouldCheckFileHash() {
        return config.getConfig().get(LauncherProperties.ASSETS_CHECK_FILE_HASH, false);
    }

    protected void copyToLegacy(String name, Path file, String hash, @Nullable Long size, boolean copy) throws IOException {
        if ("pre-1.6".equals(id)) {
            // TODO: old versions have the map_to_resource thing, copy to resources
            val legacy = files.getDir("assets").toPath().resolve("virtual").resolve("legacy").resolve(name);
            if (shouldLog) {
                log.info("Legacy version, copying to " + legacy);
            } else {
                log.debug("Legacy version, copying to " + legacy);
            }

            integrityCheck("Legacy", legacy, hash, size);
            if (copy && !Files.exists(legacy)) {
                Files.copy(file, legacy, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    protected void mapToResources(String name, Path file, boolean mapToResources, String hash, @Nullable Long size, boolean copy) throws IOException {
        if (mapToResources) {
            val resources = files.getDir("resources").toPath().resolve(name);
            log.debug("Mapping " + name + " to resources " + resources);
            integrityCheck("Resources", resources, hash, size);
            if (copy && !Files.exists(resources)) {
                Files.copy(file, resources, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    protected boolean integrityCheck(String type, Path file, String hash, @Nullable Long size) throws IOException {
        if (shouldCheckFileHash() && Files.exists(file) && !checksumService.checkIntegrity(file, size, hash)) {
            log.warn(type + " file " + file + " failed the integrity check, deleting...");
            Files.delete(file);
            return false;
        }

        return true;
    }

}
