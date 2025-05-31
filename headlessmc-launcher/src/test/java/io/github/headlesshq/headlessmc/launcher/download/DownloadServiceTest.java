package io.github.headlesshq.headlessmc.launcher.download;

import io.github.headlesshq.headlessmc.jline.JlineProgressbarProvider;
import io.github.headlesshq.headlessmc.launcher.LauncherMock;
import lombok.SneakyThrows;
import net.lenni0451.commons.httpclient.HttpResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class DownloadServiceTest {
    @Test
    public void testDownload() throws IOException {
        DownloadService downloadService = new DownloadService() {
            @Override
            @SneakyThrows
            public HttpResponse get(URI url) {
                return new HttpResponse(url.toURL(), 404, new byte[0], new HashMap<>());
            }
        };

        DownloadService finalDownloadService = downloadService;
        assertThrows(IOException.class, () -> finalDownloadService.download("http://example.com", LauncherMock.INSTANCE.getFileManager().getBase().toPath().resolve("test")));
        byte[] bytes = { 1, 2, 3, 4};
        downloadService = new DownloadService() {
            @Override
            @SneakyThrows
            public HttpResponse get(URI url) {
                return new HttpResponse(url.toURL(), 200, bytes, new HashMap<>());
            }
        };

        String sha1 = "12dada1fff4d4787ade3333147202c3b443e376f";
        assertEquals(sha1, downloadService.getChecksumService().hash(bytes));
        assertThrows(IOException.class, () -> finalDownloadService.download("http://example.com", LauncherMock.INSTANCE.getFileManager().getBase().toPath().resolve("test"), null, "wronghash"));
        assertSame(bytes, downloadService.download(URI.create("http://example.com"), null, sha1));
        assertSame(bytes, downloadService.download(URI.create("http://example.com"), null, null));
        assertSame(bytes, downloadService.download(URI.create("http://example.com"), 4L, null));
    }

    // TODO: spin up simple http server and serve some files to check all download stuff?
    @Test
    @Disabled("downloads stuff")
    public void testDownloadLargeFile() throws IOException {
        DownloadService downloadService = new DownloadService();
        String fileUrl = "https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u442-b06/OpenJDK8U-jdk_x64_mac_hotspot_8u442b06.tar.gz";
        downloadService.downloadBigFile(fileUrl, Paths.get("text.tar.gz"), "Test", new JlineProgressbarProvider());
    }

}
