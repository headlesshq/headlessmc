package io.github.headlesshq.headlessmc.launcher.libraries;

import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.version.Version;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class McJarDownloader {
    private final DownloadService downloadService;
    private final McFiles mcFiles;

    public Path downloadMcJar(String key, Version version) {
        Path file = mcFiles.getVersionsDir().resolve(version.getId()).resolve(version.getId() + ".jar");
        if (!Files.exists(file)) {
            Version.Download download = Optional.ofNullable(version.getDownloads())
                .map(downloads -> downloads.get(key))
                .orElseThrow(() -> new LibraryException(
                    "Failed to download %s jar from version %s, no such download: %s".formatted(
                        key,
                        version.getId(),
                        version.getDownloads()
                    )));

            downloadService.download(
                    URI.create(Objects.requireNonNull(
                        download.getUrl(),
                        key + " jar download had no URL in " + version.getId() + ": " + version.getDownloads()
                    ))
                ).sha1(download.getSha1())
                .size(download.getSize())
                .toFile(file);
        }

        return file;
    }

}
