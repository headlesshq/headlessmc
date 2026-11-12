package io.github.headlesshq.headlessmc.commands.plugin;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.net.MockDownloadService;
import io.github.headlesshq.headlessmc.net.hash.DefaultHashAlgorithmProvider;
import io.github.headlesshq.headlessmc.net.hash.HashAlgorithmProvider;
import io.github.headlesshq.headlessmc.net.hash.HashServiceImpl;
import io.github.headlesshq.headlessmc.plugins.PluginConfig;
import io.github.headlesshq.headlessmc.plugins.PluginIndexService;
import io.github.headlesshq.headlessmc.plugins.PluginManifest;
import io.github.headlesshq.headlessmc.plugins.PluginScanner;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import jakarta.enterprise.inject.Instance;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A plugin index served by a {@link MockDownloadService}, for testing {@link SearchCommand} and {@link DownloadCommand}.
 */
final class PluginIndexFixture {
    static final String HEADLESSMC_VERSION = "3.0.0";
    static final URI INDEX_URL = URI.create("https://example.com/index.json");

    final MockDownloadService downloadService = new MockDownloadService();
    final PluginScanner scanner;
    final PluginIndexService indexService;

    @SuppressWarnings("unchecked")
    PluginIndexFixture() {
        Instance<HashAlgorithmProvider> providers = Mockito.mock(Instance.class);
        Mockito.when(providers.stream()).thenAnswer(_ -> Stream.of(new DefaultHashAlgorithmProvider()));
        DefaultJacksonJsonService jsonService = new DefaultJacksonJsonService();
        scanner = new PluginScanner(new HashServiceImpl(providers), jsonService);
        PluginConfig config = Mockito.mock(PluginConfig.class);
        Mockito.when(config.indexUrl()).thenReturn(INDEX_URL.toString());
        Holder<PluginConfig> holder = () -> config;
        indexService = new PluginIndexService(holder, downloadService, jsonService, scanner, HEADLESSMC_VERSION);
    }

    PluginIndexFixture index(String json) {
        downloadService.register(INDEX_URL, json);
        return this;
    }

    /**
     * @return a {@code downloads} entry of the index for the given jar.
     */
    static String release(String version, String headlessmc, String url, byte[] jar) {
        return release(version, headlessmc, url, sha256(jar), jar.length);
    }

    static String release(String version, String headlessmc, String url, String sha256, long size) {
        return """
            {"version": "%s", "headlessmc": "%s", "java": 25, "url": "%s", "sha256": "%s", "size": %d, "unknown": true}
            """.formatted(version, headlessmc, url, sha256, size);
    }

    static byte[] pluginJar(String id, String version) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry(PluginManifest.PATH));
            zip.write("""
                {"schemaVersion": 1, "id": "%s", "version": "%s", "headlessmc": "*"}
                """.formatted(id, version).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return bytes.toByteArray();
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
