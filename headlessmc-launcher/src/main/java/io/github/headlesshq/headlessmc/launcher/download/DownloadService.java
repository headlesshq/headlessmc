package io.github.headlesshq.headlessmc.launcher.download;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import io.github.headlesshq.headlessmc.api.command.ProgressBarProvider;
import io.github.headlesshq.headlessmc.java.download.DownloadClient;
import io.github.headlesshq.headlessmc.launcher.files.IOService;
import io.github.headlesshq.headlessmc.launcher.util.IOConsumer;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.RetryHandler;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.function.Supplier;

@Getter
@RequiredArgsConstructor
public class DownloadService extends IOService implements DownloadClient {
    private final ChecksumService defaultChecksumService = new ChecksumService();
    private final ChecksumService checksumService;
    @Setter
    private Supplier<HttpClient> httpClientFactory = this::getDefaultHttpClient;

    public DownloadService() {
        this(new ChecksumService());
    }

    public void download(String from, Path to) throws IOException {
        download(from, to, null, null);
    }

    public void download(String from, Path to, @Nullable Long size, @Nullable String hash) throws IOException {
        download(new URL(from), to, size, hash);
    }

    public void download(URL from, Path to, @Nullable Long size, @Nullable String hash) throws IOException {
        download(from, size, hash, bytes -> writeToFile(to, bytes));
    }

    public void download(URL from, @Nullable Long size, @Nullable String hash, IOConsumer<byte[]> action) throws IOException {
        HttpResponse response = download(from);
        byte[] bytes = response.getContent();
        if (!checksumService.checkIntegrity(bytes, size, hash)) {
            throw new IOException("Failed to verify checksum! " + hash + " vs " + checksumService.hash(bytes));
        }

        action.accept(bytes);
    }

    public HttpResponse download(URL from) throws IOException {
        HttpResponse httpResponse = get(from);
        if (httpResponse.getStatusCode() > 299 || httpResponse.getStatusCode() < 200) {
            throw new IOException("Failed to get " + from + ", response " + httpResponse.getStatusCode() + ": " + httpResponse.getContentAsString());
        }

        return httpResponse;
    }

    public byte[] download(URL from, @Nullable Long size, @Nullable String hash) throws IOException {
        byte[][] ref = new byte[1][];
        download(from, size, hash, bytes -> ref[0] = bytes);
        return ref[0];
    }

    public HttpResponse get(URL url) throws IOException {
        return httpClientFactory.get().get(url).execute();
    }

    public HttpClient getDefaultHttpClient() {
        return new HttpClient()
                .setConnectTimeout(5_000)
                .setReadTimeout(5_000 * 2)
                .setCookieManager(null)
                .setFollowRedirects(true)
                .setRetryHandler(new RetryHandler(0, 50));
    }

    @Override
    public String httpGetText(String url) throws IOException {
        HttpResponse httpResponse = get(new URL(url));
        if (httpResponse.getStatusCode() > 299 || httpResponse.getStatusCode() < 200) {
            throw new IOException("Failed to download " + url + ", response " + httpResponse.getStatusCode() + ": " + httpResponse.getContentAsString());
        }

        return httpResponse.getContentAsString();
    }

    @Override
    public void downloadBigFile(String url, Path destination, String progressBarTitle, ProgressBarProvider progressBarProvider) throws IOException {
        HttpClient httpClient = httpClientFactory.get()
                .setExecutor(hc -> new LargeFileRequestExecutor(hc, progressBarProvider, progressBarTitle, destination));
        HttpResponse httpResponse = httpClient.get(new URL(url)).execute();
        if (httpResponse.getStatusCode() > 299 || httpResponse.getStatusCode() < 200) {
            throw new IOException("Failed to download " + url + ", response " + httpResponse.getStatusCode() + ": " + httpResponse.getContentAsString());
        }
    }

}
