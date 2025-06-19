package io.github.headlesshq.headlessmc.platform;

import java.io.IOException;
import java.nio.file.Path;

public interface PlatformDownloader {
    Path download(PlatformDownloadOptions options) throws IOException;

}
