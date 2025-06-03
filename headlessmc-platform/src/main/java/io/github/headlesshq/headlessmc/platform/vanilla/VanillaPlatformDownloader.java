package io.github.headlesshq.headlessmc.platform.vanilla;

import io.github.headlesshq.headlessmc.platform.PlatformDownloadOptions;
import io.github.headlesshq.headlessmc.platform.PlatformDownloader;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;

@Vanilla
@ApplicationScoped
public class VanillaPlatformDownloader implements PlatformDownloader {
    @Override
    public void download(PlatformDownloadOptions options) throws IOException {
        if (options.getVersionID().isServer()) {

        } else {

        }
    }

}
