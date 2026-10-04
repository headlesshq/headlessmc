package io.github.headlesshq.headlessmc.launcher.logging;

import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.net.MockDownloadService;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.util.TemplateStrings;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LoggingDownloaderTest {
    private static final URI CONFIG_URL = URI.create("https://launcher.mojang.com/client-1.12.xml");
    private static final byte[] CONFIG = "<configuration/>".getBytes(StandardCharsets.UTF_8);

    private record TestDownload(
        @Nullable String getId,
        @Nullable String getSha1,
        @Nullable Long getSize,
        @Nullable String getUrl,
        @Nullable String getPath
    ) implements Version.Download {}

    private record TestLoggingConfiguration(
        String getArgument,
        Version.Download getFile,
        String getType
    ) implements Version.LoggingConfiguration {}

    @TempDir
    Path root;

    private final MockDownloadService downloads = new MockDownloadService();

    private McFiles mcFiles;
    private LoggingDownloader downloader;

    @BeforeEach
    void setup() {
        mcFiles = TestFiles.mcFiles(root);
        downloader = downloader(false);
        downloads.register(CONFIG_URL, CONFIG);
    }

    private LoggingDownloader downloader(boolean patch) {
        LoggingConfig config = () -> patch;
        return new LoggingDownloader(downloads, () -> config, new DefaultFileService(new DefaultFileSystemProvider()), mcFiles);
    }

    private Version version(@Nullable Map<String, Version.LoggingConfiguration> logging) {
        return new FakeVersion("1.12.2").withLogging(logging);
    }

    private Version.LoggingConfiguration configuration() {
        return new TestLoggingConfiguration(
            "-Dlog4j.configurationFile=${path}",
            new TestDownload("client-1.12.xml", null, (long) CONFIG.length, CONFIG_URL.toString(), null),
            "log4j2-xml"
        );
    }

    private Path configFile() {
        return mcFiles.getAssetsDir().resolve("log_configs").resolve("client-1.12.xml");
    }

    private Path patchedConfigFile() {
        return mcFiles.getAssetsDir().resolve("log_configs").resolve("client-1.12.xml-patched.xml");
    }

    @Test
    void versionsWithoutLoggingAreSkipped() {
        assertEquals(Optional.empty(), downloader.downloadLogging(new TemplateStrings(), version(null)));
        assertEquals(Optional.empty(), downloader.downloadLogging(new TemplateStrings(), version(Map.of())));
    }

    @Test
    void downloadsTheLoggingConfigurationAndBuildsTheArgument() throws Exception {
        Optional<String> argument = downloader.downloadLogging(
            new TemplateStrings(), version(Map.of(Version.LOGGING_CLIENT, configuration()))
        );

        assertEquals(
            Optional.of("-Dlog4j.configurationFile=" + configFile().toAbsolutePath()),
            argument
        );
        assertArrayEquals(CONFIG, Files.readAllBytes(configFile()));
    }

    @Test
    void anExistingConfigurationIsNotDownloadedAgain() throws Exception {
        Files.createDirectories(configFile().getParent());
        Files.write(configFile(), "cached".getBytes(StandardCharsets.UTF_8));

        downloader.downloadLogging(new TemplateStrings(), version(Map.of(Version.LOGGING_CLIENT, configuration())));

        assertFalse(downloads.wasRequested(CONFIG_URL));
        assertEquals("cached", Files.readString(configFile()));
    }

    @Test
    void configurationsWithoutAnIdAreRejected() {
        Version.LoggingConfiguration broken = new TestLoggingConfiguration(
            "-Dlog4j.configurationFile=${path}",
            new TestDownload(null, null, null, CONFIG_URL.toString(), null),
            "log4j2-xml"
        );

        assertThrows(NullPointerException.class, () -> downloader.downloadLogging(
            new TemplateStrings(), version(Map.of(Version.LOGGING_CLIENT, broken))
        ));
    }

    @Test
    void configurationsWithoutAUrlAreRejected() {
        Version.LoggingConfiguration broken = new TestLoggingConfiguration(
            "-Dlog4j.configurationFile=${path}",
            new TestDownload("client-1.12.xml", null, null, null, null),
            "log4j2-xml"
        );

        assertThrows(NullPointerException.class, () -> downloader.downloadLogging(
            new TemplateStrings(), version(Map.of(Version.LOGGING_CLIENT, broken))
        ));
    }

    @Test
    void patchingReplacesXmlLayoutsAndPointsTheArgumentAtThePatchedFile() throws Exception {
        Files.createDirectories(configFile().getParent());
        Files.writeString(configFile(), "<Console><XMLLayout /></Console><File><LegacyXMLLayout /></File>");

        Optional<String> argument = downloader(true).downloadLogging(
            new TemplateStrings(), version(Map.of(Version.LOGGING_CLIENT, configuration()))
        );

        assertEquals(
            Optional.of("-Dlog4j.configurationFile=" + patchedConfigFile().toAbsolutePath()),
            argument
        );
        String patched = Files.readString(patchedConfigFile());
        assertFalse(patched.contains("XMLLayout"));
        assertTrue(patched.contains("<PatternLayout pattern=\"[%d{HH:mm:ss}] [%t/%level]: %msg%n\" />"));
        assertTrue(patched.contains("<PatternLayout pattern=\"[%d{HH:mm:ss}] [%t/%level]: %msg{nolookups}%n\"/>"));
        // the original file stays untouched
        assertEquals(
            "<Console><XMLLayout /></Console><File><LegacyXMLLayout /></File>",
            Files.readString(configFile())
        );
    }

    @Test
    void anExistingPatchedConfigurationIsReused() throws Exception {
        Files.createDirectories(configFile().getParent());
        Files.writeString(configFile(), "<XMLLayout />");
        Files.writeString(patchedConfigFile(), "already-patched");

        Optional<String> argument = downloader(true).downloadLogging(
            new TemplateStrings(), version(Map.of(Version.LOGGING_CLIENT, configuration()))
        );

        assertEquals(
            Optional.of("-Dlog4j.configurationFile=" + patchedConfigFile().toAbsolutePath()),
            argument
        );
        assertEquals("already-patched", Files.readString(patchedConfigFile()));
    }

}
