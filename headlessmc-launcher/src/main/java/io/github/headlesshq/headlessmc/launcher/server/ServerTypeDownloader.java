package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.download.DownloadService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public interface ServerTypeDownloader {
    DownloadHandler download(Launcher launcher, String version, @Nullable String typeVersion, String... args) throws IOException;

    interface DownloadHandler {
        Path download(TypeVersionToDownloadPathResolve typeVersionToDownloadPathResolve) throws IOException;
    }

    @FunctionalInterface
    interface TypeVersionToDownloadPathResolve {
        Path resolve(@Nullable String typeVersion) throws IOException;
    }

    @RequiredArgsConstructor
    class UrlJarDownloadHandler implements DownloadHandler {
        private final DownloadService downloadService;
        private final String url;
        private final @Nullable String typeVersion;

        @Override
        public Path download(TypeVersionToDownloadPathResolve typeVersionToDownloadPathResolve) throws IOException {
            Path path = typeVersionToDownloadPathResolve.resolve(typeVersion);
            Files.createDirectories(path);
            downloadService.download(url, path.resolve(Server.DEFAULT_JAR));
            return path;
        }
    }

}
