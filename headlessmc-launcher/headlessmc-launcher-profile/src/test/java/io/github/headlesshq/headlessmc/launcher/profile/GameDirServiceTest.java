package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.config.ConfigException;
import io.github.headlesshq.headlessmc.config.ConfigService;
import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.FileConfig;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
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

class GameDirServiceTest {
    private record TestFileConfig(boolean gameForEachVersion, Optional<String> gameDir) implements FileConfig {
        @Override
        public Optional<String> location() {
            return Optional.empty();
        }

        @Override
        public Optional<String> mcDir() {
            return Optional.empty();
        }
    }

    /** Just delegates {@link #getConfig()}, since that is all {@link GameDirService} uses. */
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
    private final Path root = fileService.getUserPath("/root");
    private final McFiles mcFiles = () -> root.resolve("mc");
    private final AppFiles appFiles = new AppFiles() {
        @Override
        public Path getConfigDir() {
            return root.resolve("config");
        }

        @Override
        public Path getDataDir() {
            return root.resolve("data");
        }

        @Override
        public Path getCacheDir() {
            return root.resolve("cache");
        }

        @Override
        public Path getStateDir() {
            return root.resolve("state");
        }
    };

    private static Config config(Map<String, String> properties) {
        return new SmallRyeConfigBuilder()
            .withSources(new PropertiesConfigSource(properties, "test"))
            .build();
    }

    private GameDirService service(
        boolean gameForEachVersion,
        Optional<String> gameDir,
        Optional<String> legacyGameDir
    ) {
        Holder<FileConfig> holder = () -> new TestFileConfig(gameForEachVersion, gameDir);
        Map<String, String> properties = legacyGameDir.map(value -> Map.of("hmc.gamedir", value)).orElse(Map.of());
        ConfigService configService = new PassthroughConfigService(config(properties));
        return new GameDirService(holder, configService, fileService, appFiles, mcFiles);
    }

    private VersionArg version(Side side) {
        return new VersionArg(Optional.of(side), "vanilla", "1.21.1", Optional.empty());
    }

    @Test
    void serverVersionsGetAPerVersionServerDir() {
        GameDirService service = service(true, Optional.empty(), Optional.empty());
        VersionArg version = version(Side.SERVER);

        assertEquals(
            appFiles.getServerDir().resolve("server-" + version.toString("-")),
            service.getGameDir(version)
        );
    }

    @Test
    void clientVersionsGetAPerVersionMcDirWhenEnabled() {
        GameDirService service = service(true, Optional.empty(), Optional.empty());
        VersionArg version = version(Side.CLIENT);

        assertEquals(mcFiles.getMcDir().resolve(version.toString("-")), service.getGameDir(version));
    }

    @Test
    void clientVersionsShareTheMcDirWhenPerVersionIsDisabled() {
        GameDirService service = service(false, Optional.empty(), Optional.empty());

        assertEquals(mcFiles.getMcDir(), service.getGameDir(version(Side.CLIENT)));
    }

    @Test
    void usesTheConfiguredGameDir() {
        GameDirService service = service(true, Optional.of("/configured/game"), Optional.empty());

        assertEquals(fileService.getUserPath("/configured/game"), service.getGameDir(version(Side.CLIENT)));
    }

    @Test
    void fallsBackToTheLegacyGameDirProperty() {
        GameDirService service = service(true, Optional.empty(), Optional.of("/legacy/game"));

        assertEquals(fileService.getUserPath("/legacy/game"), service.getGameDir(version(Side.CLIENT)));
    }

    @Test
    void conflictingGameDirAndLegacyGameDirThrow() {
        GameDirService service = service(true, Optional.of("/a"), Optional.of("/b"));

        assertThrows(ConfigException.class, () -> service.getGameDir(version(Side.CLIENT)));
    }

}
