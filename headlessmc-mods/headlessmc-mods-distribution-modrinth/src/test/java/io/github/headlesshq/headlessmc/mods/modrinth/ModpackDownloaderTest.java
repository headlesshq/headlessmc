package io.github.headlesshq.headlessmc.mods.modrinth;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.net.MockDownloadService;
import io.github.headlesshq.headlessmc.progressbar.ProgressBar;
import io.github.headlesshq.headlessmc.progressbar.ProgressBarServiceManager;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import io.github.headlesshq.headlessmc.version.arg.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ModpackDownloaderTest {
    private static final URI PACK_URL = URI.create("https://cdn.modrinth.com/pack.mrpack");
    private static final URI MOD_URL = URI.create("https://cdn.modrinth.com/sodium.jar");
    private static final URI BROKEN_URL = URI.create("https://cdn.modrinth.com/broken.jar");
    private static final byte[] MOD = "sodium-jar".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path root;

    private final ProgressBarServiceManager progressBars = configuration -> ProgressBar.dummy();
    private final MockDownloadService downloads = new MockDownloadService();

    private ModpackDownloader downloader;

    @BeforeEach
    void setup() {
        downloader = new ModpackDownloader(progressBars, new DefaultJacksonJsonService(), new DefaultFileService(new DefaultFileSystemProvider()));
        downloads.register(MOD_URL, MOD);
    }

    private static byte[] zip(Map<String, String> entries) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return bytes.toByteArray();
    }

    private String index(String files) {
        return "{\"formatVersion\":1,\"versionId\":\"1\",\"name\":\"pack\",\"files\":[" + files
            + "],\"dependencies\":{\"minecraft\":\"1.21.1\"}}";
    }

    private String file(String path, String url, String env) {
        return "{\"path\":\"" + path + "\",\"hashes\":{},\"downloads\":[\"" + url + "\"],\"fileSize\":" + MOD.length
            + (env == null ? "" : ",\"env\":" + env) + "}";
    }

    private void registerPack(Map<String, String> entries) {
        downloads.register(PACK_URL, zip(entries));
    }

    private void download(Side side, Path gameDir) {
        downloader.download("pack", side, gameDir, downloads.context(), downloads.download(PACK_URL));
    }

    @Test
    void downloadsFilesOfAModpack() throws IOException {
        registerPack(Map.of("modrinth.index.json", index(file("mods/sodium.jar", MOD_URL.toString(), null))));
        Path gameDir = root.resolve("game");

        download(Side.CLIENT, gameDir);

        assertArrayEquals(MOD, Files.readAllBytes(gameDir.resolve("mods").resolve("sodium.jar")));
    }

    @Test
    void copiesOverrides() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("modrinth.index.json", index(""));
        entries.put("overrides/config/options.txt", "options");
        entries.put("client-overrides/config/client.txt", "client");
        entries.put("server-overrides/config/server.txt", "server");
        registerPack(entries);
        Path gameDir = root.resolve("game");

        download(Side.CLIENT, gameDir);

        assertEquals("options", Files.readString(gameDir.resolve("config").resolve("options.txt")));
        assertEquals("client", Files.readString(gameDir.resolve("config").resolve("client.txt")));
        assertFalse(Files.exists(gameDir.resolve("config").resolve("server.txt")));
    }

    @Test
    void copiesServerOverridesForServers() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("modrinth.index.json", index(""));
        entries.put("server-overrides/config/server.txt", "server");
        registerPack(entries);
        Path gameDir = root.resolve("game");

        download(Side.SERVER, gameDir);

        assertEquals("server", Files.readString(gameDir.resolve("config").resolve("server.txt")));
    }

    @Test
    void filesUnsupportedOnASideAreSkipped() {
        String clientOnly = file("mods/sodium.jar", MOD_URL.toString(), "{\"client\":\"required\",\"server\":\"unsupported\"}");
        registerPack(Map.of("modrinth.index.json", index(clientOnly)));
        Path gameDir = root.resolve("game");

        download(Side.SERVER, gameDir);

        assertFalse(Files.exists(gameDir.resolve("mods").resolve("sodium.jar")));
    }

    @Test
    void optionalFilesAreDownloaded() throws IOException {
        String optional = file("mods/sodium.jar", MOD_URL.toString(), "{\"client\":\"optional\"}");
        registerPack(Map.of("modrinth.index.json", index(optional)));
        Path gameDir = root.resolve("game");

        download(Side.CLIENT, gameDir);

        assertTrue(Files.exists(gameDir.resolve("mods").resolve("sodium.jar")));
    }

    @Test
    void packsWithoutAnIndexAreRejected() {
        registerPack(Map.of("readme.txt", "no index here"));

        assertThrows(HeadlessMcIOException.class, () -> download(Side.CLIENT, root.resolve("game")));
    }

    @Test
    void filesWithoutDownloadsAreRejected() {
        String noDownloads = "{\"path\":\"mods/sodium.jar\",\"hashes\":{},\"downloads\":[],\"fileSize\":1}";
        registerPack(Map.of("modrinth.index.json", index(noDownloads)));

        assertThrows(HeadlessMcIOException.class, () -> download(Side.CLIENT, root.resolve("game")));
    }

    @Test
    void failingDownloadsFallBackToTheNextMirror() throws IOException {
        String mirrored = "{\"path\":\"mods/sodium.jar\",\"hashes\":{},\"downloads\":[\"" + BROKEN_URL + "\",\""
            + MOD_URL + "\"],\"fileSize\":" + MOD.length + "}";
        registerPack(Map.of("modrinth.index.json", index(mirrored)));
        Path gameDir = root.resolve("game");

        download(Side.CLIENT, gameDir);

        assertArrayEquals(MOD, Files.readAllBytes(gameDir.resolve("mods").resolve("sodium.jar")));
    }

    @Test
    void failingDownloadsWithoutAWorkingMirrorThrow() {
        String broken = "{\"path\":\"mods/sodium.jar\",\"hashes\":{},\"downloads\":[\"" + BROKEN_URL
            + "\"],\"fileSize\":" + MOD.length + "}";
        registerPack(Map.of("modrinth.index.json", index(broken)));

        HeadlessMcException e = assertThrows(
            HeadlessMcException.class, () -> download(Side.CLIENT, root.resolve("game"))
        );
        assertTrue(e.getMessage().contains("Failed to download file"));
    }

    @Test
    void zipSlipIsDetected() {
        registerPack(Map.of("../escaped.txt", "evil"));

        assertThrows(HeadlessMcIOException.class, () -> download(Side.CLIENT, root.resolve("game")));
    }

}
