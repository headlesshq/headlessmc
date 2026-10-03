package io.github.headlesshq.headlessmc.platform.vanilla;

import io.github.headlesshq.headlessmc.exceptions.FileException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.NotFoundException;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.files.cache.CacheBuilder;
import io.github.headlesshq.headlessmc.net.MockDownloadService;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.platform.server.ServerFinder;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.ProcessedVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionParser;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.github.headlesshq.headlessmc.version.service.VersionJsonService;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VanillaInstallerImplTest {
    private static final URI MANIFEST_1_21_1 = URI.create("https://launchermeta.mojang.com/1.21.1.json");
    private static final URI SERVER_JAR = URI.create("https://launcher.mojang.com/server.jar");
    private static final byte[] SERVER_CONTENT = "server-jar".getBytes(StandardCharsets.UTF_8);

    private record TestDownload(
        @Nullable String getId,
        @Nullable String getSha1,
        @Nullable Long getSize,
        @Nullable String getUrl,
        @Nullable String getPath
    ) implements Version.Download {}

    /**
     * Parses the {@code <id>[;java=<n>][;server=<url>]} format used by these tests
     * and only supports {@link #getParser()}, which is all the installer needs.
     */
    private static final class TestVersionJsonService implements VersionJsonService {
        private final VersionParser parser = reader -> {
            String content = read(reader);
            String[] parts = content.split(";");
            FakeVersion version = new FakeVersion(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i];
                if (part.startsWith("java=")) {
                    version.withJavaVersion(Integer.parseInt(part.substring("java=".length())));
                } else if (part.startsWith("server=")) {
                    version.withDownload(Version.DOWNLOAD_SERVER, new TestDownload(
                        null, sha1(SERVER_CONTENT), (long) SERVER_CONTENT.length,
                        part.substring("server=".length()), null
                    ));
                } else if ("no-server-url".equals(part)) {
                    version.withDownload(Version.DOWNLOAD_SERVER, new TestDownload(null, null, null, null, null));
                }
            }

            return version;
        };

        @Override
        public VersionParser getParser() {
            return parser;
        }

        @Override
        public Set<String> getInstalledFileNames() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Version getVersion(String fileName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Version> tryGetVersion(String fileName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProcessedVersion resolve(Version version) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProcessedVersion resolve(Version version, MissingVersionInstaller installer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Version> getInstalledVersions() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Version> observeChanges(ChangeAction action) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProcessedVersion process(Version version) {
            throw new UnsupportedOperationException();
        }

        private static String read(Reader reader) {
            try (BufferedReader buffered = new BufferedReader(reader)) {
                return String.join("", buffered.readAllLines()).trim();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

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

    @TempDir
    Path root;

    private final MockDownloadService downloads = new MockDownloadService();

    private McFiles mcFiles;
    private FakePlatformService platformService;
    private VanillaInstallerImpl installer;

    @BeforeEach
    void setup() {
        mcFiles = TestFiles.mcFiles(root);
        platformService = new FakePlatformService(new FakeVanillaPlatform("1.21.1", "1.20.4"));

        Cache<VanillaManifest> cache = CacheBuilder.<VanillaManifest>create()
            .withSource(() -> Optional.of(new VanillaManifest(List.of(
                new VanillaManifest.Version("1.21.1", "release", MANIFEST_1_21_1.toString())
            ))))
            .build();

        installer = new VanillaInstallerImpl(new TestVersionJsonService(), downloads, cache, mcFiles);
        downloads.register(MANIFEST_1_21_1, "1.21.1;java=21");
    }

    private VanillaVersion version(String name) {
        return platformService.getVanillaPlatform().getVersionService().getVersion(name).orElseThrow();
    }

    private VersionID id(String name) {
        return VersionID.resolve(platformService, VersionArg.parse("vanilla", name));
    }

    @Test
    void getVersionStreamsTheVersionJsonWithoutWritingIt() {
        Version version = installer.getVersion(version("1.21.1"));

        assertEquals("1.21.1", version.getId());
        assertEquals(21, version.requireJavaVersion());
        assertFalse(Files.exists(mcFiles.getMcDir().resolve("versions").resolve("1.21.1").resolve("1.21.1.json")));
    }

    @Test
    void installClientWritesTheVersionJson() throws IOException {
        Path mcDir = root.resolve("custom-mc");

        Version version = installer.installClient(id("1.21.1"), mcDir, new TypedMapImpl());

        Path json = mcDir.resolve("versions").resolve("1.21.1").resolve("1.21.1.json");
        assertEquals("1.21.1", version.getId());
        assertEquals("1.21.1;java=21", Files.readString(json));
    }

    @Test
    void existingVersionJsonIsReusedWithoutDownloading() throws IOException {
        Path mcDir = root.resolve("custom-mc");
        Path json = mcDir.resolve("versions").resolve("1.21.1").resolve("1.21.1.json");
        Files.createDirectories(json.getParent());
        Files.writeString(json, "1.21.1;java=17");

        Version version = installer.installClient(id("1.21.1"), mcDir, new TypedMapImpl());

        assertEquals(17, version.requireJavaVersion());
        assertFalse(downloads.wasRequested(MANIFEST_1_21_1));
    }

    @Test
    void forceInstallOverwritesAnExistingVersionJson() throws IOException {
        Path mcDir = root.resolve("custom-mc");
        Path json = mcDir.resolve("versions").resolve("1.21.1").resolve("1.21.1.json");
        Files.createDirectories(json.getParent());
        Files.writeString(json, "1.21.1;java=17");

        TypedMap args = new TypedMapImpl();
        args.put(ClientInstaller.FORCE_INSTALL, true);
        Version version = installer.installClient(id("1.21.1"), mcDir, args);

        assertEquals(21, version.requireJavaVersion());
        assertTrue(downloads.wasRequested(MANIFEST_1_21_1));
    }

    @Test
    void versionsMissingFromTheManifestThrow() {
        NotFoundException e = assertThrows(
            NotFoundException.class, () -> installer.getVersion(version("1.20.4"))
        );
        assertTrue(e.getMessage().contains("Failed to find version"));
    }

    @Test
    void installsTheServerJar() throws Exception {
        downloads.register(MANIFEST_1_21_1, "1.21.1;java=21;server=" + SERVER_JAR);
        downloads.register(SERVER_JAR, SERVER_CONTENT);

        Path dir = root.resolve("server");
        Files.createDirectories(dir);
        ServerInstaller.Installation installation = installer.installServer(id("1.21.1"), dir, new TypedMapImpl());

        assertEquals(21, installation.javaVersion());
        assertArrayEquals(SERVER_CONTENT, Files.readAllBytes(dir.resolve(ServerFinder.DEFAULT_JAR)));
    }

    @Test
    void serverInstallWithoutADownloadThrows() {
        FileException e = assertThrows(FileException.class,
            () -> installer.installServer(id("1.21.1"), root.resolve("server"), new TypedMapImpl()));
        assertTrue(e.getMessage().contains("Failed to find server download"));
    }

    @Test
    void serverInstallWithoutADownloadUrlThrows() {
        downloads.register(MANIFEST_1_21_1, "1.21.1;java=21;no-server-url");

        assertThrows(HeadlessMcException.class,
            () -> installer.installServer(id("1.21.1"), root.resolve("server"), new TypedMapImpl()));
    }

}
