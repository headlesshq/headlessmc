package io.github.headlesshq.headlessmc.net.java;

import io.github.headlesshq.headlessmc.net.DownloadService;

/**
 * Runs all tests of the {@link JavaDownloadTest} with the {@link UrlConnectionDownloadService}.
 */
class UrlConnectionDownloadTest extends JavaDownloadTest {
    @Override
    DownloadService service(Config config) {
        return new UrlConnectionDownloadService(progressBarService, jsonService, hashService, () -> config);
    }

}
