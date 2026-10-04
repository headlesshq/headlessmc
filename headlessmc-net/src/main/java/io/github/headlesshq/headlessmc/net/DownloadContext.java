package io.github.headlesshq.headlessmc.net;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;

import java.net.URI;

/**
 *
 */
public interface DownloadContext extends AutoCloseable {
    // follow redirects? Cookies? Retries? connect timeout? read timeout?

    DownloadBuilder download(URI url);

    @Override
    void close() throws HeadlessMcException;

}
