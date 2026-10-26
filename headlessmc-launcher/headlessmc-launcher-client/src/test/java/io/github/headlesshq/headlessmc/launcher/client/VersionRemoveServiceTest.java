package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.client.FakeVersionMatcherService;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import io.github.headlesshq.headlessmc.util.maven.Artifact;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.github.headlesshq.headlessmc.version.service.FakeVersionJsonService;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class VersionRemoveServiceTest {
    private static final String FABRIC_9 = "fabric-loader-0.16.9-1.21.1";
    private static final String FABRIC_5 = "fabric-loader-0.16.5-1.21.1";
    private static final String SHARED = "aa00000000000000000000000000000000000000";
    private static final String ONLY_17 = "bb00000000000000000000000000000000000000";
    private static final String ONLY_12 = "cc00000000000000000000000000000000000000";

    @TempDir
    Path root;

    private final FakeVersionJsonService versionJsonService = new FakeVersionJsonService();
    private final FakeVersionMatcherService matcherService = new FakeVersionMatcherService();
    private FakePlatformService platformService;
    private FakeProfileService profileService;
    private McFiles mcFiles;

    private record Download(@Nullable String getPath) implements Version.Download {
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
            return null;
        }
    }

    private record Downloads(Version.@Nullable Download getArtifact, @Nullable Map<String, Version.Download> getClassifiers)
        implements Version.LibraryDownloads {
    }

    private record Library(String getName, Version.@Nullable LibraryDownloads getDownloads) implements Version.Library {
        @Override
        public @Nullable Map<String, String> getNatives() {
            return null;
        }

        @Override
        public Version.@Nullable Extract getExtract() {
            return null;
        }

        @Override
        public @Nullable List<Version.Rule> getRules() {
            return null;
        }

        @Override
        public @Nullable String getMd5() {
            return null;
        }

        @Override
        public @Nullable String getSha256() {
            return null;
        }

        @Override
        public @Nullable String getSha512() {
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
            return null;
        }

        @Override
        public @Nullable String getPath() {
            return null;
        }
    }

    private record AssetIndex(String getId) implements Version.AssetIndex {
        @Override
        public @Nullable Long getTotalSize() {
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
            return null;
        }

        @Override
        public @Nullable String getPath() {
            return null;
        }
    }

    private record Logging(Version.Download getFile) implements Version.LoggingConfiguration {
        @Override
        public String getArgument() {
            return "-Dlog4j.configurationFile=${path}";
        }

        @Override
        public String getType() {
            return "log4j2-xml";
        }
    }

    private record LoggingFile(String getId) implements Version.Download {
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
            return null;
        }

        @Override
        public @Nullable String getPath() {
            return null;
        }
    }

    @BeforeEach
    void setup() throws IOException {
        mcFiles = TestFiles.mcFiles(root);
        platformService = new FakePlatformService(
            new FakeVanillaPlatform("1.21.1", "1.20.4", "1.5.2", "1.5.1", "1.7.2"),
            FakePlatform.create("fabric", new String[]{"1.21.1", "0.16.9", "0.16.5"})
        );
        profileService = new FakeProfileService(root.resolve("profiles"));

        install(new FakeVersion("1.21.1")
            .withLibrary(library("com.example:shared:1.0", true))
            .withLibrary(library("com.example:vanilla-only:1.0", true))
            .withAssetIndex(new AssetIndex("17"))
            .withLogging(logging("client-1.21.2.xml")), "1.21.1");
        install(new FakeVersion(FABRIC_9)
            .withInheritsFrom("1.21.1")
            .withLibrary(library("net.fabricmc:fabric-loader:0.16.9", false))
            .withLibrary(library("net.fabricmc:sponge-mixin:0.15.0", false)), "fabric", "1.21.1", "0.16.9");
        install(new FakeVersion(FABRIC_5)
            .withInheritsFrom("1.21.1")
            .withLibrary(library("net.fabricmc:fabric-loader:0.16.5", false))
            .withLibrary(library("net.fabricmc:sponge-mixin:0.15.0", false)), "fabric", "1.21.1", "0.16.5");
        install(new FakeVersion("1.20.4")
            .withLibrary(library("com.example:shared:1.0", true))
            .withLibrary(new Library("org.lwjgl:lwjgl-platform:2.9.4", new Downloads(null, Map.of(
                "natives-linux", new Download("org/lwjgl/lwjgl-platform/2.9.4/lwjgl-platform-2.9.4-natives-linux.jar"),
                "natives-windows", new Download("org/lwjgl/lwjgl-platform/2.9.4/lwjgl-platform-2.9.4-natives-windows.jar")
            ))))
            .withAssetIndex(new AssetIndex("12"))
            .withLogging(logging("client-1.12.xml")), "1.20.4");

        assetIndex("17", "", Map.of("shared.png", SHARED, "only17.png", ONLY_17));
        assetIndex("12", "", Map.of("shared.png", SHARED, "only12.png", ONLY_12));
        createFile(mcFiles.getAssetsDir().resolve("log_configs").resolve("client-1.21.2.xml"));
        createFile(mcFiles.getAssetsDir().resolve("log_configs").resolve("client-1.12.xml"));
        createFile(mcFiles.getAssetsDir().resolve("log_configs").resolve("client-1.12.xml-patched.xml"));
    }

    private VersionRemoveService service() {
        return new VersionRemoveService(
            matcherService,
            versionJsonService,
            platformService,
            profileService,
            new DefaultFileService(new DefaultFileSystemProvider()),
            new DefaultJacksonJsonService(),
            mcFiles
        );
    }

    private void install(FakeVersion version, String... versionArg) throws IOException {
        versionJsonService.add(version);
        matcherService.withMatch(version.getId(), VersionID.resolve(platformService, VersionArg.parse(versionArg)));
        Path dir = mcFiles.getVersionsDir().resolve(version.getId());
        createFile(dir.resolve(version.getId() + ".json"));
        createFile(dir.resolve(version.getId() + ".jar"));
        for (Version.Library library : version.getLibraries()) {
            Version.LibraryDownloads downloads = library.getDownloads();
            if (downloads != null && downloads.getClassifiers() != null) {
                for (Version.Download download : downloads.getClassifiers().values()) {
                    createFile(mcFiles.getLibraryDir().resolve(download.getPath()));
                }
            } else {
                createFile(library.getArtifact().jar(mcFiles.getLibraryDir()));
            }
        }
    }

    private Library library(String name, boolean withDownloads) {
        if (!withDownloads) {
            return new Library(name, null);
        }

        Path path = mcFiles.getLibraryDir().relativize(Artifact.of(name).jar(mcFiles.getLibraryDir()));
        return new Library(name, new Downloads(new Download(path.toString().replace('\\', '/')), null));
    }

    private static Map<String, Version.LoggingConfiguration> logging(String id) {
        return Map.of(Version.LOGGING_CLIENT, new Logging(new LoggingFile(id)));
    }

    private void assetIndex(String id, String flags, Map<String, String> objects) throws IOException {
        String json = objects.entrySet().stream()
            .map(e -> "\"%s\": {\"hash\": \"%s\", \"size\": 1}".formatted(e.getKey(), e.getValue()))
            .collect(Collectors.joining(",", "{" + flags + "\"objects\": {", "}}"));
        Path index = mcFiles.getAssetsDir().resolve("indexes").resolve(id + ".json");
        Files.createDirectories(index.getParent());
        Files.writeString(index, json);
        for (String hash : objects.values()) {
            createFile(object(hash));
        }
    }

    private Path object(String hash) {
        return mcFiles.getAssetsDir().resolve("objects").resolve(hash.substring(0, 2)).resolve(hash);
    }

    private Path library(String name) {
        return Artifact.of(name).jar(mcFiles.getLibraryDir());
    }

    private static void createFile(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        if (!Files.exists(file)) {
            Files.writeString(file, file.getFileName().toString());
        }
    }

    private VersionRemoval plan(String id) {
        return service().plan(versionJsonService.getVersion(id));
    }

    private void remove(String id) {
        VersionRemoveService service = service();
        service.remove(service.plan(versionJsonService.getVersion(id)));
        versionJsonService.remove(id);
    }

    @Test
    void removesModLoaderVersionButKeepsLibrariesNeededByOthers() {
        VersionRemoval removal = plan(FABRIC_9);
        assertFalse(removal.isBlocked());
        assertEquals(mcFiles.getVersionsDir().resolve(FABRIC_9), removal.files().getLast());

        remove(FABRIC_9);

        assertFalse(Files.exists(mcFiles.getVersionsDir().resolve(FABRIC_9)));
        assertFalse(Files.exists(library("net.fabricmc:fabric-loader:0.16.9")));
        assertFalse(Files.exists(library("net.fabricmc:fabric-loader:0.16.9").getParent()), "empty dirs are removed");
        assertTrue(Files.exists(library("net.fabricmc:fabric-loader:0.16.5")));
        assertTrue(Files.exists(library("net.fabricmc:sponge-mixin:0.15.0")));
        assertTrue(Files.exists(library("com.example:vanilla-only:1.0")), "the parent version still needs it");
        assertTrue(Files.exists(mcFiles.getAssetsDir().resolve("indexes").resolve("17.json")));
        assertTrue(Files.exists(object(ONLY_17)));
        assertTrue(Files.exists(mcFiles.getAssetsDir().resolve("log_configs").resolve("client-1.21.2.xml")));
        assertTrue(Files.exists(mcFiles.getVersionsDir().resolve("1.21.1")));
    }

    @Test
    void removesUnusedLibrariesAssetsAndLoggingConfigs() {
        remove("1.20.4");

        assertFalse(Files.exists(mcFiles.getVersionsDir().resolve("1.20.4")));
        assertTrue(Files.exists(library("com.example:shared:1.0")));
        assertFalse(Files.exists(mcFiles.getLibraryDir().resolve("org/lwjgl/lwjgl-platform/2.9.4/lwjgl-platform-2.9.4-natives-linux.jar")));
        assertFalse(Files.exists(mcFiles.getLibraryDir().resolve("org/lwjgl/lwjgl-platform/2.9.4/lwjgl-platform-2.9.4-natives-windows.jar")));
        assertFalse(Files.exists(mcFiles.getLibraryDir().resolve("org")));
        assertTrue(Files.exists(mcFiles.getLibraryDir()));

        assertFalse(Files.exists(mcFiles.getAssetsDir().resolve("indexes").resolve("12.json")));
        assertFalse(Files.exists(object(ONLY_12)));
        assertTrue(Files.exists(object(SHARED)));
        assertTrue(Files.exists(object(ONLY_17)));

        assertFalse(Files.exists(mcFiles.getAssetsDir().resolve("log_configs").resolve("client-1.12.xml")));
        assertFalse(Files.exists(mcFiles.getAssetsDir().resolve("log_configs").resolve("client-1.12.xml-patched.xml")));
        assertTrue(Files.exists(mcFiles.getAssetsDir().resolve("log_configs").resolve("client-1.21.2.xml")));
    }

    @Test
    void keepsAssetsOfAnIndexUsedByAnotherVersion() throws IOException {
        install(new FakeVersion("1.20.4-copy").withAssetIndex(new AssetIndex("12")), "1.20.4");

        remove("1.20.4");

        assertTrue(Files.exists(mcFiles.getAssetsDir().resolve("indexes").resolve("12.json")));
        assertTrue(Files.exists(object(ONLY_12)));
    }

    @Test
    void removesResourcesAndVirtualAssetsNotNeededByOtherVersions() throws IOException {
        install(new FakeVersion("1.5.2").withAssetIndex(new AssetIndex("pre-1.6")), "1.5.2");
        install(new FakeVersion("1.5.1").withAssetIndex(new AssetIndex("pre-1.6-other")), "1.5.1");
        assetIndex("pre-1.6", "\"map_to_resources\": true,", Map.of("sound/a.ogg", ONLY_12, "sound/b.ogg", SHARED));
        assetIndex("pre-1.6-other", "\"map_to_resources\": true,", Map.of("sound/b.ogg", SHARED));
        createFile(mcFiles.getResourcesDir().resolve("sound").resolve("a.ogg"));
        createFile(mcFiles.getResourcesDir().resolve("sound").resolve("b.ogg"));

        install(new FakeVersion("1.7.2").withAssetIndex(new AssetIndex("legacy")), "1.7.2");
        assetIndex("legacy", "\"virtual\": true,", Map.of("icons/icon.png", ONLY_17));
        createFile(mcFiles.getAssetsDir().resolve("virtual").resolve("legacy").resolve("icons").resolve("icon.png"));

        remove("1.5.2");
        assertFalse(Files.exists(mcFiles.getResourcesDir().resolve("sound").resolve("a.ogg")));
        assertTrue(Files.exists(mcFiles.getResourcesDir().resolve("sound").resolve("b.ogg")));
        assertTrue(Files.exists(object(ONLY_12)), "still needed by the asset index of 1.20.4");

        remove("1.7.2");
        assertFalse(Files.exists(mcFiles.getAssetsDir().resolve("virtual").resolve("legacy")));
        assertTrue(Files.exists(object(ONLY_17)), "still needed by the asset index of 1.21.1");
    }

    @Test
    void versionsInheritingFromTheVersionBlockTheRemoval() {
        VersionRemoval removal = plan("1.21.1");

        assertTrue(removal.isBlocked());
        assertEquals(List.of(FABRIC_9, FABRIC_5), removal.dependentVersions().stream().map(Version::getId).toList());
        assertTrue(removal.files().stream().noneMatch(file -> file.equals(library("com.example:vanilla-only:1.0"))),
            "libraries needed by dependent versions are kept even when forced");
    }

    @Test
    void removesDependentVersionsAndProfilesWithDependents() {
        profileService.save(new Profile("fabric", VersionArg.parse("fabric", "1.21.1"), root));
        profileService.save(new Profile("vanilla-old", VersionArg.parse("1.20.4"), root));
        VersionRemoveService service = service();

        VersionRemoval removal = service.plan(versionJsonService.getVersion("1.21.1"), true);
        assertFalse(removal.isBlocked());
        assertEquals(List.of("1.21.1", FABRIC_9, FABRIC_5), removal.removedVersions().stream().map(Version::getId).toList());
        assertEquals(List.of("fabric"), removal.removedProfiles().stream().map(Profile::name).toList());
        assertEquals(mcFiles.getVersionsDir().resolve("1.21.1"), removal.files().getLast());

        service.remove(removal);

        assertFalse(Files.exists(mcFiles.getVersionsDir().resolve(FABRIC_9)));
        assertFalse(Files.exists(mcFiles.getVersionsDir().resolve(FABRIC_5)));
        assertFalse(Files.exists(library("net.fabricmc:sponge-mixin:0.15.0")));
        assertFalse(Files.exists(library("com.example:vanilla-only:1.0")));
        assertTrue(Files.exists(library("com.example:shared:1.0")), "1.20.4 still needs it");
        assertFalse(Files.exists(object(ONLY_17)));
        assertTrue(Files.exists(object(SHARED)));
        assertEquals(List.of("vanilla-old"), profileService.getProfiles().stream().map(Profile::name).toList());
    }

    @Test
    void profilesThatNeedTheVersionBlockTheRemoval() {
        profileService.save(new Profile("specific", VersionArg.parse("fabric", "1.21.1", "0.16.9"), root));
        profileService.save(new Profile("any", VersionArg.parse("fabric", "1.21.1"), root));
        profileService.save(new Profile("latest", VersionArg.parse("fabric", "1.21.1", "latest"), root));
        profileService.save(new Profile("other", VersionArg.parse("fabric", "1.21.1", "0.16.5"), root));
        profileService.save(new Profile("server", VersionArg.parse("server", "fabric", "1.21.1", "0.16.9"), root));

        VersionRemoval removal = plan(FABRIC_9);
        assertEquals(List.of("specific"), removal.blockingProfiles().stream().map(Profile::name).toList());

        profileService.remove(profileService.getProfile("specific").orElseThrow());
        remove(FABRIC_9);

        removal = plan(FABRIC_5);
        assertEquals(List.of("any", "latest", "other"), removal.blockingProfiles().stream().map(Profile::name).toList());
    }

    @Test
    void findsVersionsByIdOrVersionArg() {
        VersionRemoveService service = service();
        assertEquals(FABRIC_9, service.findVersion(List.of(FABRIC_9)).getId());
        assertEquals(FABRIC_5, service.findVersion(List.of("fabric", "1.21.1", "0.16.5")).getId());
        assertEquals("1.20.4", service.findVersion(List.of("vanilla", "1.20.4")).getId());

        IllegalArgumentException ambiguous = assertThrows(IllegalArgumentException.class,
            () -> service.findVersion(List.of("fabric", "1.21.1")));
        assertTrue(ambiguous.getMessage().contains(FABRIC_9));
        assertTrue(ambiguous.getMessage().contains(FABRIC_5));

        assertThrows(IllegalArgumentException.class, () -> service.findVersion(List.of("1.5.2")));
        assertThrows(IllegalArgumentException.class, () -> service.findVersion(List.of()));
    }

}
