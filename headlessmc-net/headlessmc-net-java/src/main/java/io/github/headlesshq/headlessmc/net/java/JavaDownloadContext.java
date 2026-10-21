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

import java.net.URI;

@RequiredArgsConstructor
final class JavaDownloadContext implements DownloadContext {
    private final ProgressBarServiceManager progressBarServiceManager;
    private final JsonService jsonService;
    private final HashService hashService;
    private final Transport transport;
    private final NetConfig config;

    public JavaDownloadContext(
        ProgressBarServiceManager progressBarServiceManager,
        JsonService jsonService,
        HashService hashService,
        NetConfig config
    ) {
        this(progressBarServiceManager, jsonService, hashService, new HttpClientTransport(config), config);
    }

    @Override
    public DownloadBuilder download(URI url) {
        return new JavaDownloadBuilder(url, progressBarServiceManager, jsonService, hashService, transport, config);
    }

    @Override
    public void close() throws HeadlessMcException {
        try {
            transport.close();
        } catch (RuntimeException e) {
            throw new HeadlessMcException(e);
        }

        if (Thread.interrupted()) {
            throw new UncheckedInterruptedException();
        }
    }

}
