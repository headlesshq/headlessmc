package io.github.headlesshq.headlessmc.launcher.libraries;

import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.net.MockDownloadService;
import io.github.headlesshq.headlessmc.version.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class McJarDownloaderTest {
    private static final byte[] JAR = "mc jar content".getBytes(StandardCharsets.UTF_8);
    private static final URI URL = URI.create("https://example.com/client.jar");

    private final MockDownloadService downloadService = new MockDownloadService();

    private static String sha1() throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(JAR));
    }

    private TestLibraries.FakeVersion version(Version.Download download) {
        TestLibraries.FakeVersion version = new TestLibraries.FakeVersion(List.of());
        version.downloads = Map.of(Version.DOWNLOAD_CLIENT, download);
        return version;
    }

    @Test
    public void downloadsMissingMcJar(@TempDir Path root) throws Exception {
        McFiles mcFiles = TestLibraries.mcFiles(root);
        downloadService.register(URL, JAR);
        McJarDownloader downloader = new McJarDownloader(downloadService, mcFiles);

        Path jar = downloader.downloadMcJar(
            Version.DOWNLOAD_CLIENT,
            version(TestLibraries.download(null, (long) JAR.length, URL.toString(), sha1()))
        );

        assertEquals(
            mcFiles.getVersionsDir().resolve("test-version").resolve("test-version.jar"),
            jar
        );
        assertArrayEquals(JAR, Files.readAllBytes(jar));
    }

    @Test
    public void skipsExistingJar(@TempDir Path root) throws Exception {
        McFiles mcFiles = TestLibraries.mcFiles(root);
        Path existing = mcFiles.getVersionsDir().resolve("test-version").resolve("test-version.jar");
        Files.createDirectories(existing.getParent());
        Files.write(existing, JAR);
        McJarDownloader downloader = new McJarDownloader(downloadService, mcFiles);

        Path jar = downloader.downloadMcJar(Version.DOWNLOAD_CLIENT, version(TestLibraries.download(null, null, null, null)));

        assertEquals(existing, jar);
        assertTrue(downloadService.getRequestedUris().isEmpty());
    }

    @Test
    public void throwsWithoutDownloadEntry(@TempDir Path root) {
        McJarDownloader downloader = new McJarDownloader(downloadService, TestLibraries.mcFiles(root));
        TestLibraries.FakeVersion version = new TestLibraries.FakeVersion(List.of());

        assertThrows(LibraryException.class, () -> downloader.downloadMcJar(Version.DOWNLOAD_CLIENT, version));
    }

    @Test
    public void throwsWithoutDownloadUrl(@TempDir Path root) {
        McJarDownloader downloader = new McJarDownloader(downloadService, TestLibraries.mcFiles(root));

        assertThrows(NullPointerException.class, () -> downloader.downloadMcJar(
            Version.DOWNLOAD_CLIENT,
            version(TestLibraries.download(null, null, null, null))
        ));
    }

}
