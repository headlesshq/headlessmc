package io.github.headlesshq.headlessmc.files;

import io.github.headlesshq.headlessmc.config.ConfigException;
import io.github.headlesshq.headlessmc.config.ConfigService;
import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.os.OS;
import io.smallrye.config.Config;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class DefaultMcFilesServiceTest {
    /** A file config whose directories can be changed, like the real configuration at runtime. */
    private static final class TestFileConfig implements FileConfig {
        private Optional<String> mcDir = Optional.empty();
        private Optional<List<String>> mcDirs = Optional.empty();
        private boolean mcDetect;
        private int mcDetectDepth = 3;

        @Override
        public boolean mcDetect() {
            return mcDetect;
        }

        @Override
        public int mcDetectDepth() {
            return mcDetectDepth;
        }

        @Override
        public Optional<String> location() {
            return Optional.empty();
        }

        @Override
        public String gameDirLayout() {
            return "headlessmc";
        }

        @Override
        public boolean gameForEachVersion() {
            return true;
        }

        @Override
        public Optional<String> mcDir() {
            return mcDir;
        }

        @Override
        public Optional<List<String>> mcDirs() {
            return mcDirs;
        }

        @Override
        public Optional<String> gameDir() {
            return Optional.empty();
        }
    }

    /** Just delegates {@link #bind} and {@link #getConfig()}, since that is all the service uses. */
    private static final class PassthroughConfigService implements ConfigService {
        private final Config config;

        private PassthroughConfigService(Config config) {
            this.config = config;
        }

        @Override
        public Config getConfig() {
            return config;
        }

        @Override
        public boolean set(String name, String value, boolean persist) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ConfigService fork() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <C extends DynamicConfig> Holder<C> getHolder(Class<C> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <C extends DynamicConfig> Holder<C> getHolder(Class<C> type, String prefix) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <V> V bind(Object key, Supplier<V> mapping) {
            return mapping.get();
        }

        @Override
        public SortedSet<String> getPropertyNames() {
            throw new UnsupportedOperationException();
        }
    }

    @TempDir
    Path root;

    private final FileService fileService = new DefaultFileService(new DefaultFileSystemProvider());
    private final OS linux = new OS("linux", OS.Type.LINUX, "6.0");
    private final TestFileConfig config = new TestFileConfig();
    private int searches;

    private DefaultMcFilesService service() {
        return service(Map.of());
    }

    private DefaultMcFilesService service(Map<String, String> properties) {
        Config smallRye = new SmallRyeConfigBuilder()
            .withSources(new PropertiesConfigSource(properties, "test"))
            .build();
        // the drives of the test: everything in <root>/drives
        McDirDetector detector = new McDirDetector() {
            @Override
            public List<Path> getDrives() {
                searches++;
                try (java.util.stream.Stream<Path> drives = Files.list(Files.createDirectories(dir("drives")))) {
                    return drives.sorted().toList();
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            }
        };

        return new DefaultMcFilesService(() -> config, new PassthroughConfigService(smallRye), fileService, detector, linux);
    }

    /** Creates a {@code .minecraft} directory with a versions directory on a drive of the test. */
    private Path mcDirOnDrive(String... path) throws IOException {
        Path mcDir = dir("drives");
        for (String name : path) {
            mcDir = mcDir.resolve(name);
        }

        Files.createDirectories(mcDir.resolve("versions"));
        return mcDir;
    }

    private Path dir(String name) {
        return root.resolve(name);
    }

    private void configure(String primary, String... additional) {
        config.mcDir = Optional.of(dir(primary).toString());
        List<String> dirs = new ArrayList<>();
        for (String name : additional) {
            dirs.add(dir(name).toString());
        }

        config.mcDirs = Optional.of(dirs);
    }

    private static List<Path> paths(List<McFiles> mcFiles) {
        return mcFiles.stream().map(McFiles::getMcDir).toList();
    }

    // the directories

    @Test
    void withoutConfigurationThereIsOnlyTheDefaultDirectory() {
        DefaultMcFilesService service = service();

        Path expected = fileService.getUserPath(System.getProperty("user.home")).resolve(".minecraft");
        assertEquals(List.of(expected), paths(service.all()));
        assertEquals(expected, service.getPrimary().getMcDir());
    }

    @Test
    void theConfiguredDirectoryIsThePrimaryOne() {
        configure("mc");

        assertEquals(dir("mc"), service().getPrimary().getMcDir());
        assertEquals(List.of(dir("mc")), paths(service().all()));
    }

    @Test
    void theLegacyPropertyStillConfiguresThePrimaryDirectory() {
        DefaultMcFilesService service = service(Map.of("hmc.mcdir", dir("legacy").toString()));

        assertEquals(dir("legacy"), service.getPrimary().getMcDir());
    }

    @Test
    void configuringThePrimaryDirectoryTwiceThrows() {
        configure("mc");
        DefaultMcFilesService service = service(Map.of("hmc.mcdir", dir("legacy").toString()));

        assertThrows(ConfigException.class, service::getPrimary);
    }

    @Test
    void additionalDirectoriesFollowThePrimaryOneInTheirOrder() {
        configure("mc", "ssd", "hdd");

        assertEquals(List.of(dir("mc"), dir("ssd"), dir("hdd")), paths(service().all()));
        assertEquals(dir("mc"), service().getPrimary().getMcDir());
    }

    @Test
    void additionalDirectoriesDoNotReplaceTheDefaultDirectory() {
        config.mcDirs = Optional.of(List.of(dir("ssd").toString()));

        List<Path> all = paths(service().all());

        assertEquals(2, all.size());
        assertEquals(fileService.getUserPath(System.getProperty("user.home")).resolve(".minecraft"), all.getFirst());
        assertEquals(dir("ssd"), all.getLast());
    }

    @Test
    void duplicatesAndBlankEntriesAreIgnored() {
        config.mcDir = Optional.of(dir("mc").toString());
        config.mcDirs = Optional.of(List.of(
            dir("ssd").toString(),
            " ",
            // the primary directory, written differently
            dir("ssd").resolve("..").resolve("mc").toString(),
            "  " + dir("ssd") + "  ",
            dir("hdd").toString()
        ));

        assertEquals(List.of(dir("mc"), dir("ssd"), dir("hdd")), paths(service().all()));
    }

    @Test
    void nestedDirectoriesAreAllowed() {
        configure("mc", "mc/nested");

        assertEquals(List.of(dir("mc"), dir("mc/nested")), paths(service().all()));
    }

    @Test
    void changesOfTheConfigurationAreFollowed() {
        configure("mc");
        DefaultMcFilesService service = service();
        assertEquals(List.of(dir("mc")), paths(service.all()));

        configure("other", "ssd");

        assertEquals(List.of(dir("other"), dir("ssd")), paths(service.all()));
        assertEquals(dir("other"), service.getPrimary().getMcDir());
    }

    @Test
    void directoriesDoNotHaveToExist() throws IOException {
        configure("mc", "not-mounted");
        Files.createDirectories(dir("mc"));

        List<McFiles> all = service().all();

        // a directory that is configured but not there, e.g. on a disk that is not mounted, is still known
        assertEquals(2, all.size());
        assertTrue(all.getFirst().isAvailable());
        assertFalse(all.getLast().isAvailable());
    }

    // finding the directory of a file

    @Test
    void findsTheDirectoryContainingAFile() {
        configure("mc", "ssd");
        DefaultMcFilesService service = service();

        assertEquals(dir("mc"), service.find(dir("mc").resolve("versions").resolve("a").resolve("a.json")).orElseThrow().getMcDir());
        assertEquals(dir("ssd"), service.find(dir("ssd").resolve("libraries").resolve("x.jar")).orElseThrow().getMcDir());
        assertEquals(dir("ssd"), service.find(dir("ssd")).orElseThrow().getMcDir());
        assertEquals(Optional.empty(), service.find(dir("elsewhere").resolve("file")));
        // a directory that only starts with the same characters is another directory
        assertEquals(Optional.empty(), service.find(dir("mc-other").resolve("file")));
    }

    @Test
    void theInnermostOfNestedDirectoriesIsFound() {
        configure("mc", "mc/nested");
        DefaultMcFilesService service = service();

        assertEquals(dir("mc/nested"), service.find(dir("mc/nested/versions/a/a.json")).orElseThrow().getMcDir());
        assertEquals(dir("mc"), service.find(dir("mc/versions/a/a.json")).orElseThrow().getMcDir());
    }

    @Test
    void pathsAreComparedNormalized() {
        configure("mc");

        Path file = dir("other").resolve("..").resolve("mc").resolve("file");

        assertEquals(dir("mc"), service().find(file).orElseThrow().getMcDir());
    }

    @Test
    void ofReturnsTheKnownDirectoryOrANewOne() {
        configure("mc", "ssd");
        DefaultMcFilesService service = service();

        // also for a path that is written differently
        assertEquals(service.all().get(1), service.of(dir("mc").resolve("..").resolve("ssd")));
        assertEquals(dir("ssd"), service.of(dir("mc").resolve("..").resolve("ssd")).getMcDir());
        McFiles unknown = service.of(dir("custom"));
        assertEquals(dir("custom"), unknown.getMcDir());
        assertFalse(service.all().contains(unknown));
    }

    @Test
    void requireFindsTheDirectoryOfAVersionJson() {
        configure("mc", "ssd");
        DefaultMcFilesService service = service();

        // in a known directory
        assertEquals(dir("ssd"), service.require(dir("ssd/versions/a/a.json")).getMcDir());
        // outside of all known directories, but laid out like a .minecraft directory
        assertEquals(dir("custom"), service.require(dir("custom/versions/a/a.json")).getMcDir());
        // any other file belongs to the primary directory
        assertEquals(dir("mc"), service.require(dir("downloads/a.json")).getMcDir());
        assertEquals(dir("mc"), service.require(dir("custom/versions/a/other.json")).getMcDir());
        assertEquals(dir("mc"), service.require(dir("custom/not-versions/a/a.json")).getMcDir());
        // a version that does not come from a file
        assertEquals(dir("mc"), service.require(null).getMcDir());
    }

    // McFiles

    @Test
    void mcFilesAreEqualIfTheyPointToTheSameDirectory() {
        McFiles a = McFiles.of(dir("mc"));
        McFiles b = McFiles.of(dir("other").resolve("..").resolve("mc"));
        McFiles c = McFiles.of(dir("ssd"));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertTrue(a.isSame(b));
        assertNotEquals(a, c);
        assertFalse(a.isSame(c));
        // the path is kept as it has been given
        assertEquals(dir("other").resolve("..").resolve("mc"), b.getMcDir());
        assertEquals(dir("mc").toString(), a.toString());
    }

    @Test
    void mcFilesKnowTheirSubdirectories() {
        McFiles mcFiles = McFiles.of(dir("mc"));

        assertEquals(dir("mc/versions"), mcFiles.getVersionsDir());
        assertEquals(dir("mc/libraries"), mcFiles.getLibraryDir());
        assertEquals(dir("mc/assets"), mcFiles.getAssetsDir());
        assertEquals(dir("mc/resources"), mcFiles.getResourcesDir());
    }

    @Test
    void aFixedServiceKnowsItsDirectoriesWithoutDuplicates() {
        McFiles mc = McFiles.of(dir("mc"));
        McFiles ssd = McFiles.of(dir("ssd"));

        McFilesService service = McFilesService.of(mc, ssd, McFiles.of(dir("mc")), ssd);

        assertEquals(List.of(mc, ssd), service.all());
        assertSame(mc, service.getPrimary());
        assertSame(ssd, service.find(dir("ssd/file")).orElseThrow());
    }

    // hmc.files.mc-detect

    @Test
    void otherDrivesAreNotSearchedUnlessEnabled() throws IOException {
        configure("primary");
        mcDirOnDrive("ssd", ".minecraft");

        assertEquals(List.of(dir("primary")), service().all().stream().map(McFiles::getMcDir).toList());
        assertEquals(0, searches);
    }

    @Test
    void directoriesFoundOnOtherDrivesFollowTheConfiguredOnes() throws IOException {
        configure("primary", "configured");
        Path onSsd = mcDirOnDrive("ssd", ".minecraft");
        Path onHdd = mcDirOnDrive("hdd", "Games", "HMCL", ".minecraft");
        config.mcDetect = true;

        List<Path> dirs = service().all().stream().map(McFiles::getMcDir).toList();

        assertEquals(List.of(dir("primary"), dir("configured")), dirs.subList(0, 2));
        // the order of what has been found is the order of the file system
        assertEquals(java.util.stream.Stream.of(onHdd, onSsd).sorted().toList(), dirs.subList(2, dirs.size()));
        // the primary directory stays the one new versions are installed into
        assertEquals(dir("primary"), service().getPrimary().getMcDir());
    }

    @Test
    void directoriesThatAreAlreadyKnownAreNotAddedAgain() throws IOException {
        Path onSsd = mcDirOnDrive("ssd", ".minecraft");
        config.mcDir = Optional.of(onSsd.toString());
        config.mcDirs = Optional.of(List.of(mcDirOnDrive("hdd", ".minecraft").toString()));
        config.mcDetect = true;

        List<Path> dirs = service().all().stream().map(McFiles::getMcDir).toList();

        assertEquals(List.of(onSsd, dir("drives").resolve("hdd").resolve(".minecraft")), dirs);
    }

    @Test
    void theDepthOfTheSearchIsConfigurable() throws IOException {
        configure("primary");
        Path top = mcDirOnDrive("ssd", ".minecraft");
        Path deep = mcDirOnDrive("ssd", "a", "b", ".minecraft");
        config.mcDetect = true;

        config.mcDetectDepth = 1;
        assertEquals(List.of(dir("primary"), top), service().all().stream().map(McFiles::getMcDir).toList());
        config.mcDetectDepth = 3;
        assertEquals(List.of(dir("primary"), top, deep), service().all().stream().map(McFiles::getMcDir).toList());
        // nonsense is treated as the smallest depth
        config.mcDetectDepth = 0;
        assertEquals(List.of(dir("primary"), top), service().all().stream().map(McFiles::getMcDir).toList());
    }

}
