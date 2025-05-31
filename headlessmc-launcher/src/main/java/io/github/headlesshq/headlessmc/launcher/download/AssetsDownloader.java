package io.github.headlesshq.headlessmc.launcher.download;

import com.google.gson.JsonObject;
import io.github.headlesshq.headlessmc.api.command.CommandLineReader;
import io.github.headlesshq.headlessmc.api.command.Progressbar;
import io.github.headlesshq.headlessmc.api.settings.Config;
import io.github.headlesshq.headlessmc.launcher.cdi.Beans;
import io.github.headlesshq.headlessmc.launcher.files.GameFiles;
import io.github.headlesshq.headlessmc.launcher.settings.AssetSettings;
import io.github.headlesshq.headlessmc.launcher.util.JsonUtil;
import jakarta.inject.Inject;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@CustomLog
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AssetsDownloader {
    private final CommandLineReader commandLineReader;
    private final ChecksumService checksumService;
    private final DownloadService downloadService;
    private final @Beans.AssetsUrl URI assetsUrl;
    private final DummyAssets dummyAssets;
    private final AssetSettings settings;
    private final GameFiles files; // TODO: qualify for injection
    private final Config config;

    @Setter
    protected boolean shouldLog = true;

    // TODO: move these into download method
    private final String url;
    private final String id;

    public void download(String url, String id) throws IOException {
        Path index = files.getMcDirectory().resolve("assets").resolve("indexes").resolve(id + ".json");
        // Why does this file always corrupt on CheerpJ?
        if (config.get(settings.alwaysDownloadAssetIndex()) || !Files.exists(index)) {
            log.info("Downloading assets from " + url);
            downloadService.download(url, index.toAbsolutePath());
        }

        JsonObject objects = JsonUtil.getObject(JsonUtil.fromFile(index.toFile()), "objects");
        if (objects == null || !objects.isJsonObject()) {
            throw new IOException("Couldn't read contents of " + index.toAbsolutePath());
        }

        ParallelIOService ioService = new ParallelIOService(
                config.get(settings.delay()),
                Math.max(1, config.get(settings.retries())),
                config.get(settings.parallel()),
                config.get(settings.backoff())
        );

        // TODO: provide better ETA, later assets take longer
        try (Progressbar progressbar = commandLineReader.displayProgressBar(new Progressbar.Configuration("Downloading Assets", objects.size()))) {
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
        String firstTwo = hash.substring(0, 2);
        Path to = files.getMcDirectory().resolve("assets").resolve("objects").resolve(firstTwo).resolve(hash);
        Path file = getAssetsFile(name, to, hash, size);
        if (!Files.exists(file)) {
            byte[] bytes = null;
            if (config.get(settings.dummyAssets())) {
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
        String from = assetsUrl + firstTwo + "/" + hash;
        if (shouldLog) {
            log.info(progress + " Downloading: " + name + " from " + from + " to " + to);
        }

        boolean checkHash = config.get(settings.checkHash());
        boolean checkSize = checkHash || config.get(settings.checkSize());
        Long expectedSize = checkSize ? size : null;
        String expectedHash = checkHash ? hash : null;
        return downloadService.download(new URL(from), expectedSize, expectedHash);
    }

    protected Path getAssetsFile(String name, Path file, @Nullable String hash, @Nullable Long size) throws IOException {
        integrityCheck("Asset (" + name + ")", file, hash, size);
        return file;
    }

    protected boolean shouldCheckFileHash() {
        return config.get(settings.checkFileHash());
    }

    protected void copyToLegacy(String name, Path file, String hash, @Nullable Long size, boolean copy) throws IOException {
        if ("pre-1.6".equals(id)) {
            // TODO: old versions have the map_to_resource thing, copy to resources
            Path legacy = files.getMcDirectory().resolve("assets").resolve("virtual").resolve("legacy").resolve(name);
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
            Path resources = files.getMcDirectory().resolve("resources").resolve(name);
            log.debug("Mapping " + name + " to resources " + resources);
            integrityCheck("Resources", resources, hash, size);
            if (copy && !Files.exists(resources)) {
                Files.copy(file, resources, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    protected boolean integrityCheck(String type, Path file, @Nullable String hash, @Nullable Long size) throws IOException {
        if (shouldCheckFileHash() && Files.exists(file) && !checksumService.checkIntegrity(file, size, hash)) {
            log.warn(type + " file " + file + " failed the integrity check, deleting...");
            Files.delete(file);
            return false;
        }

        return true;
    }

}
