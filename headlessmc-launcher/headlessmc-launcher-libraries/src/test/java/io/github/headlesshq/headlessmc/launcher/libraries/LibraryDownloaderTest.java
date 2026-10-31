package io.github.headlesshq.headlessmc.launcher.libraries;

import io.github.headlesshq.headlessmc.config.ConfigService;
import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.net.MockDownloadService;
import io.github.headlesshq.headlessmc.net.hash.HashService;
import io.github.headlesshq.headlessmc.progressbar.ProgressbarService;
import io.github.headlesshq.headlessmc.util.maven.Artifact;
import io.github.headlesshq.headlessmc.util.maven.MavenRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class LibraryDownloaderTest {
    private static final byte[] CONTENT = "library content".getBytes(StandardCharsets.UTF_8);

    @Inject
    ConfigService configService;

    @Inject
    ProgressbarService progressbarService;

    @Inject
    HashService hashService;

    private final MockDownloadService downloadService = new MockDownloadService();

    private LibraryDownloader downloader() {
        return downloader(TestFiles.mcFilesService(McFiles.of(Path.of("unused"))));
    }

    private LibraryDownloader downloader(McFilesService mcFilesService) {
        ConfigService fork = configService.fork();
        fork.set("hmc.libraries.parallel.retry-max-attempts", "1", false);
        fork.set("hmc.libraries.parallel.retry-initial-interval-ms", "10", false);
        fork.set("hmc.libraries.parallel.retry-max-interval-ms", "20", false);
        Holder<LibraryConfig> config = fork.getHolder(LibraryConfig.class);
        return new LibraryDownloader(progressbarService, downloadService, mcFilesService, config, hashService);
    }

    private LibraryFile libraryFile(McFiles mcFiles, String coordinates, String url) {
        Artifact artifact = Artifact.of(coordinates);
        return new LibraryFile(
            new TestLibraries.FakeLibrary(coordinates),
            artifact.jar(mcFiles.getLibraryDir()),
            TestLibraries.download(null, (long) CONTENT.length, url, null)
        );
    }

    @Test
    public void downloadsMissingLibraries(@TempDir Path root) throws Exception {
        McFiles mcFiles = TestLibraries.mcFiles(root);
        URI first = URI.create("https://example.com/first.jar");
        URI second = URI.create("https://example.com/second.jar");
        downloadService.register(first, CONTENT).register(second, CONTENT);
        List<LibraryFile> files = List.of(
            libraryFile(mcFiles, "com.example:first:1.0", first.toString()),
            libraryFile(mcFiles, "com.example:second:1.0", second.toString())
        );

        downloader().download(List.of(), files);

        for (LibraryFile file : files) {
            assertArrayEquals(CONTENT, Files.readAllBytes(file.path()));
        }
    }

    @Test
    public void skipsExistingLibraries(@TempDir Path root) throws Exception {
        McFiles mcFiles = TestLibraries.mcFiles(root);
        LibraryFile file = libraryFile(mcFiles, "com.example:existing:1.0", "https://example.com/existing.jar");
        Files.createDirectories(file.path().getParent());
        Files.write(file.path(), CONTENT);

        downloader().download(List.of(), List.of(file));

        assertTrue(downloadService.getRequestedUris().isEmpty());
    }

    @Test
    public void retriesWithMavenRepositories(@TempDir Path root) throws Exception {
        McFiles mcFiles = TestLibraries.mcFiles(root);
        MavenRepository repository = MavenRepository.of("https://mirror.example.com/maven2");
        Artifact artifact = Artifact.of("com.example:retried:1.0");
        // the primary URL is not registered, so the download fails and falls back to the repository
        downloadService.register(repository.getJar(artifact), CONTENT);
        LibraryFile file = libraryFile(mcFiles, "com.example:retried:1.0", "https://example.com/broken.jar");

        downloader().download(List.of(repository), List.of(file));

        assertArrayEquals(CONTENT, Files.readAllBytes(file.path()));
        assertTrue(downloadService.wasRequested(URI.create("https://example.com/broken.jar")));
    }

    @Test
    public void copiesLibrariesFromOtherMcFolders(@TempDir Path root) throws Exception {
        McFiles mcFiles = TestLibraries.mcFiles(root);
        McFiles other = McFiles.of(root.resolve("other"));
        LibraryFile file = libraryFile(mcFiles, "com.example:copied:1.0", "https://example.com/copied.jar");
        Path copy = Artifact.of("com.example:copied:1.0").jar(other.getLibraryDir());
        Files.createDirectories(copy.getParent());
        Files.write(copy, CONTENT);

        downloader(TestFiles.mcFilesService(mcFiles, other)).download(List.of(), List.of(file));

        assertArrayEquals(CONTENT, Files.readAllBytes(file.path()));
        assertTrue(downloadService.getRequestedUris().isEmpty());
    }

    @Test
    public void downloadsLibrariesThatFailVerificationInOtherMcFolders(@TempDir Path root) throws Exception {
        McFiles mcFiles = TestLibraries.mcFiles(root);
        McFiles other = McFiles.of(root.resolve("other"));
        URI url = URI.create("https://example.com/corrupt.jar");
        downloadService.register(url, CONTENT);
        LibraryFile file = libraryFile(mcFiles, "com.example:corrupt:1.0", url.toString());
        Path copy = Artifact.of("com.example:corrupt:1.0").jar(other.getLibraryDir());
        Files.createDirectories(copy.getParent());
        Files.writeString(copy, "corrupt");

        downloader(TestFiles.mcFilesService(mcFiles, other)).download(List.of(), List.of(file));

        assertArrayEquals(CONTENT, Files.readAllBytes(file.path()));
        assertTrue(downloadService.wasRequested(url));
    }

}
