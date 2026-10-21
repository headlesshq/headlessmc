package io.github.headlesshq.headlessmc.net.java;

import io.github.headlesshq.headlessmc.net.NetConfig;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * A {@link Transport} using {@link HttpClient}.
 */
@RequiredArgsConstructor
final class HttpClientTransport implements Transport {
    private final HttpClient httpClient;

    HttpClientTransport(@Nullable NetConfig config) {
        this(getClient(config));
    }

    @Override
    public Response send(URI uri, String userAgent) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .header("User-Agent", userAgent)
            .uri(uri)
            .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        return new Response(
            response.statusCode(),
            response.headers().firstValueAsLong("Content-Length"),
            response.body()
        );
    }

    @Override
    public void close() {
        // HttpClient only implements AutoCloseable since Java 21
        if (httpClient instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
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
