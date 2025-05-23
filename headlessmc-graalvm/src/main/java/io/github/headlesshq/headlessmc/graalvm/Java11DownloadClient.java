package io.github.headlesshq.headlessmc.graalvm;

import io.github.headlesshq.headlessmc.api.command.ProgressBarProvider;
import io.github.headlesshq.headlessmc.api.command.Progressbar;
import io.github.headlesshq.headlessmc.java.download.DownloadClient;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * A {@link DownloadClient} using the Java 11 {@link HttpClient}.
 */
public class Java11DownloadClient implements DownloadClient {
    @Override
    public String httpGetText(String url) throws IOException {
        try (HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()) {
            HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (!(response.statusCode() >= 200 && response.statusCode() < 400)) {
                throw new IOException("HTTP error code: " + response.statusCode());
            }

            return response.body();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void downloadBigFile(String url, Path destination, String progressBarTitle, ProgressBarProvider progressBarProvider) throws IOException {
        try (HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()) {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (!(response.statusCode() >= 200 && response.statusCode() < 400)) {
                throw new IOError(new IOException("HTTP error code: " + response.statusCode()));
            }

            long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
            try (InputStream is = response.body();
                 OutputStream os = Files.newOutputStream(destination, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 Progressbar progressbar = progressBarProvider.displayProgressBar(
                         new Progressbar.Configuration(progressBarTitle, contentLength, new Progressbar.Configuration.Unit("mb", 1_000_000)))) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                    progressbar.stepBy(bytesRead);
                }
            }
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

}
