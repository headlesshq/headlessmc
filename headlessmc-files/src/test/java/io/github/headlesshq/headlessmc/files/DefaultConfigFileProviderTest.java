package io.github.headlesshq.headlessmc.files;

import io.github.headlesshq.headlessmc.config.ConfigFileProvider;
import io.github.headlesshq.headlessmc.config.ConfigServiceImpl;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class DefaultConfigFileProviderTest {
    @Inject
    ConfigFileProvider configFileProvider;

    @Inject
    AppFiles appFiles;

    @Inject
    ConfigServiceImpl configService;

    @Test
    void configFileLivesInTheConfigDir() {
        assertEquals(appFiles.getConfigDir().resolve("config.properties"), configFileProvider.getConfigFile());
    }

    @Test
    void configFileIsInTheConfiguredLocation(@TempDir Path dir) {
        Path configFile = new DefaultConfigFileProvider(new AppFiles() {
            @Override
            public Path getConfigDir() {
                return dir;
            }

            @Override
            public Path getDataDir() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Path getCacheDir() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Path getStateDir() {
                throw new UnsupportedOperationException();
            }
        }).getConfigFile();

        assertEquals(dir.resolve("config.properties"), configFile);
    }

    @Test
    void configServiceLoadsTheConfigFileOnStartup() {
        assertEquals(Optional.of(configFileProvider.getConfigFile()), configService.getConfigFile());
    }

}
