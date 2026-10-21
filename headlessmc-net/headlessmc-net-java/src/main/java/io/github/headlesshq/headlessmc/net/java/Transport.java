package io.github.headlesshq.headlessmc.net.java;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.OptionalLong;

/**
 * Sends the HTTP GET requests of a {@link JavaDownloadContext}.
 * Everything else (verification, progress bars, retries, ...) is done by the {@link JavaDownloadBuilder}.
 *
 * @see HttpClientTransport
 * @see UrlConnectionTransport
 */
interface Transport extends AutoCloseable {
    /**
     * Sends a GET request.
     *
     * @param uri       the uri to request.
     * @param userAgent the User-Agent header to send.
     * @return the response, the caller has to close its body.
     * @throws IOException if the request fails.
     * @throws InterruptedException if the thread is interrupted while waiting for the response.
     */
    Response send(URI uri, String userAgent) throws IOException, InterruptedException;

    @Override
    void close();

    /**
     * @param statusCode    the HTTP status code.
     * @param contentLength the Content-Length header, if sent.
     * @param body          the response body, the error body for error status codes.
     */
    record Response(int statusCode, OptionalLong contentLength, InputStream body) {
    }

}
