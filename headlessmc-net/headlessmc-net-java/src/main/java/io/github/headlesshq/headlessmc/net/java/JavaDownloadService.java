package io.github.headlesshq.headlessmc.net.java;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.net.*;
import io.github.headlesshq.headlessmc.net.hash.HashService;
import io.github.headlesshq.headlessmc.progressbar.ProgressBarServiceManager;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.net.URI;

/**
 * Downloads with {@link java.net.http.HttpClient}.
 * Replaced by the {@link UrlConnectionDownloadService} when building with the {@code url-connection} build profile.
 */
@DefaultBean
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class JavaDownloadService implements DownloadService {
    private final ProgressBarServiceManager progressBarServiceManager;
    private final JsonService jsonService;
    private final HashService hashService;
    private final Holder<NetConfig> config;

    @Override
    public DownloadContext context() {
        return new JavaDownloadContext(progressBarServiceManager, jsonService, hashService, config.get());
    }

    @Override
    public DownloadContext context(NetConfig config) {
        return new JavaDownloadContext(progressBarServiceManager, jsonService, hashService, config);
    }

    @Override
    public DownloadBuilder download(URI url) {
        return new DelegatingDownloadBuilder(this, url);
    }

}
