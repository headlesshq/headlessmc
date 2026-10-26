package io.github.headlesshq.headlessmc.mods.github;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GitHubPlatformTest {
    private static final byte[] JAR = "jar".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path root;

    private final Map<String, GitHubRelease> latestReleases = new HashMap<>();
    private final Map<String, GitHubRelease> taggedReleases = new HashMap<>();
    private final MockDownloadService downloads = new MockDownloadService();

    private final GitHubAPI api = new GitHubAPI() {
        @Override
        public GitHubRelease getLatestRelease(String owner, String repo) {
            return orNotFound(latestReleases.get(owner + "/" + repo));
        }

        @Override
        public GitHubRelease getReleaseByTag(String owner, String repo, String tag) {
            return orNotFound(taggedReleases.get(owner + "/" + repo + "@" + tag));
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

        platform = new GitHubDistributionPlatform(instance(repositories), downloads, api, modCache);
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
        return new GitHubRelease.Asset(name, uri.toString(), (long) JAR.length, null);
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
            platform.search("hmc", Set.of(ModType.MOD)).stream().map(RemoteMod::id).toList());
        assertEquals(List.of("hmc-prometheus"),
            platform.search("PROMETHEUS", Set.of()).stream().map(RemoteMod::id).toList());
        assertEquals(List.of(), platform.search("hmc", Set.of(ModType.SHADER)));
        assertEquals(List.of(), platform.search("hmc", VersionArg.parse("vanilla", "1.21.1"), Set.of(ModType.MOD)));
        assertEquals(5, platform.search("", VersionArg.parse("fabric", "1.21.1"), Set.of(ModType.MOD)).size());
    }

    @Test
    void sha256IsParsedFromTheDigest() {
        GitHubRelease.Asset asset = new GitHubRelease.Asset("a.jar", "https://example.com/a.jar", 1L, "sha256:abc");

        assertEquals(Optional.of("abc"), asset.sha256());
        assertEquals(Optional.empty(), new GitHubRelease.Asset("a.jar", "https://example.com/a.jar", 1L, null).sha256());
    }

}
