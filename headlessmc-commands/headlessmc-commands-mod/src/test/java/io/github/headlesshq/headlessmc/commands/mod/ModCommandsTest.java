package io.github.headlesshq.headlessmc.commands.mod;

import io.github.headlesshq.headlessmc.console.RecordingConsole;
import io.github.headlesshq.headlessmc.console.format.SimpleTableProvider;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.mods.ModTypeService;
import io.github.headlesshq.headlessmc.mods.distribution.ModCache;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionPlatform;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionPlatformService;
import io.github.headlesshq.headlessmc.mods.distribution.RemoteMod;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.mods.Mod;
import io.github.headlesshq.headlessmc.platform.mods.ModReader;
import io.github.headlesshq.headlessmc.platform.mods.ModSupport;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ModCommandsTest {
    private record Download(Path dir, @Nullable Path worldDir, String id, VersionArg version, ModType type) {}

    private record Search(String query, Optional<VersionArg> version, Set<ModType> types) {}

    @TempDir
    Path root;

    private final RecordingConsole console = new RecordingConsole();
    private final TableProvider tables = new SimpleTableProvider();

    private final Map<String, List<Mod>> modsByFileName = new HashMap<>();
    private final List<Download> downloads = new ArrayList<>();
    private final List<Search> searches = new ArrayList<>();
    private final List<RemoteMod> searchResults = new ArrayList<>();

    private final ModTypeService modTypeService = new ModTypeService() {
        @Override
        public Set<ModType> getAllModTypes() {
            return Set.of(
                ModType.MOD, ModType.PLUGIN, ModType.RESOURCE_PACK, ModType.MOD_PACK, ModType.DATA_PACK, ModType.SHADER
            );
        }

        @Override
        public Optional<ModType> getByName(String name) {
            return getAllModTypes().stream().filter(type -> type.name().equalsIgnoreCase(name)).findFirst();
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

    private final ModReader modReader = new ModReader() {
        @Override
        public List<Mod> read(Path jar) {
            return modsByFileName.getOrDefault(jar.getFileName().toString(), List.of());
        }

        @Override
        public Optional<List<Mod>> readEntry(InputStream inputStream) {
            return Optional.empty();
        }

        @Override
        public Set<String> getEntryNames() {
            return Set.of("fabric.mod.json");
        }
    };

    private final ModDistributionPlatform distributionPlatform = new ModDistributionPlatform() {
        @Override
        public List<RemoteMod> search(String query, Set<ModType> types) {
            searches.add(new Search(query, Optional.empty(), types));
            return searchResults;
        }

        @Override
        public List<RemoteMod> search(String query, VersionArg version, Set<ModType> types) {
            searches.add(new Search(query, Optional.of(version), types));
            return searchResults;
        }

        @Override
        public Path download(Path dir, @Nullable Path worldDir, String id, VersionArg version, ModType type) {
            downloads.add(new Download(dir, worldDir, id, version, type));
            return dir.resolve(id + ".jar");
        }

        @Override
        public String getName() {
            return "fake";
        }
    };

    private final ModDistributionPlatformService distributionPlatformService = new ModDistributionPlatformService() {
        @Override
        public ModDistributionPlatform defaultPlatform() {
            return distributionPlatform;
        }

        @Override
        public Stream<ModDistributionPlatform> platforms() {
            return Stream.of(distributionPlatform);
        }

        @Override
        public Optional<ModDistributionPlatform> byName(String name) {
            return platforms().filter(platform -> platform.getName().equalsIgnoreCase(name)).findFirst();
        }
    };

    private FakePlatform fabric;
    private FakePlatformService platformService;
    private FakeProfileService profileService;
    private ProfileResolver profileResolver;
    private ModListingService modListingService;

    @BeforeEach
    void setup() {
        fabric = FakePlatform.create("fabric", new String[]{"1.21.1", "0.16.9"});
        fabric.withModSupport(new ModSupport(List.of(ModType.MOD, ModType.RESOURCE_PACK), List.of(modReader)));
        platformService = new FakePlatformService(new FakeVanillaPlatform("1.21.1"), fabric);
        profileService = new FakeProfileService(root);
        profileResolver = new ProfileResolver(platformService, profileService);
        modListingService = new ModListingService(platformService, profileResolver, modTypeService, modCache);
    }

    private Profile profile(String name, String platform) {
        return profileService.createDefault(name, VersionArg.parse(platform, "1.21.1"));
    }

    private Path createFile(Path file) {
        try {
            Files.createDirectories(file.getParent());
            return Files.createFile(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Mod mod(String id, String name) {
        return new Mod(id, name, Optional.empty(), List.of("Author"), Map.of());
    }

    // ------------------------------------------------------------------ list

    private ListCommand list() {
        return new ListCommand(modListingService, tables, console);
    }

    @Test
    void listWithoutProfileThrows() {
        assertThrows(IllegalArgumentException.class, list()::run);
    }

    @Test
    void listShowsModsOfAllModTypes() {
        Profile profile = profile("main", "fabric");
        createFile(profile.path().resolve("mods").resolve("sodium.jar"));
        createFile(profile.path().resolve("mods").resolve("notajar.txt"));
        createFile(profile.path().resolve("resourcepacks").resolve("faithful.jar"));
        createFile(profile.path().resolve("shaderpacks").resolve("bsl.zip"));
        modsByFileName.put("sodium.jar", List.of(mod("sodium", "Sodium")));
        modsByFileName.put("faithful.jar", List.of(mod("faithful", "Faithful")));

        ListCommand command = list();
        command.setVersionArg(List.of("main"));
        command.run();

        String output = console.output();
        assertTrue(output.contains("sodium"));
        assertTrue(output.contains("Faithful"));
        assertTrue(output.contains("Author"));
        assertTrue(output.contains("bsl.zip"));
        assertFalse(output.contains("notajar"));
    }

    @Test
    void listWithTypeOnlyShowsThatType() {
        Profile profile = profile("main", "fabric");
        createFile(profile.path().resolve("mods").resolve("sodium.jar"));
        createFile(profile.path().resolve("resourcepacks").resolve("faithful.jar"));
        modsByFileName.put("sodium.jar", List.of(mod("sodium", "Sodium")));
        modsByFileName.put("faithful.jar", List.of(mod("faithful", "Faithful")));

        ListCommand command = list();
        command.setVersionArg(List.of("main"));
        command.setType("resourcepack");
        command.run();

        assertTrue(console.output().contains("Faithful"));
        assertFalse(console.output().contains("sodium"));
    }

    @Test
    void listWithUnknownTypeThrows() {
        profile("main", "fabric");

        ListCommand command = list();
        command.setVersionArg(List.of("main"));
        command.setType("unknown");
        assertThrows(IllegalArgumentException.class, command::run);
    }

    @Test
    void listSkipsUnreadableJars() {
        Profile profile = profile("main", "fabric");
        createFile(profile.path().resolve("mods").resolve("unknown.jar"));

        ListCommand command = list();
        command.setVersionArg(List.of("main"));
        command.run();

        assertFalse(console.output().contains("unknown.jar"));
    }

    @Test
    void listOnPlatformWithoutModSupportSkipsMods() {
        Profile profile = profile("plain", "vanilla");
        createFile(profile.path().resolve("mods").resolve("sodium.jar"));
        createFile(profile.path().resolve("resourcepacks").resolve("faithful.zip"));

        ListCommand command = list();
        command.setVersionArg(List.of("plain"));
        command.run();

        assertFalse(console.output().contains("sodium.jar"));
        assertTrue(console.output().contains("faithful.zip"));
    }

    @Test
    void listDatapacksOfAllWorlds() {
        Profile profile = profile("main", "fabric");
        Path saves = profile.path().resolve(Worlds.SAVES);
        createFile(saves.resolve("World A").resolve(Worlds.DATAPACKS).resolve("terralith.zip"));
        createFile(saves.resolve("World B").resolve(Worlds.DATAPACKS).resolve("incendium.zip"));

        ListCommand command = list();
        command.setVersionArg(List.of("main"));
        command.run();

        assertTrue(console.output().contains("terralith.zip"));
        assertTrue(console.output().contains("incendium.zip"));
    }

    @Test
    void listDatapacksOfOneWorld() {
        Profile profile = profile("main", "fabric");
        Path saves = profile.path().resolve(Worlds.SAVES);
        createFile(saves.resolve("World A").resolve(Worlds.DATAPACKS).resolve("terralith.zip"));
        createFile(saves.resolve("World B").resolve(Worlds.DATAPACKS).resolve("incendium.zip"));
        createFile(profile.path().resolve("resourcepacks").resolve("faithful.jar"));
        modsByFileName.put("faithful.jar", List.of(mod("faithful", "Faithful")));

        ListCommand command = list();
        command.setVersionArg(List.of("main"));
        command.setWorld("World A");
        command.run();

        assertTrue(console.output().contains("terralith.zip"));
        assertFalse(console.output().contains("incendium.zip"));
        assertFalse(console.output().contains("Faithful"));
    }

    // ---------------------------------------------------------------- worlds

    @Test
    void worldsWithoutProfileThrows() {
        WorldsCommand command = new WorldsCommand(profileResolver, tables, console);
        assertThrows(IllegalArgumentException.class, command::run);
    }

    @Test
    void worldsListsSaves() {
        Profile profile = profile("main", "fabric");
        createFile(profile.path().resolve(Worlds.SAVES).resolve("New World").resolve("level.dat"));

        WorldsCommand command = new WorldsCommand(profileResolver, tables, console);
        command.setVersionArg(List.of("main"));
        command.run();

        assertTrue(console.output().contains("New World"));
    }

    // ------------------------------------------------------------------- add

    private AddCommand add() {
        return new AddCommand(distributionPlatformService, profileResolver, platformService, modTypeService, console);
    }

    @Test
    void addWithoutModThrows() {
        AddCommand command = add();
        command.setType("mod");
        command.setVersionArg(List.of("main"));
        assertThrows(IllegalArgumentException.class, command::run);
    }

    @Test
    void addWithoutTypeThrows() {
        AddCommand command = add();
        command.setMod("sodium");
        command.setVersionArg(List.of("main"));
        assertThrows(IllegalArgumentException.class, command::run);
    }

    @Test
    void addWithoutProfileThrows() {
        AddCommand command = add();
        command.setMod("sodium");
        command.setType("mod");
        assertThrows(IllegalArgumentException.class, command::run);
    }

    @Test
    void addDownloadsModToProfile() {
        Profile profile = profile("main", "fabric");

        AddCommand command = add();
        command.setType("mod");
        command.setMod("sodium");
        command.setVersionArg(List.of("main"));
        command.run();

        assertEquals(List.of(new Download(profile.path(), null, "sodium", profile.version(), ModType.MOD)), downloads);
        assertTrue(console.output().contains("Downloaded " + profile.path().resolve("sodium.jar") + " successfully."));
    }

    @Test
    void addUsesExplicitType() {
        AddCommand command = add();
        command.setType("ResourcePack");
        command.setMod("faithful");
        command.setVersionArg(List.of("main"));
        profile("main", "fabric");
        command.run();

        assertEquals(ModType.RESOURCE_PACK, downloads.getFirst().type());
    }

    @Test
    void addWithUnknownTypeThrows() {
        profile("main", "fabric");

        AddCommand command = add();
        command.setType("unknown");
        command.setMod("sodium");
        command.setVersionArg(List.of("main"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("Failed to find mod type unknown"));
    }

    @Test
    void addModToUnsupportedPlatformStillDownloads() {
        Profile profile = profile("plain", "vanilla");

        AddCommand command = add();
        command.setType("mod");
        command.setMod("sodium");
        command.setVersionArg(List.of("plain"));
        command.run();

        assertEquals(profile.path(), downloads.getFirst().dir());
    }

    @Test
    void addWithUnknownDistributionPlatformThrows() {
        profile("main", "fabric");

        AddCommand command = add();
        command.setType("mod");
        command.setMod("sodium");
        command.setPlatform("curseforge");
        command.setVersionArg(List.of("main"));
        assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(downloads.isEmpty());
    }

    @Test
    void addWithNamedDistributionPlatform() {
        profile("main", "fabric");

        AddCommand command = add();
        command.setType("mod");
        command.setMod("sodium");
        command.setPlatform("FAKE");
        command.setVersionArg(List.of("main"));
        command.run();

        assertEquals(1, downloads.size());
    }

    @Test
    void addDatapackWithoutWorldThrows() {
        profile("main", "fabric");

        AddCommand command = add();
        command.setType("datapack");
        command.setMod("terralith");
        command.setVersionArg(List.of("main"));
        assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(downloads.isEmpty());
    }

    @Test
    void addDatapackToWorld() {
        Profile profile = profile("main", "fabric");
        createFile(profile.path().resolve(Worlds.SAVES).resolve("New World").resolve("level.dat"));

        AddCommand command = add();
        command.setType("datapack");
        command.setMod("terralith");
        command.setWorld("New World");
        command.setVersionArg(List.of("main"));
        command.run();

        Download download = downloads.getFirst();
        assertEquals(profile.path(), download.dir());
        assertEquals(profile.path().resolve(Worlds.SAVES).resolve("New World"), download.worldDir());
        assertEquals(ModType.DATA_PACK, download.type());
    }

    // ---------------------------------------------------------------- remove

    private RemoveCommand remove() {
        return new RemoveCommand(modListingService, profileService, tables, console);
    }

    @Test
    void removeWithoutModThrows() {
        assertThrows(IllegalArgumentException.class, remove()::run);
    }

    @Test
    void removeByFileName() {
        Profile profile = profile("main", "fabric");
        Path jar = createFile(profile.path().resolve("mods").resolve("sodium-0.5.jar"));
        modsByFileName.put("sodium-0.5.jar", List.of(mod("sodium", "Sodium")));

        RemoveCommand command = remove();
        command.setVersionArg(List.of("main"));
        command.setFile("sodium-0.5.jar");
        command.run();

        assertFalse(Files.exists(jar));
        assertTrue(console.output().contains("Deleted mod successfully."));
    }

    @Test
    void removeByFullPath() {
        Profile profile = profile("main", "fabric");
        Path jar = createFile(profile.path().resolve("mods").resolve("sodium-0.5.jar"));
        modsByFileName.put("sodium-0.5.jar", List.of(mod("sodium", "Sodium")));

        RemoveCommand command = remove();
        command.setVersionArg(List.of("main"));
        command.setFile(jar.toString());
        command.run();

        assertFalse(Files.exists(jar));
    }

    @Test
    void removeByModId() {
        Profile profile = profile("main", "fabric");
        Path jar = createFile(profile.path().resolve("mods").resolve("some-file.jar"));
        modsByFileName.put("some-file.jar", List.of(mod("sodium", "Sodium")));

        RemoveCommand command = remove();
        command.setVersionArg(List.of("main"));
        command.setMod("SODIUM");
        command.run();

        assertFalse(Files.exists(jar));
    }

    @Test
    void removeByModIdAndMismatchingFileThrows() {
        Profile profile = profile("main", "fabric");
        Path jar = createFile(profile.path().resolve("mods").resolve("some-file.jar"));
        modsByFileName.put("some-file.jar", List.of(mod("sodium", "Sodium")));

        RemoveCommand command = remove();
        command.setVersionArg(List.of("main"));
        command.setMod("sodium");
        command.setFile("other-file.jar");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("Failed to find mod sodium with file other-file.jar"));
        assertTrue(Files.exists(jar));
    }

    @Test
    void removeUnknownModThrows() {
        Profile profile = profile("main", "fabric");
        createFile(profile.path().resolve("mods").resolve("other.jar"));
        modsByFileName.put("other.jar", List.of(mod("other", "Other")));

        RemoveCommand command = remove();
        command.setVersionArg(List.of("main"));
        command.setMod("sodium");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("Failed to find mod sodium"));
    }

    @Test
    void removeUnknownFileThrows() {
        profile("main", "fabric");

        RemoveCommand command = remove();
        command.setVersionArg(List.of("main"));
        command.setFile("other.jar");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(e.getMessage().contains("Failed to find mod file other.jar"));
    }

    @Test
    void removeWithExplicitTypeOnlySearchesThatDir() {
        Profile profile = profile("main", "fabric");
        Path mod = createFile(profile.path().resolve("mods").resolve("a.jar"));
        Path pack = createFile(profile.path().resolve("resourcepacks").resolve("a.jar"));
        modsByFileName.put("a.jar", List.of(mod("a", "A")));

        RemoveCommand command = remove();
        command.setVersionArg(List.of("main"));
        command.setType("resourcepack");
        command.setFile("a.jar");
        command.run();

        assertTrue(Files.exists(mod));
        assertFalse(Files.exists(pack));
    }

    @Test
    void removeWithMultipleMatchesThrows() {
        Profile profile = profile("main", "fabric");
        Path mod = createFile(profile.path().resolve("mods").resolve("a.jar"));
        Path pack = createFile(profile.path().resolve("resourcepacks").resolve("a.jar"));
        modsByFileName.put("a.jar", List.of(mod("a", "A")));

        RemoveCommand command = remove();
        command.setVersionArg(List.of("main"));
        command.setFile("a.jar");
        HeadlessMcException e = assertThrows(HeadlessMcException.class, command::run);
        assertTrue(e.getMessage().contains("Multiple files matched"));
        assertTrue(Files.exists(mod));
        assertTrue(Files.exists(pack));
    }

    @Test
    void removeWithMultipleMatchesAndForceDeletesAll() {
        Profile profile = profile("main", "fabric");
        Path mod = createFile(profile.path().resolve("mods").resolve("a.jar"));
        Path pack = createFile(profile.path().resolve("resourcepacks").resolve("a.jar"));
        modsByFileName.put("a.jar", List.of(mod("a", "A")));

        RemoveCommand command = remove();
        command.setVersionArg(List.of("main"));
        command.setFile("a.jar");
        command.setForce(true);
        command.run();

        assertFalse(Files.exists(mod));
        assertFalse(Files.exists(pack));
    }

    @Test
    void removeModFromUnsupportedPlatformThrows() {
        Profile profile = profile("plain", "vanilla");
        Path jar = createFile(profile.path().resolve("mods").resolve("a.jar"));

        RemoveCommand command = remove();
        command.setVersionArg(List.of("plain"));
        command.setFile("a.jar");
        assertThrows(IllegalArgumentException.class, command::run);
        assertTrue(Files.exists(jar));
    }

    @Test
    void removeUnreadableFileFromUnsupportedType() {
        Profile profile = profile("plain", "vanilla");
        Path pack = createFile(profile.path().resolve("resourcepacks").resolve("faithful.zip"));

        RemoveCommand command = remove();
        command.setVersionArg(List.of("plain"));
        command.setFile("faithful.zip");
        command.run();

        assertFalse(Files.exists(pack));
    }

    @Test
    void removeDatapackFromWorld() {
        Profile profile = profile("main", "fabric");
        Path pack = createFile(profile.path()
            .resolve(Worlds.SAVES).resolve("New World").resolve(Worlds.DATAPACKS).resolve("terralith.zip"));

        RemoveCommand command = remove();
        command.setVersionArg(List.of("main"));
        command.setType("datapack");
        command.setWorld("New World");
        command.setFile("terralith.zip");
        command.run();

        assertFalse(Files.exists(pack));
    }

    @Test
    void removeWithoutProfileRequiresAllEvenForASingleMatch() {
        Profile profile = profile("main", "fabric");
        profile("other", "fabric");
        Path jar = createFile(profile.path().resolve("mods").resolve("sodium.jar"));
        modsByFileName.put("sodium.jar", List.of(mod("sodium", "Sodium")));

        RemoveCommand command = remove();
        command.setMod("sodium");
        command.run();

        assertTrue(Files.exists(jar));
        assertTrue(console.output().contains("Found mod files across 1 profiles"));
    }

    @Test
    void removeWithoutProfileAndAllOnlyListsMatches() {
        Profile main = profile("main", "fabric");
        Profile other = profile("other", "fabric");
        Path mainJar = createFile(main.path().resolve("mods").resolve("sodium.jar"));
        Path otherJar = createFile(other.path().resolve("mods").resolve("sodium.jar"));
        modsByFileName.put("sodium.jar", List.of(mod("sodium", "Sodium")));

        RemoveCommand command = remove();
        command.setMod("sodium");
        command.run();

        assertTrue(Files.exists(mainJar));
        assertTrue(Files.exists(otherJar));
        assertTrue(console.output().contains("Found mod files across 2 profiles"));
        assertTrue(console.output().contains("main"));
        assertTrue(console.output().contains("other"));
    }

    @Test
    void removeWithoutProfileAndAllDeletesFromAllProfiles() {
        Profile main = profile("main", "fabric");
        Profile other = profile("other", "fabric");
        Path mainJar = createFile(main.path().resolve("mods").resolve("sodium.jar"));
        Path otherJar = createFile(other.path().resolve("mods").resolve("sodium.jar"));
        modsByFileName.put("sodium.jar", List.of(mod("sodium", "Sodium")));

        RemoveCommand command = remove();
        command.setMod("sodium");
        command.setAll(true);
        command.run();

        assertFalse(Files.exists(mainJar));
        assertFalse(Files.exists(otherJar));
    }

    @Test
    void removeWithoutProfileAndMultipleMatchesInOneProfileThrows() {
        Profile profile = profile("main", "fabric");
        createFile(profile.path().resolve("mods").resolve("a.jar"));
        createFile(profile.path().resolve("resourcepacks").resolve("a.jar"));
        modsByFileName.put("a.jar", List.of(mod("a", "A")));

        RemoveCommand command = remove();
        command.setFile("a.jar");
        command.setAll(true);
        HeadlessMcException e = assertThrows(HeadlessMcException.class, command::run);
        assertTrue(e.getMessage().contains("Multiple files matched for profile main"));
    }

    @Test
    void removeWithoutProfileAndNoMatchesThrows() {
        profile("main", "fabric");

        RemoveCommand command = remove();
        command.setMod("sodium");
        assertThrows(IllegalArgumentException.class, command::run);
    }

    // ---------------------------------------------------------------- search

    private SearchCommand search() {
        return new SearchCommand(distributionPlatformService, profileResolver, platformService, modTypeService, tables, console);
    }

    @Test
    void searchWithEmptyQueryThrows() {
        SearchCommand command = search();
        command.setQuery("");
        assertThrows(IllegalArgumentException.class, command::run);
    }

    @Test
    void searchWithoutProfileUsesModType() {
        searchResults.add(new RemoteMod("sodium", "Sodium", "Fast rendering"));

        SearchCommand command = search();
        command.setQuery("fast render");
        command.run();

        assertEquals(1, searches.size());
        Search search = searches.getFirst();
        assertEquals("fast render", search.query());
        assertEquals(Optional.empty(), search.version());
        assertEquals(Set.of(ModType.MOD), search.types());
        assertTrue(console.output().contains("Fast rendering"));
    }

    @Test
    void searchWithTypeButWithoutProfile() {
        SearchCommand command = search();
        command.setQuery("faithful");
        command.setType("resourcepack");
        command.run();

        assertEquals(Set.of(ModType.RESOURCE_PACK), searches.getFirst().types());
    }

    @Test
    void searchWithProfileScopesToItsVersion() {
        Profile profile = profile("main", "fabric");

        SearchCommand command = search();
        command.setQuery("sodium");
        command.setVersionArg(List.of("main"));
        command.run();

        Search search = searches.getFirst();
        assertEquals(Optional.of(profile.version()), search.version());
        assertEquals(Set.of(ModType.MOD, ModType.MOD_PACK), search.types());
    }

}
