package io.github.headlesshq.headlessmc.java.download;

import io.github.headlesshq.headlessmc.api.HeadlessMc;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface JavaDownloader {
    default void download(JavaDownloadRequest version) throws IOException {
        download(Paths.get(HeadlessMc.NAME).resolve("java"), version);
    }

    void download(Path javaVersionsDir, JavaDownloadRequest request) throws IOException;

}
