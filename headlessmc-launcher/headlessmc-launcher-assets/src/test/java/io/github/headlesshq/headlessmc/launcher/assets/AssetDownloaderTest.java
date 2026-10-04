package io.github.headlesshq.headlessmc.launcher.assets;

import io.github.headlesshq.headlessmc.config.ConfigService;
import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.net.MockDownloadService;
import io.github.headlesshq.headlessmc.net.hash.HashService;
import io.github.headlesshq.headlessmc.progressbar.ProgressbarService;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.version.Version;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class AssetDownloaderTest {
    private static final String INDEX_URL = "https://launchermeta.mojang.com/1.21.1.json";
    private static final String ASSET_URL_BASE = "https://resources.download.minecraft.net";
    private static final byte[] ICON = "icon-bytes".getBytes(StandardCharsets.UTF_8);

    @Inject
    ConfigService configService;

    @Inject
    ProgressbarService progressbarService;

    @Inject
    HashService hashService;

    @Inject
    JsonService jsonService;

    @Inject
    FileService fileService;

    private final MockDownloadService downloadService = new MockDownloadService();

    private static String sha1(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(content);
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }

            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private McFiles mcFiles(Path root) {
        return new McFiles() {
            @Override
            public Path getMcDir() {
                return root.resolve("mc");
            }

            @Override
            public Path getVersionsDir() {
                return getMcDir().resolve("versions");
            }

            @Override
            public Path getLibraryDir() {
                return getMcDir().resolve("libraries");
            }

            @Override
            public Path getAssetsDir() {
                return getMcDir().resolve("assets");
            }

            @Override
            public Path getResourcesDir() {
                return getMcDir().resolve("resources");
            }
        };
    }

    private AssetDownloader downloader(McFiles mcFiles, boolean dummy) {
        ConfigService fork = configService.fork();
        fork.set("hmc.assets.dummy", String.valueOf(dummy), false);
        fork.set("hmc.assets.url", ASSET_URL_BASE, false);
        fork.set("hmc.assets.parallel.retry-max-attempts", "1", false);
        fork.set("hmc.assets.parallel.retry-initial-interval-ms", "10", false);
        fork.set("hmc.assets.parallel.retry-max-interval-ms", "20", false);
        Holder<AssetsConfig> config = fork.getHolder(AssetsConfig.class);
        return new AssetDownloader(
            progressbarService, downloadService, fileService, hashService,
            new DummyAssets(), config, mcFiles, jsonService
        );
    }

    private Version.AssetIndex assetIndex(String id, @Nullable Long totalSize) {
        return new Version.AssetIndex() {
            @Override
            public @Nullable Long getTotalSize() {
                return totalSize;
            }

            @Override
            public String getId() {
                return id;
            }

            @Override
            public @Nullable String getSha1() {
                return null;
            }

            @Override
            public @Nullable Long getSize() {
                return null;
            }

            @Override
            public String getUrl() {
                return INDEX_URL;
            }

            @Override
            public @Nullable String getPath() {
                return null;
            }
        };
    }

    private String indexJson(String extra) {
        return "{" + extra + "\"objects\":{\"icons/icon.png\":{\"hash\":\"" + sha1(ICON)
            + "\",\"size\":" + ICON.length + "}}}";
    }

    private void registerAsset() {
        String hash = sha1(ICON);
        downloadService.register(URI.create(ASSET_URL_BASE + "/" + hash.substring(0, 2) + "/" + hash), ICON);
    }

    private Path objectFile(McFiles mcFiles) {
        String hash = sha1(ICON);
        return mcFiles.getAssetsDir().resolve("objects").resolve(hash.substring(0, 2)).resolve(hash);
    }

    @Test
    public void downloadsIndexAndAssets(@TempDir Path root) throws Exception {
        McFiles mcFiles = mcFiles(root);
        downloadService.register(URI.create(INDEX_URL), indexJson(""));
        registerAsset();

        AssetsLocation location = downloader(mcFiles, false).download(assetIndex("1.21.1", (long) ICON.length));

        assertEquals("1.21.1", location.id());
        assertEquals(mcFiles.getAssetsDir(), location.location());
        assertArrayEquals(ICON, Files.readAllBytes(objectFile(mcFiles)));
        assertTrue(Files.exists(mcFiles.getAssetsDir().resolve("indexes").resolve("1.21.1.json")));
    }

    @Test
    public void mismatchedTotalSizeIsToleratedAndLogged(@TempDir Path root) throws Exception {
        McFiles mcFiles = mcFiles(root);
        downloadService.register(URI.create(INDEX_URL), indexJson(""));
        registerAsset();

        assertNotNull(downloader(mcFiles, false).download(assetIndex("1.21.1", 999L)));
    }

    @Test
    public void alreadyDownloadedAssetsAreNotRequestedAgain(@TempDir Path root) throws Exception {
        McFiles mcFiles = mcFiles(root);
        downloadService.register(URI.create(INDEX_URL), indexJson(""));
        registerAsset();

        Path file = objectFile(mcFiles);
        Files.createDirectories(file.getParent());
        Files.write(file, ICON);

        String hash = sha1(ICON);
        URI assetUri = URI.create(ASSET_URL_BASE + "/" + hash.substring(0, 2) + "/" + hash);
        downloader(mcFiles, false).download(assetIndex("1.21.1", (long) ICON.length));

        assertFalse(downloadService.wasRequested(assetUri));
    }

    @Test
    public void corruptedAssetsAreDownloadedAgain(@TempDir Path root) throws Exception {
        McFiles mcFiles = mcFiles(root);
        downloadService.register(URI.create(INDEX_URL), indexJson(""));
        registerAsset();

        Path file = objectFile(mcFiles);
        Files.createDirectories(file.getParent());
        Files.write(file, "corrupted".getBytes(StandardCharsets.UTF_8));

        downloader(mcFiles, false).download(assetIndex("1.21.1", (long) ICON.length));

        assertArrayEquals(ICON, Files.readAllBytes(file));
    }

    @Test
    public void virtualIndexCopiesAssetsToTheVirtualDir(@TempDir Path root) throws Exception {
        McFiles mcFiles = mcFiles(root);
        downloadService.register(URI.create(INDEX_URL), indexJson("\"virtual\":true,"));
        registerAsset();
        Files.createDirectories(mcFiles.getAssetsDir().resolve("virtual").resolve("legacy").resolve("icons"));

        AssetsLocation location = downloader(mcFiles, false).download(assetIndex("legacy", (long) ICON.length));

        Path virtual = mcFiles.getAssetsDir().resolve("virtual").resolve("legacy");
        assertEquals(virtual, location.location());
        assertArrayEquals(ICON, Files.readAllBytes(virtual.resolve("icons").resolve("icon.png")));
    }

    @Test
    public void mapToResourcesCopiesAssetsToTheResourcesDir(@TempDir Path root) throws Exception {
        McFiles mcFiles = mcFiles(root);
        downloadService.register(URI.create(INDEX_URL), indexJson("\"map_to_resources\":true,"));
        registerAsset();
        Files.createDirectories(mcFiles.getResourcesDir().resolve("icons"));

        AssetsLocation location = downloader(mcFiles, false).download(assetIndex("pre-1.6", (long) ICON.length));

        assertEquals(mcFiles.getResourcesDir(), location.location());
        assertArrayEquals(ICON, Files.readAllBytes(mcFiles.getResourcesDir().resolve("icons").resolve("icon.png")));
    }

    @Test
    public void dummyModeUsesTheBundledPlaceholders(@TempDir Path root) throws Exception {
        McFiles mcFiles = mcFiles(root);
        downloadService.register(URI.create(INDEX_URL), indexJson(""));

        downloader(mcFiles, true).download(assetIndex("1.21.1", (long) ICON.length));

        // the placeholder png is used instead of the real asset, so it is not the real content
        assertTrue(Files.exists(objectFile(mcFiles)));
        assertFalse(java.util.Arrays.equals(ICON, Files.readAllBytes(objectFile(mcFiles))));
    }

    @Test
    public void assetIndexWithoutIdOrUrlIsRejected(@TempDir Path root) {
        AssetDownloader downloader = downloader(mcFiles(root), false);
        Version.AssetIndex noId = new Version.AssetIndex() {
            @Override
            public @Nullable Long getTotalSize() {
                return null;
            }

            @Override
            public @Nullable String getId() {
                return null;
            }

            @Override
            public @Nullable String getSha1() {
                return null;
            }

            @Override
            public @Nullable Long getSize() {
                return null;
            }

            @Override
            public @Nullable String getUrl() {
                return INDEX_URL;
            }

            @Override
            public @Nullable String getPath() {
                return null;
            }
        };

        assertThrows(NullPointerException.class, () -> downloader.download(noId));
    }

    @Test
    public void dummyAssetsResolveByExtension() {
        DummyAssets dummyAssets = new DummyAssets();

        assertNotNull(dummyAssets.getResource("icons/icon.PNG"));
        assertNotNull(dummyAssets.getResource("sounds/click.ogg"));
        assertNull(dummyAssets.getResource("lang/en_us.json"));
    }

}
