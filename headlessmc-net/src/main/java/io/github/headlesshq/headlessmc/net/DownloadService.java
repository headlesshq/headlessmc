package io.github.headlesshq.headlessmc.net;

import java.net.URI;

/**
 * A {@code DownloadService} helps with constructing downloads.
 * The default implementation is an HTTP GET request.
 * A singular Download can be build using {@link #download(URI)},
 * if multiple downloads are done from the same API and
 * session, cookies etc. should be reused, a {@link DownloadContext}
 * can be used, via {@link #context()} or {@link #context(NetConfig)}.
 *
 * @see DownloadBuilder
 * @see DownloadContext
 */
public interface DownloadService {
    /**
     * A {@link DownloadContext} can be reused for multiple download requests.
     * This is useful when requests are made to the same data source and
     * the context, session, cookies, etc. should be reused.
     *
     * @return a {@link DownloadContext} for the default configuration.
     */
    DownloadContext context();

    /**
     * A {@link DownloadContext} can be reused for multiple download requests.
     * This is useful when requests are made to the same data source and
     * the context, session, cookies, etc. should be reused.
     *
     * @return a {@link DownloadContext} for the given configuration.
     */
    DownloadContext context(NetConfig config);

    /**
     * Creates a {@link DownloadBuilder} for a single download from
     * a given {@link URI}.
     *
     * @param url the URL to download from.
     * @return a {@link DownloadBuilder} to download from the given URI.
     */
    DownloadBuilder download(URI url);

}
