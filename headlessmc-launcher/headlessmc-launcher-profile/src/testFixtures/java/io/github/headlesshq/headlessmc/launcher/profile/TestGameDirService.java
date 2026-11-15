package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.config.ConfigService;
import io.github.headlesshq.headlessmc.config.DynamicConfig;
import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.FileConfig;
import io.smallrye.config.Config;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.function.Supplier;

/**
 * The real {@link GameDirService} over a configuration that can be changed by tests.
 * By default it is configured like HeadlessMc is by default: the layout {@link GameDirLayout#HEADLESSMC}
 * with a game directory for each version.
 */
public final class TestGameDirService extends GameDirService {
    private final Settings settings;

    /**
     * @param appFiles the files of HeadlessMc, the game directories of servers are placed in them.
     */
    public TestGameDirService(AppFiles appFiles) {
        this(new Settings(), appFiles);
    }

    private TestGameDirService(Settings settings, AppFiles appFiles) {
        super(() -> settings, settings, new DefaultFileService(new DefaultFileSystemProvider()), appFiles);
        this.settings = settings;
    }

    /**
     * @param layout the value of {@code hmc.files.game-dir-layout}.
     * @return this service.
     */
    public TestGameDirService withLayout(GameDirLayout layout) {
        return withLayout(layout.getConfigName());
    }

    /**
     * @param layout the value of {@code hmc.files.game-dir-layout}, which may be invalid.
     * @return this service.
     */
    public TestGameDirService withLayout(String layout) {
        settings.layout = layout;
        return this;
    }

    /**
     * @param gameForEachVersion the value of {@code hmc.files.game-for-each-version}.
     * @return this service.
     */
    public TestGameDirService withGameForEachVersion(boolean gameForEachVersion) {
        settings.gameForEachVersion = gameForEachVersion;
        return this;
    }

    /**
     * @param gameDir the value of {@code hmc.files.game}, {@code null} to not configure a game directory.
     * @return this service.
     */
    public TestGameDirService withGameDir(@Nullable String gameDir) {
        settings.gameDir = gameDir;
        return this;
    }

    /**
     * @param gameDir the value of the legacy property {@code hmc.gamedir}, {@code null} to not set it.
     * @return this service.
     */
    public TestGameDirService withLegacyGameDir(@Nullable String gameDir) {
        settings.legacyGameDir = gameDir;
        return this;
    }

    /**
     * The configuration of the service: its {@link FileConfig}, and a {@link ConfigService} that only
     * provides {@link #getConfig()}, which is all the {@link GameDirService} uses.
     */
    private static final class Settings implements FileConfig, ConfigService {
        private String layout = GameDirLayout.HEADLESSMC.getConfigName();
        private boolean gameForEachVersion = true;
        private @Nullable String gameDir;
        private @Nullable String legacyGameDir;

        @Override
        public Optional<String> location() {
            return Optional.empty();
        }

        @Override
        public boolean gameForEachVersion() {
            return gameForEachVersion;
        }

        @Override
        public boolean mcDetect() {
            return false;
        }

        @Override
        public int mcDetectDepth() {
            return 3;
        }

        @Override
        public String gameDirLayout() {
            return layout;
        }

        @Override
        public Optional<String> mcDir() {
            return Optional.empty();
        }

        @Override
        public Optional<List<String>> mcDirs() {
            return Optional.empty();
        }

        @Override
        public Optional<String> gameDir() {
            return Optional.ofNullable(gameDir);
        }

        @Override
        public Config getConfig() {
            Map<String, String> properties = legacyGameDir == null ? Map.of() : Map.of("hmc.gamedir", legacyGameDir);
            return new SmallRyeConfigBuilder().withSources(new PropertiesConfigSource(properties, "test")).build();
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

}
