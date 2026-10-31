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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class DefaultMcFilesServiceTest {
    private record TestFileConfig(Optional<String> mcDir, Optional<List<String>> mcFolders) implements FileConfig {
        @Override
        public Optional<String> location() {
            return Optional.empty();
        }

        @Override
        public boolean gameForEachVersion() {
            return true;
        }

        @Override
        public Optional<String> gameDir() {
            return Optional.empty();
        }
    }

    /** Just delegates {@link #bind} and {@link #getConfig()}, since that is all {@link DefaultMcFilesService} uses. */
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

    private final FileService fileService = new DefaultFileService(new DefaultFileSystemProvider());
    private final OS linux = new OS("linux", OS.Type.LINUX, "6.0");
    private final OS macos = new OS("macos", OS.Type.MACOS, "15");

    private static Config config(Map<String, String> properties) {
        return new SmallRyeConfigBuilder()
            .withSources(new PropertiesConfigSource(properties, "test"))
            .build();
    }

    private DefaultMcFilesService mcFiles(Optional<String> mcDir, Optional<String> legacyMcDir, OS os) {
        return mcFiles(mcDir, legacyMcDir, Optional.empty(), os);
    }

    private DefaultMcFilesService mcFiles(
        Optional<String> mcDir,
        Optional<String> legacyMcDir,
        Optional<List<String>> mcFolders,
        OS os
    ) {
        Holder<FileConfig> holder = () -> new TestFileConfig(mcDir, mcFolders);
        Map<String, String> properties = legacyMcDir.map(value -> Map.of("hmc.mcdir", value)).orElse(Map.of());
        ConfigService configService = new PassthroughConfigService(config(properties));
        return new DefaultMcFilesService(holder, configService, fileService, os);
    }

    @Test
    void usesTheConfiguredMcDir() {
        DefaultMcFilesService mcFiles = mcFiles(Optional.of("/configured/mc"), Optional.empty(), linux);

        assertEquals(fileService.getUserPath("/configured/mc"), mcFiles.getPrimaryMcFiles().getMcDir());
    }

    @Test
    void fallsBackToTheLegacyMcDirProperty() {
        DefaultMcFilesService mcFiles = mcFiles(Optional.empty(), Optional.of("/legacy/mc"), linux);

        assertEquals(fileService.getUserPath("/legacy/mc"), mcFiles.getPrimaryMcFiles().getMcDir());
    }

    @Test
    void conflictingMcDirAndLegacyMcDirThrow() {
        DefaultMcFilesService mcFiles = mcFiles(Optional.of("/a"), Optional.of("/b"), linux);

        assertThrows(ConfigException.class, mcFiles::getPrimaryMcFiles);
    }

    @Test
    void fallsBackToTheUserHomeMinecraftDirOnLinux() {
        DefaultMcFilesService mcFiles = mcFiles(Optional.empty(), Optional.empty(), linux);

        Path expected = fileService.getUserPath(System.getProperty("user.home")).resolve(".minecraft");
        assertEquals(expected, mcFiles.getPrimaryMcFiles().getMcDir());
    }

    @Test
    void fallsBackToTheApplicationSupportDirOnMacOs() {
        DefaultMcFilesService mcFiles = mcFiles(Optional.empty(), Optional.empty(), macos);

        Path expected = fileService.getUserPath(System.getProperty("user.home"))
            .resolve("Library").resolve("Application Support").resolve("minecraft");
        assertEquals(expected, mcFiles.getPrimaryMcFiles().getMcDir());
    }

    @Test
    void testComputeMcDir() {
        mcFiles(Optional.empty(), Optional.empty(), macos).getPrimaryMcFiles();
    }

    @Test
    void withoutAdditionalFoldersThereIsOnlyThePrimaryFolder() {
        DefaultMcFilesService mcFiles = mcFiles(Optional.of("/primary"), Optional.empty(), linux);

        assertEquals(List.of(McFiles.of(fileService.getUserPath("/primary"))), mcFiles.getAllMcFiles());
    }

    @Test
    void additionalFoldersFollowThePrimaryFolderInOrder() {
        DefaultMcFilesService mcFiles = mcFiles(
            Optional.of("/primary"),
            Optional.empty(),
            Optional.of(List.of("/b", " /a ", "", "/primary", "/b")),
            linux
        );

        assertEquals(
            List.of(
                McFiles.of(fileService.getUserPath("/primary")),
                McFiles.of(fileService.getUserPath("/b")),
                McFiles.of(fileService.getUserPath("/a"))
            ),
            mcFiles.getAllMcFiles()
        );
        assertEquals(McFiles.of(fileService.getUserPath("/primary")), mcFiles.getPrimaryMcFiles());
    }

    @Test
    void findsTheInnermostFolderContainingAPath() {
        DefaultMcFilesService mcFiles = mcFiles(
            Optional.of("/primary"),
            Optional.empty(),
            Optional.of(List.of("/primary/nested", "/other")),
            linux
        );

        Path primary = fileService.getUserPath("/primary");
        assertEquals(Optional.of(McFiles.of(primary)), mcFiles.getMcFiles(primary.resolve("versions")));
        assertEquals(
            Optional.of(McFiles.of(primary.resolve("nested"))),
            mcFiles.getMcFiles(primary.resolve("nested").resolve("versions"))
        );
        assertEquals(Optional.empty(), mcFiles.getMcFiles(fileService.getUserPath("/elsewhere")));
    }

}
