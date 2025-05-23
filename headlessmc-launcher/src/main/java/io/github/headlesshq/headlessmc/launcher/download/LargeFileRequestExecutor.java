package io.github.headlesshq.headlessmc.launcher.download;

import io.github.headlesshq.headlessmc.api.command.ProgressBarProvider;
import io.github.headlesshq.headlessmc.api.command.Progressbar;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.executor.RequestExecutor;
import net.lenni0451.commons.httpclient.proxy.SingleProxySelector;
import net.lenni0451.commons.httpclient.requests.HttpContentRequest;
import net.lenni0451.commons.httpclient.requests.HttpRequest;
import net.lenni0451.commons.httpclient.utils.HttpRequestUtils;
import net.lenni0451.commons.httpclient.utils.IgnoringTrustManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This is just {@link net.lenni0451.commons.httpclient.executor.URLConnectionExecutor}
 * but the InputStream from the connection is written to the given file.
 */
final class LargeFileRequestExecutor extends RequestExecutor {
    private final ProgressBarProvider progressBarProvider;
    private final String progressBarTitle;
    private final Path file;

    public LargeFileRequestExecutor(HttpClient client, ProgressBarProvider progressBarProvider, String progressBarTitle, Path file) {
        super(client);
        this.progressBarProvider = progressBarProvider;
        this.progressBarTitle = progressBarTitle;
        this.file = file;
    }

    @Override
    public @NotNull HttpResponse execute(@NotNull HttpRequest request) throws IOException {
        CookieManager cookieManager = this.getCookieManager(request);
        HttpURLConnection connection = this.openConnection(request, cookieManager);
        return this.executeRequest(connection, cookieManager, request);
    }

    private HttpURLConnection openConnection(final HttpRequest request, final CookieManager cookieManager) throws IOException {
        SingleProxySelector proxySelector = null;
        if (this.client.getProxyHandler().isProxySet()) proxySelector = this.client.getProxyHandler().getProxySelector();
        try {
            if (proxySelector != null) proxySelector.set();
            URL url = request.getURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (this.isIgnoreInvalidSSL(request) && connection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                httpsConnection.setSSLSocketFactory(IgnoringTrustManager.makeIgnoringSSLContext().getSocketFactory());
            }
            this.setupConnection(connection, cookieManager, request);
            connection.connect();
            return connection;
        } finally {
            if (proxySelector != null) proxySelector.reset();
        }
    }

    private void setupConnection(HttpURLConnection connection, @Nullable CookieManager cookieManager, HttpRequest request) throws IOException {
        HttpRequestUtils.setHeaders(connection, this.getHeaders(request, cookieManager));
        connection.setConnectTimeout(this.client.getConnectTimeout());
        connection.setReadTimeout(this.client.getReadTimeout());
        connection.setRequestMethod(request.getMethod());
        connection.setDoInput(true);
        connection.setDoOutput(request instanceof HttpContentRequest && ((HttpContentRequest) request).getContent() != null);
        switch (request.getFollowRedirects()) {
            case NOT_SET:
                connection.setInstanceFollowRedirects(this.client.isFollowRedirects());
                break;
            case FOLLOW:
                connection.setInstanceFollowRedirects(true);
                break;
            case IGNORE:
                connection.setInstanceFollowRedirects(false);
                break;
        }
    }

    private HttpResponse executeRequest(HttpURLConnection connection, @Nullable CookieManager cookieManager, HttpRequest request) throws IOException {
        try {
            if (connection.getDoOutput()) {
                OutputStream os = connection.getOutputStream();
                os.write(Objects.requireNonNull(((HttpContentRequest) request).getContent()).getAsBytes());
                os.flush();
            }

            InputStream inputStream;
            if (connection.getResponseCode() >= 400) inputStream = connection.getErrorStream();
            else inputStream = connection.getInputStream();

            long contentLength = connection.getHeaderFields().getOrDefault("Content-Length", Collections.emptyList()).stream().map(Long::parseLong).findFirst().orElse(-1L);
            try (InputStream is = inputStream;
                 OutputStream os = Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 Progressbar progressbar = progressBarProvider.displayProgressBar(
                         new Progressbar.Configuration(progressBarTitle, contentLength, new Progressbar.Configuration.Unit("mb", 1_000_000))))
            {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                    progressbar.stepBy(bytesRead);
                }
            } catch (IOException e) {
                throw new IOError(e);
            }

            byte[] body = new byte[0];
            HttpResponse response = new HttpResponse(
                    request.getURL(),
                    connection.getResponseCode(),
                    body,
                    connection
                            .getHeaderFields()
                            .entrySet()
                            .stream()
                            .filter(e -> e.getKey() != null)
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            );
            HttpRequestUtils.updateCookies(cookieManager, request.getURL(), connection.getHeaderFields());
            return response;
        } finally {
            connection.disconnect();
        }
    }

}
