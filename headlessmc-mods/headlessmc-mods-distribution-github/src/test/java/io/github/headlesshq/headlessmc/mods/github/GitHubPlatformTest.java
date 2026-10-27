package io.github.headlesshq.headlessmc.mods.github;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.cache.CacheBuilder;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.mods.distribution.ModCache;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionException;
import io.github.headlesshq.headlessmc.mods.distribution.RemoteMod;
import io.github.headlesshq.headlessmc.net.MockDownloadService;
import io.github.headlesshq.headlessmc.net.rest.ApiException;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.core.Response;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GitHubPlatformTest {
    private static final byte[] JAR = "jar".getBytes(StandardCharsets.UTF_8);
    private static final String MOD_ID_PREFIX = "mod:";

    @TempDir
    Path root;

    private final Map<String, GitHubRelease> latestReleases = new HashMap<>();
    private final Map<String, GitHubRelease> taggedReleases = new HashMap<>();
    private final MockDownloadService downloads = new MockDownloadService();

    private final GitHubAPI api = new GitHubAPI() {
        @Override
        public GitHubRelease getLatestRelease(String owner, String repo, @Nullable String authorization) {
            return orNotFound(latestReleases.get(owner + "/" + repo));
        }

        @Override
        public GitHubRelease getReleaseByTag(String owner, String repo, String tag, @Nullable String authorization) {
            return orNotFound(taggedReleases.get(owner + "/" + repo + "@" + tag));
        }
    };

    private final GitHubConfig config = new GitHubConfig() {
        @Override
        public Optional<String> token() {
            return Optional.empty();
        }

        @Override
        public int cacheMinutes() {
            return 0;
        }
    };

    /**
     * Test jars contain {@code mod:<mod-id>} instead of real mod metadata.
     */
    private final ModIdReader modIdReader = (jar, version) -> {
        try {
            String content = Files.readString(jar);
            return content.startsWith(MOD_ID_PREFIX) ? Set.of(content.substring(MOD_ID_PREFIX.length())) : Set.of();
        } catch (Exception e) {
            return Set.of();
        }
    };

    private static GitHubRelease orNotFound(@Nullable GitHubRelease release) {
        if (release == null) {
            throw new ApiException(Response.status(Response.Status.NOT_FOUND).build());
        }

        return release;
    }

    private final ModCache modCache = new ModCache() {
        @Override
        public Optional<RemoteMod> find(Path file) throws HeadlessMcIOException {
            return Optional.empty();
        }

        @Override
        public void add(Path file, RemoteMod mod) {

        }
    };

    private GitHubDistributionPlatform platform;

    @BeforeEach
    void setup() {
        List<ModRepository> repositories = List.of(
            new HmcSpecificsRepository(),
            new HmcOptimizationsRepository(),
            new McRuntimeTestRepository(),
            new HmcMcpRepository(),
            new HmcPrometheusRepository()
        );

        GitHubReleaseService releaseService = new GitHubReleaseService(
            api, config, new GitHubReleaseCache(CacheBuilder.<GitHubReleaseCache.Releases>create().build())
        );
        platform = new GitHubDistributionPlatform(
            instance(repositories),
            downloads,
            releaseService,
            modCache,
            new DefaultFileService(new DefaultFileSystemProvider()),
            modIdReader
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> Instance<T> instance(List<T> values) {
        return (Instance<T>) Proxy.newProxyInstance(
            GitHubPlatformTest.class.getClassLoader(),
            new Class<?>[]{Instance.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "stream" -> values.stream();
                case "iterator" -> values.iterator();
                default -> throw new UnsupportedOperationException(method.getName());
            }
        );
    }

    private GitHubRelease.Asset asset(String repo, String tag, String name) {
        URI uri = URI.create("https://github.com/headlesshq/" + repo + "/releases/download/" + tag + "/" + name);
        downloads.register(uri, JAR);
        return new GitHubRelease.Asset(name, uri.toString(), (long) JAR.length, "sha256:" + sha256(JAR));
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Path installed(String name, String content) throws Exception {
        Path mods = Files.createDirectories(root.resolve("mods"));
        return Files.writeString(mods.resolve(name), content);
    }

    private void release(String repo, String tag, String... assetNames) {
        latestReleases.put("headlesshq/" + repo, new GitHubRelease(tag, assets(repo, tag, assetNames)));
    }

    private void taggedRelease(String repo, String tag, String... assetNames) {
        taggedReleases.put("headlesshq/" + repo + "@" + tag, new GitHubRelease(tag, assets(repo, tag, assetNames)));
    }

    private List<GitHubRelease.Asset> assets(String repo, String tag, String... assetNames) {
        return Arrays.stream(assetNames).map(name -> asset(repo, tag, name)).toList();
    }

    @Test
    void nameIsGitHub() {
        assertEquals("github", platform.getName());
    }

    @Test
    void downloadsTheLatestReleaseForTheVersionAndLoader() throws Exception {
        taggedRelease("hmc-specifics", "1.21.1-latest",
            "hmc-specifics-1.21.1-fabric-latest.jar",
            "hmc-specifics-1.21.1-forge-latest.jar");

        Path file = platform.download(root, null, "hmc-specifics", VersionArg.parse("forge", "1.21.1"), ModType.MOD);

        assertEquals(root.resolve("mods").resolve("hmc-specifics-1.21.1-forge-latest.jar"), file);
        assertArrayEquals(JAR, Files.readAllBytes(file));
    }

    @Test
    void downloadsASpecificVersion() throws Exception {
        taggedRelease("hmc-optimizations", "1.21.1-latest", "hmc-optimizations-1.21.1-fabric-latest.jar");
        taggedRelease("hmc-optimizations", "1.21.1-0.9.0", "hmc-optimizations-1.21.1-fabric-0.9.0.jar");

        Path file = platform.download(root, null, "hmc-optimizations@0.9.0", VersionArg.parse("fabric", "1.21.1"), ModType.MOD);

        assertEquals("hmc-optimizations-1.21.1-fabric-0.9.0.jar", file.getFileName().toString());
    }

    @Test
    void mcRuntimeTestIsDownloadedFromTheModRepository() throws Exception {
        taggedRelease("mc-runtime-test-mod", "1.20.4-latest", "mc-runtime-test-1.20.4-neoforge-latest.jar");

        Path file = platform.download(root, null, "mc-runtime-test", VersionArg.parse("neoforge", "1.20.4"), ModType.MOD);

        assertEquals("mc-runtime-test-1.20.4-neoforge-latest.jar", file.getFileName().toString());
    }

    @Test
    void addonsAreDownloadedAlongsideHmcSpecifics() throws Exception {
        taggedRelease("hmc-specifics", "1.21.1-latest", "hmc-specifics-1.21.1-neoforge-latest.jar");
        release("headlessmc-mcp", "0.1.0", "headlessmc-mcp.jar", "hmc-mcp-bridge-0.1.0.jar");
        release("hmc-prometheus", "0.1.0", "hmc-prometheus-0.1.0.jar");

        VersionArg version = VersionArg.parse("neoforge", "1.21.1");
        Path mcp = platform.download(root, null, "hmc-mcp", version, ModType.MOD);
        Path prometheus = platform.download(root, null, "hmc-prometheus", version, ModType.MOD);

        Path mods = root.resolve("mods");
        assertEquals(mods.resolve("hmc-mcp-bridge-0.1.0.jar"), mcp);
        assertEquals(mods.resolve("hmc-prometheus-0.1.0.jar"), prometheus);
        assertTrue(Files.exists(mods.resolve("hmc-specifics-1.21.1-neoforge-latest.jar")));
    }

    @Test
    void addonsCanBeDownloadedForASpecificTag() throws Exception {
        taggedRelease("hmc-specifics", "1.21.1-latest", "hmc-specifics-1.21.1-fabric-latest.jar");
        taggedRelease("hmc-prometheus", "0.0.9", "hmc-prometheus-0.0.9.jar");

        Path file = platform.download(root, null, "hmc-prometheus@0.0.9", VersionArg.parse("fabric", "1.21.1"), ModType.MOD);

        assertEquals("hmc-prometheus-0.0.9.jar", file.getFileName().toString());
    }

    @Test
    void addonsAreNotDownloadedIfHmcSpecificsDoesNotSupportTheVersion() {
        taggedRelease("hmc-specifics", "1.20.4-latest", "hmc-specifics-1.20.4-forge-latest.jar");
        release("hmc-prometheus", "0.1.0", "hmc-prometheus-0.1.0.jar");

        ModDistributionException e = assertThrows(ModDistributionException.class,
            () -> platform.download(root, null, "hmc-prometheus", VersionArg.parse("fabric", "1.20.4"), ModType.MOD));

        assertTrue(e.getMessage().contains("requires hmc-specifics"));
        assertFalse(Files.exists(root.resolve("mods")));
    }

    @Test
    void addonsAreNotDownloadedIfHmcSpecificsHasNoReleaseForTheVersion() {
        release("headlessmc-mcp", "0.1.0", "hmc-mcp-bridge-0.1.0.jar");

        ModDistributionException e = assertThrows(ModDistributionException.class,
            () -> platform.download(root, null, "hmc-mcp", VersionArg.parse("fabric", "1.20.4"), ModType.MOD));

        assertTrue(e.getMessage().contains("requires hmc-specifics"));
    }

    @Test
    void missingReleasesThrow() {
        ModDistributionException e = assertThrows(ModDistributionException.class,
            () -> platform.download(root, null, "hmc-specifics@0.0.1", VersionArg.parse("fabric", "1.21.1"), ModType.MOD));

        assertTrue(e.getMessage().contains("1.21.1-0.0.1"));
    }

    @Test
    void missingFilesThrow() {
        taggedRelease("mc-runtime-test-mod", "1.21.1-latest");

        ModDistributionException e = assertThrows(ModDistributionException.class,
            () -> platform.download(root, null, "mc-runtime-test", VersionArg.parse("fabric", "1.21.1"), ModType.MOD));

        assertTrue(e.getMessage().contains("contains no file"));
    }

    @Test
    void unsupportedPlatformsAndTypesThrow() {
        assertThrows(ModDistributionException.class,
            () -> platform.download(root, null, "hmc-specifics", VersionArg.parse("vanilla", "1.21.1"), ModType.MOD));
        assertThrows(ModDistributionException.class,
            () -> platform.download(root, null, "hmc-specifics", VersionArg.parse("fabric", "1.21.1"), ModType.RESOURCE_PACK));
        assertThrows(ModDistributionException.class,
            () -> platform.download(root, null, "unknown", VersionArg.parse("fabric", "1.21.1"), ModType.MOD));
    }

    @Test
    void searchesByNameAndDescription() {
        assertEquals(List.of("hmc-specifics", "hmc-optimizations", "hmc-mcp", "hmc-prometheus"),
            platform.search("hmc", ModType.MOD).stream().map(RemoteMod::id).toList());
        assertEquals(List.of("hmc-prometheus"),
            platform.search("PROMETHEUS", ModType.MOD).stream().map(RemoteMod::id).toList());
        assertEquals(List.of(), platform.search("hmc", ModType.SHADER));
        assertEquals(List.of(), platform.search("hmc", VersionArg.parse("vanilla", "1.21.1"), ModType.MOD));
        assertEquals(5, platform.search("", VersionArg.parse("fabric", "1.21.1"), ModType.MOD).size());
    }

    @Test
    void installedVersionsWithTheSameModIdAreReplaced() throws Exception {
        taggedRelease("hmc-specifics", "1.21.1-latest", "hmc-specifics-1.21.1-fabric-latest.jar");
        Path old = installed("hmc-specifics-1.20.4-fabric-3.0.0.jar", MOD_ID_PREFIX + "headlessmc");
        Path renamed = installed("specifics.jar", MOD_ID_PREFIX + "headlessmc");
        Path other = installed("other.jar", MOD_ID_PREFIX + "other");

        Path file = platform.download(root, null, "hmc-specifics", VersionArg.parse("fabric", "1.21.1"), ModType.MOD);

        assertEquals(root.resolve("mods").resolve("hmc-specifics-1.21.1-fabric-latest.jar"), file);
        assertArrayEquals(JAR, Files.readAllBytes(file));
        assertFalse(Files.exists(old));
        assertFalse(Files.exists(renamed));
        assertTrue(Files.exists(other));
    }

    @Test
    void installedFilesWithTheSameHashAreNotDownloadedAgain() throws Exception {
        taggedRelease("hmc-specifics", "1.21.1-latest", "hmc-specifics-1.21.1-fabric-latest.jar");
        Path installed = installed("hmc-specifics-1.21.1-fabric-latest.jar", "jar");

        Path file = platform.download(root, null, "hmc-specifics", VersionArg.parse("fabric", "1.21.1"), ModType.MOD);

        assertEquals(installed, file);
        assertTrue(downloads.getRequestedUris().isEmpty());
    }

    @Test
    void outdatedFilesWithTheSameNameAreReplaced() throws Exception {
        taggedRelease("hmc-specifics", "1.21.1-latest", "hmc-specifics-1.21.1-fabric-latest.jar");
        Path installed = installed("hmc-specifics-1.21.1-fabric-latest.jar", "outdated");

        platform.download(root, null, "hmc-specifics", VersionArg.parse("fabric", "1.21.1"), ModType.MOD);

        assertArrayEquals(JAR, Files.readAllBytes(installed));
    }

    @Test
    void addonsDoNotDuplicateAnInstalledHmcSpecifics() throws Exception {
        taggedRelease("hmc-specifics", "1.21.1-latest", "hmc-specifics-1.21.1-fabric-latest.jar");
        release("hmc-prometheus", "0.1.0", "hmc-prometheus-0.1.0.jar");
        Path pinned = installed("hmc-specifics-1.21.1-fabric-3.0.1.jar", MOD_ID_PREFIX + "headlessmc");

        platform.download(root, null, "hmc-prometheus", VersionArg.parse("fabric", "1.21.1"), ModType.MOD);

        assertFalse(Files.exists(pinned));
        try (Stream<Path> files = Files.list(root.resolve("mods"))) {
            assertEquals(
                Set.of("hmc-specifics-1.21.1-fabric-latest.jar", "hmc-prometheus-0.1.0.jar"),
                files.map(file -> file.getFileName().toString()).collect(Collectors.toSet())
            );
        }
    }

    @Test
    void failedDownloadsKeepTheInstalledVersion() throws Exception {
        String name = "hmc-specifics-1.21.1-fabric-latest.jar";
        URI uri = URI.create("https://github.com/headlesshq/hmc-specifics/releases/download/1.21.1-latest/" + name);
        downloads.register(uri, "corrupted".getBytes(StandardCharsets.UTF_8));
        taggedReleases.put("headlesshq/hmc-specifics@1.21.1-latest", new GitHubRelease("1.21.1-latest", List.of(
            new GitHubRelease.Asset(name, uri.toString(), (long) JAR.length, "sha256:" + sha256(JAR))
        )));
        Path old = installed("hmc-specifics-1.21.1-fabric-3.0.1.jar", MOD_ID_PREFIX + "headlessmc");

        assertThrows(Exception.class,
            () -> platform.download(root, null, "hmc-specifics", VersionArg.parse("fabric", "1.21.1"), ModType.MOD));

        assertTrue(Files.exists(old));
        assertFalse(Files.exists(root.resolve("mods").resolve(name)));
    }

    @Test
    void sha256IsParsedFromTheDigest() {
        GitHubRelease.Asset asset = new GitHubRelease.Asset("a.jar", "https://example.com/a.jar", 1L, "sha256:abc");

        assertEquals(Optional.of("abc"), asset.sha256());
        assertEquals(Optional.empty(), new GitHubRelease.Asset("a.jar", "https://example.com/a.jar", 1L, null).sha256());
    }

}
