package io.github.headlesshq.headlessmc.net.java;

import io.github.headlesshq.headlessmc.net.NetConfig;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

/**
 * A {@link Transport} using {@link HttpURLConnection}, for platforms without {@code java.net.http},
 * like Android, or without the NIO channels the HttpClient needs, like CheerpJ in the browser,
 * where {@link HttpURLConnection} is implemented with fetch().
 * The HTTP version of the {@link NetConfig} is ignored.
 */
@Slf4j
final class UrlConnectionTransport implements Transport {
    private static final int MAX_REDIRECTS = 10;

    private final @Nullable CookieManager cookies;

    UrlConnectionTransport(@Nullable NetConfig config) {
        // cookies are kept per transport, not with the global CookieHandler
        this.cookies = config == null || config.cookies() ? new CookieManager() : null;
    }

    @Override
    public Response send(URI uri, String userAgent) throws IOException, InterruptedException {
        URI current = uri;
        for (int redirects = 0; ; redirects++) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            URLConnection urlConnection = current.toURL().openConnection();
            if (!(urlConnection instanceof HttpURLConnection connection)) {
                throw new IOException("Not an HTTP url: " + current);
            }

            // followed manually, HttpURLConnection does not follow redirects from http to https
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", userAgent);
            addCookies(current, connection);
            int statusCode = connection.getResponseCode();
            storeCookies(current, connection);

            String location = connection.getHeaderField("Location");
            if (isRedirect(statusCode) && location != null) {
                connection.disconnect();
                if (redirects >= MAX_REDIRECTS) {
                    throw new IOException("Too many redirects for " + uri);
                }

                current = current.resolve(location);
                log.debug("Redirected to {}", current);
                continue;
            }

            long contentLength = connection.getContentLengthLong();
            return new Response(
                statusCode,
                contentLength < 0 ? OptionalLong.empty() : OptionalLong.of(contentLength),
                body(connection, statusCode)
            );
        }
    }

    @Override
    public void close() {
        // HttpURLConnections are pooled by the JDK
    }

    private InputStream body(HttpURLConnection connection, int statusCode) throws IOException {
        InputStream body = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        return body == null ? new ByteArrayInputStream(new byte[0]) : body;
    }

    private void addCookies(URI uri, HttpURLConnection connection) throws IOException {
        if (cookies != null) {
            for (Map.Entry<String, List<String>> header : cookies.get(uri, Map.of()).entrySet()) {
                if (!header.getValue().isEmpty()) {
                    connection.setRequestProperty(header.getKey(), String.join("; ", header.getValue()));
                }
            }
        }
    }

    private void storeCookies(URI uri, HttpURLConnection connection) throws IOException {
        if (cookies != null) {
            cookies.put(uri, connection.getHeaderFields());
        }
    }

    private static boolean isRedirect(int statusCode) {
        return statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308;
    }

}
