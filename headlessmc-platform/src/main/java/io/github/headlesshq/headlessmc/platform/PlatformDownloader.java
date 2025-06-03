package io.github.headlesshq.headlessmc.platform;

import java.io.IOException;

public interface PlatformDownloader {
    void download(PlatformDownloadOptions options) throws IOException;

}
