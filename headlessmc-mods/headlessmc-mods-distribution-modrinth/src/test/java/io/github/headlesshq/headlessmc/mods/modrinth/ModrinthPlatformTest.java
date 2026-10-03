package io.github.headlesshq.headlessmc.mods.modrinth;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.mods.distribution.ModCache;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionException;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionPlatform;
import io.github.headlesshq.headlessmc.mods.distribution.RemoteMod;
import io.github.headlesshq.headlessmc.net.MockDownloadService;
import io.github.headlesshq.headlessmc.progressbar.ProgressBar;
import io.github.headlesshq.headlessmc.progressbar.ProgressBarServiceManager;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ModrinthPlatformTest {
    private record Query(String query, String facets) {}

    private record VersionQuery(String project, String gameVersions, String loaders) {}

    private static final URI MOD_URL = URI.create("https://cdn.modrinth.com/sodium.jar");
    private static final byte[] MOD = "sodium-jar".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path root;

    private final List<Query> queries = new ArrayList<>();
    private final List<VersionQuery> versionQueries = new ArrayList<>();
    private final List<ModrinthProject> hits = new ArrayList<>();
    private final List<ModrinthProjectVersion> projectVersions = new ArrayList<>();

    private final ModrinthAPI api = new ModrinthAPI() {
        @Override
        public ModrinthSearchResult search(String query, String facets) {
            queries.add(new Query(query, facets));
            return new ModrinthSearchResult(hits);
        }

        @Override
        public ModrinthProject getProject(String project) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ModrinthProjectVersion> getProjectVersions(String project, String gameVersions, String loaders) {
            versionQueries.add(new VersionQuery(project, gameVersions, loaders));
            return projectVersions;
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

    private final ProgressBarServiceManager progressBars = configuration -> ProgressBar.dummy();
    private final MockDownloadService downloads = new MockDownloadService();

    private ModpackDownloader modpackDownloader;
    private ModrinthDistributionPlatform platform;

    @BeforeEach
    void setup() {
        modpackDownloader = new ModpackDownloader(progressBars, new DefaultJacksonJsonService(), new DefaultFileService(new DefaultFileSystemProvider()));
        platform = new ModrinthDistributionPlatform(modpackDownloader, downloads, api, modCache);
        hits.add(new ModrinthProject("sodium", "Sodium", "Fast rendering"));
        downloads.register(MOD_URL, MOD);
    }

    private ModrinthProjectVersion version(List<String> gameVersions, List<String> loaders) {
        return new ModrinthProjectVersion(gameVersions, loaders, List.of(
            new ModrinthFile(Map.of(), MOD_URL.toString(), "sodium.jar", true, MOD.length)
        ));
    }

    @Test
    void nameIsTheDefaultPlatform() {
        assertEquals(ModDistributionPlatform.DEFAULT, platform.getName());
    }

    @Test
    void searchesWithModTypeFacets() {
        List<RemoteMod> result = platform.search("fast", Set.of(ModType.MOD));

        assertEquals(List.of(new RemoteMod("sodium", "Sodium", "Fast rendering")), result);
        assertEquals("fast", queries.getFirst().query());
        assertTrue(queries.getFirst().facets().contains("mod"));
    }

    @Test
    void searchesScopedToAVersionIncludeLoaderAndVersionFacets() {
        platform.search("fast", VersionArg.parse("fabric", "1.21.1"), Set.of(ModType.MOD));

        String facets = queries.getFirst().facets();
        assertTrue(facets.contains("fabric"));
        assertTrue(facets.contains("1.21.1"));
    }

    @Test
    void vanillaSearchesDoNotFilterByLoader() {
        platform.search("fast", VersionArg.parse("vanilla", "1.21.1"), Set.of(ModType.DATA_PACK));

        assertFalse(queries.getFirst().facets().contains("vanilla"));
    }

    @Test
    void downloadsThePrimaryFileOfAMatchingVersion() throws Exception {
        projectVersions.add(version(List.of("1.21.1"), List.of("fabric")));

        Path file = platform.download(root, null, "sodium", VersionArg.parse("fabric", "1.21.1"), ModType.MOD);

        assertEquals(root.resolve("mods").resolve("sodium.jar"), file);
        assertArrayEquals(MOD, Files.readAllBytes(file));
        assertEquals("sodium", versionQueries.getFirst().project());
        assertEquals("1.21.1", versionQueries.getFirst().gameVersions());
    }

    @Test
    void dataPacksAreDownloadedToTheWorld() throws Exception {
        projectVersions.add(version(List.of("1.21.1"), List.of(ModType.DATA_PACK.name())));
        Path world = root.resolve("saves").resolve("world");

        Path file = platform.download(root, world, "terralith", VersionArg.parse("vanilla", "1.21.1"), ModType.DATA_PACK);

        assertEquals(world.resolve("datapacks").resolve("sodium.jar"), file);
        assertArrayEquals(MOD, Files.readAllBytes(file));
    }

    @Test
    void dataPacksWithoutWorldThrow() {
        projectVersions.add(version(List.of("1.21.1"), List.of(ModType.DATA_PACK.name())));

        assertThrows(IllegalArgumentException.class,
            () -> platform.download(root, null, "terralith", VersionArg.parse("vanilla", "1.21.1"), ModType.DATA_PACK));
    }

    @Test
    void missingProjectVersionsThrow() {
        ModDistributionException e = assertThrows(ModDistributionException.class,
            () -> platform.download(root, null, "sodium", VersionArg.parse("fabric", "1.21.1"), ModType.MOD));
        assertTrue(e.getMessage().contains("Failed to find any versions"));
    }

    @Test
    void versionsForOtherLoadersOrGameVersionsAreRejected() {
        projectVersions.add(version(List.of("1.20.4"), List.of("fabric")));
        projectVersions.add(version(List.of("1.21.1"), List.of("forge")));

        ModDistributionException e = assertThrows(ModDistributionException.class,
            () -> platform.download(root, null, "sodium", VersionArg.parse("fabric", "1.21.1"), ModType.MOD));
        assertTrue(e.getMessage().contains("Failed to find any versions"));
    }

    @Test
    void versionsWithoutFilesAreRejected() {
        projectVersions.add(new ModrinthProjectVersion(List.of("1.21.1"), List.of("fabric"), List.of()));

        ModDistributionException e = assertThrows(ModDistributionException.class,
            () -> platform.download(root, null, "sodium", VersionArg.parse("fabric", "1.21.1"), ModType.MOD));
        assertTrue(e.getMessage().contains("Failed to get primary download file"));
    }

    @Test
    void theFirstFileIsUsedIfNoneIsPrimary() {
        ModrinthProjectVersion version = new ModrinthProjectVersion(List.of("1.21.1"), List.of("fabric"), List.of(
            new ModrinthFile(Map.of(), MOD_URL.toString(), "first.jar", false, MOD.length),
            new ModrinthFile(Map.of(), MOD_URL.toString(), "second.jar", false, MOD.length)
        ));

        assertEquals("first.jar", version.getPrimaryFile().orElseThrow().filename());
        assertEquals(List.of("1.21.1"), version.gameVersions());
    }

}
