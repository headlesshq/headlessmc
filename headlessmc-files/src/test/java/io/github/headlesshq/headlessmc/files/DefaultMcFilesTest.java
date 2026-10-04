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
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class DefaultMcFilesTest {
    private record TestFileConfig(Optional<String> mcDir) implements FileConfig {
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

    /** Just delegates {@link #bind} and {@link #getConfig()}, since that is all {@link DefaultMcFiles} uses. */
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

    private DefaultMcFiles mcFiles(Optional<String> mcDir, Optional<String> legacyMcDir, OS os) {
        Holder<FileConfig> holder = () -> new TestFileConfig(mcDir);
        Map<String, String> properties = legacyMcDir.map(value -> Map.of("hmc.mcdir", value)).orElse(Map.of());
        ConfigService configService = new PassthroughConfigService(config(properties));
        return new DefaultMcFiles(holder, configService, fileService, os);
    }

    @Test
    void usesTheConfiguredMcDir() {
        DefaultMcFiles mcFiles = mcFiles(Optional.of("/configured/mc"), Optional.empty(), linux);

        assertEquals(fileService.getUserPath("/configured/mc"), mcFiles.getMcDir());
    }

    @Test
    void fallsBackToTheLegacyMcDirProperty() {
        DefaultMcFiles mcFiles = mcFiles(Optional.empty(), Optional.of("/legacy/mc"), linux);

        assertEquals(fileService.getUserPath("/legacy/mc"), mcFiles.getMcDir());
    }

    @Test
    void conflictingMcDirAndLegacyMcDirThrow() {
        DefaultMcFiles mcFiles = mcFiles(Optional.of("/a"), Optional.of("/b"), linux);

        assertThrows(ConfigException.class, mcFiles::getMcDir);
    }

    @Test
    void fallsBackToTheUserHomeMinecraftDirOnLinux() {
        DefaultMcFiles mcFiles = mcFiles(Optional.empty(), Optional.empty(), linux);

        Path expected = fileService.getUserPath(System.getProperty("user.home")).resolve(".minecraft");
        assertEquals(expected, mcFiles.getMcDir());
    }

    @Test
    void fallsBackToTheApplicationSupportDirOnMacOs() {
        DefaultMcFiles mcFiles = mcFiles(Optional.empty(), Optional.empty(), macos);

        Path expected = fileService.getUserPath(System.getProperty("user.home"))
            .resolve("Library").resolve("Application Support").resolve("minecraft");
        assertEquals(expected, mcFiles.getMcDir());
    }

    @Test
    void testComputeMcDir() {
        mcFiles(Optional.empty(), Optional.empty(), macos).getMcDir();
    }

}
