package io.github.headlesshq.headlessmc.net;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import lombok.RequiredArgsConstructor;

import java.net.URI;

@RequiredArgsConstructor
public class MockDownloadContext implements DownloadContext {
    private final MockDownloadService mockDownloadService;

    @Override
    public DownloadBuilder download(URI url) {
        return new MockDownloadBuilder(url, mockDownloadService);
    }

    @Override
    public void close() throws HeadlessMcException {

    }

}
