package io.github.headlesshq.headlessmc.net;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcNoSuchAlgorithmException;
import io.github.headlesshq.headlessmc.exceptions.RequestException;
import io.github.headlesshq.headlessmc.exceptions.VerificationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class MockDownloadServiceTest {
    private static final URI URI_A = URI.create("https://example.com/a.txt");
    private static final byte[] CONTENT = "mock content".getBytes(StandardCharsets.UTF_8);

    private final MockDownloadService service = new MockDownloadService().register(URI_A, CONTENT);

    private static String hex(String algorithm) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(CONTENT));
    }

    @Test
    public void downloadsRegisteredContentToFile(@TempDir Path dir) throws Exception {
        Path file = service.download(URI_A)
            .sha1(hex("SHA-1"))
            .sha256(hex("SHA-256"))
            .size((long) CONTENT.length)
            .retry(3, 10, 2.0, 0.5)
            .toFile(dir.resolve("nested").resolve("a.txt"));

        assertArrayEquals(CONTENT, Files.readAllBytes(file));
        assertTrue(service.wasRequested(URI_A));
    }

    @Test
    public void mapsDownloadContent() {
        String content = service.download(URI_A)
            .map(download -> download.getAsString(StandardCharsets.UTF_8));

        assertEquals("mock content", content);
    }

    @Test
    public void startsDownload() {
        AtomicInteger statusCode = new AtomicInteger();
        service.download(URI_A).start(download -> statusCode.set(download.getStatusCode()));
        assertEquals(200, statusCode.get());
    }

    @Test
    public void verifiesHashes(@TempDir Path dir) {
        DownloadBuilder builder = service.download(URI_A).sha1("0000");
        assertThrows(VerificationException.class, () -> builder.toFile(dir.resolve("a.txt")));
    }

    @Test
    public void verifiesLenientHashNames(@TempDir Path dir) throws Exception {
        service.download(URI_A)
            .hashes(Map.of("sha256", hex("SHA-256")))
            .toFile(dir.resolve("a.txt"));

        DownloadBuilder wrong = service.download(URI_A).hash("sha_512", "0000");
        assertThrows(VerificationException.class, () -> wrong.toFile(dir.resolve("b.txt")));
    }

    @Test
    public void verifiesSize(@TempDir Path dir) {
        DownloadBuilder builder = service.download(URI_A).size(1L);
        assertThrows(VerificationException.class, () -> builder.toFile(dir.resolve("a.txt")));
    }

    @Test
    public void throwsForUnknownHashAlgorithms(@TempDir Path dir) {
        DownloadBuilder builder = service.download(URI_A).hash("no-such-algorithm", "abc");
        assertThrows(HeadlessMcNoSuchAlgorithmException.class, () -> builder.toFile(dir.resolve("a.txt")));
    }

    @Test
    public void ignoresUnknownAlgorithmIfOthersMatch(@TempDir Path dir) throws Exception {
        service.download(URI_A)
            .hash("no-such-algorithm", "abc")
            .hash("SHA-256", hex("SHA-256"))
            .toFile(dir.resolve("a.txt"));
    }

    @Test
    public void handlesErrorStatusCodes() {
        URI missing = URI.create("https://example.com/missing.txt");
        service.register(missing, 404, "not found".getBytes(StandardCharsets.UTF_8));

        assertThrows(RequestException.class, () -> service.download(missing).map(Download::getStatusCode));

        int handled = service.download(missing)
            .statusCodeHandler((code, response) -> {
            })
            .map(Download::getStatusCode);
        assertEquals(404, handled);
    }

    @Test
    public void throwsForUnregisteredUris() {
        URI unknown = URI.create("https://example.com/unknown.txt");
        assertThrows(RequestException.class, () -> service.download(unknown).map(Download::getStatusCode));
        assertTrue(service.wasRequested(unknown));
    }

    @Test
    public void contextServesDownloads(@TempDir Path dir) throws Exception {
        try (DownloadContext context = service.context()) {
            Path file = context.download(URI_A).toFile(dir.resolve("a.txt"));
            assertArrayEquals(CONTENT, Files.readAllBytes(file));
        }
    }

}
