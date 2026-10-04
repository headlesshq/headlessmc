package io.github.headlesshq.headlessmc.net;

import io.github.headlesshq.headlessmc.exceptions.RequestException;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A {@link DownloadService} for tests that does not make network connections.
 */
@Alternative
@Priority(1)
@ApplicationScoped
public class MockDownloadService implements DownloadService {
    private final Map<URI, Supplier<Download>> mockDownloads = new HashMap<>();
    private final List<URI> requestedUris = new ArrayList<>();

    public MockDownloadService register(URI uri, byte[] bytes) {
        return register(uri, 200, bytes);
    }

    public MockDownloadService register(URI uri, String content) {
        return register(uri, content.getBytes(StandardCharsets.UTF_8));
    }

    public MockDownloadService register(URI uri, int statusCode, byte[] bytes) {
        return register(uri, () -> new DownloadImpl(
            Optional.of((long) bytes.length),
            statusCode,
            new ByteArrayInputStream(bytes)
        ));
    }

    public MockDownloadService register(URI uri, Supplier<Download> download) {
        mockDownloads.put(uri, download);
        return this;
    }

    public List<URI> getRequestedUris() {
        return requestedUris;
    }

    public boolean wasRequested(URI uri) {
        return requestedUris.contains(uri);
    }

    Download open(URI uri) throws RequestException {
        requestedUris.add(uri);
        Supplier<Download> download = mockDownloads.get(uri);
        if (download == null) {
            throw new RequestException("No mock download registered for " + uri);
        }

        return download.get();
    }

    @Override
    public DownloadContext context() {
        return new MockDownloadContext(this);
    }

    @Override
    public DownloadContext context(NetConfig config) {
        return new MockDownloadContext(this);
    }

    @Override
    public DownloadBuilder download(URI url) {
        return new DelegatingDownloadBuilder(this, url);
    }

}
