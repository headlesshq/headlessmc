package io.github.headlesshq.headlessmc.files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppFilesTest {
    private AppFiles appFiles(Path root) {
        return new AppFiles() {
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
    }

    @Test
    void authJavaPluginAndServerDirsLiveUnderTheDataDir(@TempDir Path root) {
        AppFiles files = appFiles(root);
        Path data = root.resolve("data");

        assertEquals(data.resolve(".auth"), files.getAuthDir());
        assertEquals(data.resolve("java"), files.getJavaDir());
        assertEquals(data.resolve("plugins"), files.getPluginDir());
        assertEquals(data.resolve("server"), files.getServerDir());
    }

    @Test
    void logsDirLivesUnderTheStateDir(@TempDir Path root) {
        AppFiles files = appFiles(root);

        assertEquals(root.resolve("state").resolve("logs"), files.getLogDir());
    }

    @Test
    void profilesDirLivesUnderTheConfigDir(@TempDir Path root) {
        AppFiles files = appFiles(root);

        assertEquals(root.resolve("config").resolve("profiles"), files.getProfilesDir());
    }

    @Test
    void nativesDirLivesUnderTheCacheDir(@TempDir Path root) {
        AppFiles files = appFiles(root);

        assertEquals(root.resolve("cache").resolve("natives"), files.getNatives());
    }

}
