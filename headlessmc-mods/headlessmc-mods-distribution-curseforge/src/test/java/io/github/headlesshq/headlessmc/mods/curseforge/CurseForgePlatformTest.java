package io.github.headlesshq.headlessmc.mods.curseforge;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.exceptions.VerificationException;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.mods.distribution.ModCache;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionException;
import io.github.headlesshq.headlessmc.mods.distribution.RemoteMod;
import io.github.headlesshq.headlessmc.net.MockDownloadService;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CurseForgePlatformTest {
    private record Search(String apiKey, @Nullable Integer classId, @Nullable String searchFilter, @Nullable String slug,
                          @Nullable String gameVersion, @Nullable Integer modLoaderType) {}

    private static final String API_KEY = "test-key";
    private static final URI MOD_URL = URI.create("https://edge.forgecdn.net/files/1/2/hmc-specifics.jar");
    private static final byte[] MOD = "hmc-specifics-jar".getBytes(StandardCharsets.UTF_8);
    private static final CurseForgeMod SPECIFICS = new CurseForgeMod(
        1754192, 6, "hmc-specifics", "hmc-specifics", "Minecraft API", true,
        new CurseForgeMod.Links("https://www.curseforge.com/minecraft/mc-mods/hmc-specifics")
    );
    private static final CurseForgeMod FAITHFUL = new CurseForgeMod(
        237299, 12, "faithful-32x", "Faithful 32x", "Faithful", false,
        new CurseForgeMod.Links("https://www.curseforge.com/minecraft/texture-packs/faithful-32x")
    );

    @TempDir
    Path root;

    private final List<Search> searches = new ArrayList<>();
    private final List<CurseForgeMod> hits = new ArrayList<>();
    private final List<CurseForgeFile> files = new ArrayList<>();
    private final List<String> requestedModIds = new ArrayList<>();
    private @Nullable String apiKey = API_KEY;

    private final CurseForgeAPI api = new CurseForgeAPI() {
        @Override
        public CurseForgeResponse<List<CurseForgeMod>> search(
            String apiKey, int gameId, @Nullable Integer classId, @Nullable String searchFilter, @Nullable String slug,
            @Nullable Integer authorId, @Nullable String gameVersion, @Nullable Integer modLoaderType,
            @Nullable Integer pageSize
        ) {
            assertEquals(CurseForgeIds.MINECRAFT, gameId);
            searches.add(new Search(apiKey, classId, searchFilter, slug, gameVersion, modLoaderType));
            return new CurseForgeResponse<>(hits);
        }

        @Override
        public CurseForgeResponse<CurseForgeMod> getMod(String apiKey, String modId) {
            requestedModIds.add(modId);
            return new CurseForgeResponse<>(SPECIFICS);
        }

        @Override
        public CurseForgeResponse<List<CurseForgeFile>> getFiles(
            String apiKey, long modId, @Nullable String gameVersion, @Nullable Integer modLoaderType
        ) {
            return new CurseForgeResponse<>(files);
        }
    };

    private final ModCache modCache = new ModCache() {
        @Override
        public Optional<RemoteMod> find(Path file) throws HeadlessMcIOException {
            return Optional.empty();
        }

        @Override
        public void add(Path file, RemoteMod mod) {

        }
    };

    private final MockDownloadService downloads = new MockDownloadService();

    private CurseForgeDistributionPlatform platform;

    @BeforeEach
    void setup() {
        platform = new CurseForgeDistributionPlatform(downloads, api, () -> () -> Optional.ofNullable(apiKey), modCache);
        hits.add(SPECIFICS);
        downloads.register(MOD_URL, MOD);
    }

    private CurseForgeFile file(String name, @Nullable URI url, Instant date, String... gameVersions) {
        return file(name, url, date, List.of(), gameVersions);
    }

    private CurseForgeFile file(
        String name, @Nullable URI url, Instant date, List<CurseForgeFile.Hash> hashes, String... gameVersions
    ) {
        return new CurseForgeFile(1, name, date, MOD.length, url == null ? null : url.toString(), hashes,
            List.of(gameVersions));
    }

    private static String sha1(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
    }

    @Test
    void nameIsCurseforge() {
        assertEquals("curseforge", platform.getName());
    }

    @Test
    void searchesWithClassIdAndApiKey() {
        List<RemoteMod> result = platform.search("hmc", ModType.MOD);

        assertEquals(List.of(new RemoteMod("hmc-specifics", "hmc-specifics", "Minecraft API")), result);
        assertEquals(new Search(API_KEY, 6, "hmc", null, null, null), searches.getFirst());
    }

    @Test
    void searchesScopedToAVersionIncludeLoaderAndVersion() {
        platform.search("hmc", VersionArg.parse("fabric", "1.21.1"), ModType.MOD);

        assertEquals(new Search(API_KEY, 6, "hmc", null, "1.21.1", 4), searches.getFirst());
    }

    @Test
    void resourcePackSearchesDoNotFilterByLoader() {
        platform.search("faithful", VersionArg.parse("fabric", "1.21.1"), ModType.RESOURCE_PACK);

        assertEquals(new Search(API_KEY, 12, "faithful", null, "1.21.1", null), searches.getFirst());
    }

    @Test
    void modPackSearchesDoNotFilterByLoader() {
        platform.search("fabulously", VersionArg.parse("fabric", "1.21.1"), ModType.MOD_PACK);

        assertEquals(new Search(API_KEY, 4471, "fabulously", null, "1.21.1", null), searches.getFirst());
    }

    @Test
    void isEnabledOnlyWithApiKey() {
        assertTrue(platform.isEnabled());

        apiKey = " ";
        assertFalse(platform.isEnabled());

        apiKey = null;
        assertFalse(platform.isEnabled());
    }

    @Test
    void missingApiKeyThrows() {
        apiKey = null;

        ModDistributionException e = assertThrows(ModDistributionException.class,
            () -> platform.search("hmc", ModType.MOD));
        assertTrue(e.getMessage().contains(CurseForgeConfig.PREFIX + ".api-key"));
        assertTrue(searches.isEmpty());
    }

    @Test
    void downloadsTheNewestMatchingFile() throws Exception {
        URI oldUrl = URI.create("https://edge.forgecdn.net/files/old.jar");
        files.add(file("old.jar", oldUrl, Instant.parse("2026-01-01T00:00:00Z"), "1.21.1", "Fabric"));
        files.add(file("hmc-specifics.jar", MOD_URL, Instant.parse("2026-10-20T00:00:00Z"), "1.21.1", "Fabric"));
        files.add(file("forge.jar", MOD_URL, Instant.parse("2026-10-21T00:00:00Z"), "1.21.1", "Forge"));

        Path file = platform.download(root, null, "hmc-specifics", VersionArg.parse("fabric", "1.21.1"), ModType.MOD);

        assertEquals(root.resolve("mods").resolve("hmc-specifics.jar"), file);
        assertArrayEquals(MOD, Files.readAllBytes(file));
        assertEquals("hmc-specifics", searches.getFirst().slug());
        assertEquals(6, searches.getFirst().classId());
        assertFalse(downloads.wasRequested(oldUrl));
    }

    @Test
    void numericIdsAreLookedUpDirectly() throws Exception {
        files.add(file("hmc-specifics.jar", MOD_URL, Instant.now(), "1.21.1", "Fabric"));

        Path file = platform.download(root, null, "1754192", VersionArg.parse("fabric", "1.21.1"), ModType.MOD);

        assertArrayEquals(MOD, Files.readAllBytes(file));
        assertTrue(searches.isEmpty());
        assertEquals(List.of("1754192"), requestedModIds);
    }

    @Test
    void unknownSlugsThrow() {
        ModDistributionException e = assertThrows(ModDistributionException.class,
            () -> platform.download(root, null, "unknown", VersionArg.parse("fabric", "1.21.1"), ModType.MOD));
        assertTrue(e.getMessage().contains("Failed to find CurseForge project"));
    }

    @Test
    void filesForOtherVersionsOrLoadersAreRejected() {
        files.add(file("a.jar", MOD_URL, Instant.now(), "1.20.4", "Fabric"));
        files.add(file("b.jar", MOD_URL, Instant.now(), "1.21.1", "NeoForge"));

        ModDistributionException e = assertThrows(ModDistributionException.class,
            () -> platform.download(root, null, "hmc-specifics", VersionArg.parse("fabric", "1.21.1"), ModType.MOD));
        assertTrue(e.getMessage().contains("Failed to find any files"));
    }

    @Test
    void filesWithoutDownloadUrlPointToTheWebsite() {
        files.add(file("hmc-specifics.jar", null, Instant.now(), "1.21.1", "Fabric"));

        ModDistributionException e = assertThrows(ModDistributionException.class,
            () -> platform.download(root, null, "hmc-specifics", VersionArg.parse("fabric", "1.21.1"), ModType.MOD));
        assertTrue(e.getMessage().contains("https://www.curseforge.com/minecraft/mc-mods/hmc-specifics/files/1"));
    }

    @Test
    void manualDownloadLinksUseTheWebsiteOfTheMod() {
        hits.clear();
        hits.add(FAITHFUL);
        files.add(file("faithful.zip", null, Instant.now(), "1.21.1"));

        ModDistributionException e = assertThrows(ModDistributionException.class, () -> platform.download(
            root, null, "faithful-32x", VersionArg.parse("fabric", "1.21.1"), ModType.RESOURCE_PACK
        ));
        assertTrue(e.getMessage().contains("https://www.curseforge.com/minecraft/texture-packs/faithful-32x/files/1"));
    }

    @Test
    void downloadsAreVerifiedWithTheFileHashes() throws Exception {
        files.add(file("hmc-specifics.jar", MOD_URL, Instant.now(),
            List.of(new CurseForgeFile.Hash(sha1(MOD), CurseForgeFile.Hash.SHA1)), "1.21.1", "Fabric"));

        Path file = platform.download(root, null, "hmc-specifics", VersionArg.parse("fabric", "1.21.1"), ModType.MOD);

        assertArrayEquals(MOD, Files.readAllBytes(file));
    }

    @Test
    void downloadsWithWrongHashesFail() throws Exception {
        files.add(file("hmc-specifics.jar", MOD_URL, Instant.now(),
            List.of(new CurseForgeFile.Hash(sha1(new byte[]{1}), CurseForgeFile.Hash.SHA1)), "1.21.1", "Fabric"));

        assertThrows(VerificationException.class, () -> platform.download(
            root, null, "hmc-specifics", VersionArg.parse("fabric", "1.21.1"), ModType.MOD
        ));
        assertFalse(Files.exists(root.resolve("mods").resolve("hmc-specifics.jar")));
    }

    @Test
    void modpacksAreNotSupported() {
        assertThrows(ModDistributionException.class,
            () -> platform.download(root, null, "pack", VersionArg.parse("fabric", "1.21.1"), ModType.MOD_PACK));
    }

    @Test
    void hashesAreMappedToAlgorithmNames() {
        CurseForgeFile file = new CurseForgeFile(1, "a.jar", Instant.now(), 1, null, List.of(
            new CurseForgeFile.Hash("abc", CurseForgeFile.Hash.SHA1),
            new CurseForgeFile.Hash("def", CurseForgeFile.Hash.MD5),
            new CurseForgeFile.Hash("ghi", 99)
        ), List.of());

        assertEquals(Map.of("SHA-1", "abc", "MD5", "def"), file.hashesByAlgorithm());
    }

}
