package io.github.headlesshq.headlessmc.net.java;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.net.DelegatingDownloadBuilder;
import io.github.headlesshq.headlessmc.net.DownloadBuilder;
import io.github.headlesshq.headlessmc.net.DownloadContext;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.net.NetConfig;
import io.github.headlesshq.headlessmc.net.hash.HashService;
import io.github.headlesshq.headlessmc.progressbar.ProgressBarServiceManager;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.net.URI;

/**
 * A {@link DownloadService} using {@link java.net.HttpURLConnection} instead of {@link java.net.http.HttpClient},
 * see {@link UrlConnectionTransport}. Replaces the {@link JavaDownloadService} when building with the
 * {@code url-connection} build profile ({@code -PurlConnection}).
 */
@ApplicationScoped
@IfBuildProfile(UrlConnectionDownloadService.PROFILE)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class UrlConnectionDownloadService implements DownloadService {
    /**
     * The Quarkus build profile that enables this service.
     */
    public static final String PROFILE = "url-connection";

    private final ProgressBarServiceManager progressBarServiceManager;
    private final JsonService jsonService;
    private final HashService hashService;
    private final Holder<NetConfig> config;

    @Override
    public DownloadContext context() {
        return context(config.get());
    }

    @Override
    public DownloadContext context(NetConfig config) {
        return new JavaDownloadContext(
            progressBarServiceManager, jsonService, hashService, new UrlConnectionTransport(config), config
        );
    }

    @Override
    public DownloadBuilder download(URI url) {
        return new DelegatingDownloadBuilder(this, url);
    }

}
