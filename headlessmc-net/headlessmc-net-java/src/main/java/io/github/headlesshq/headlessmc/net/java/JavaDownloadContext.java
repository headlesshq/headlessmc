package io.github.headlesshq.headlessmc.net.java;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import io.github.headlesshq.headlessmc.net.DownloadBuilder;
import io.github.headlesshq.headlessmc.net.DownloadContext;
import io.github.headlesshq.headlessmc.net.NetConfig;
import io.github.headlesshq.headlessmc.net.hash.HashService;
import io.github.headlesshq.headlessmc.progressbar.ProgressBarServiceManager;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;

@RequiredArgsConstructor
final class JavaDownloadContext implements DownloadContext {
    private final ProgressBarServiceManager progressBarServiceManager;
    private final JsonService jsonService;
    private final HashService hashService;
    private final HttpClient httpClient;
    private final NetConfig config;

    public JavaDownloadContext(
        ProgressBarServiceManager progressBarServiceManager,
        JsonService jsonService,
        HashService hashService,
        NetConfig config
    ) {
        this(progressBarServiceManager, jsonService, hashService, getClient(config), config);
    }

    @Override
    public DownloadBuilder download(URI url) {
        return new JavaDownloadBuilder(url, progressBarServiceManager, jsonService, hashService, httpClient, config);
    }

    @Override
    public void close() throws HeadlessMcException {
        httpClient.close();
        if (Thread.interrupted()) {
            throw new UncheckedInterruptedException();
        }
    }

    private static HttpClient getClient(@Nullable NetConfig config) {
        HttpClient.Builder builder = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL);
        if (config == null) {
            return builder.build();
        }

        if (config.cookies()) {
            builder = builder.cookieHandler(new CookieManager());
        }

        if (config.httpVersion().isPresent()) {
            switch (config.httpVersion().get()) {
                case HTTP_1_1 -> builder = builder.version(HttpClient.Version.HTTP_1_1);
                case HTTP_2 -> builder = builder.version(HttpClient.Version.HTTP_2);
                case HTTP_3 -> {
                    // support is coming in JDK 26
                    HttpClient.Version version = HttpClient.Version.valueOf(config.httpVersion().get().name());
                    builder = builder.version(version);
                }
            }
        }

        return builder.build();
    }

}
