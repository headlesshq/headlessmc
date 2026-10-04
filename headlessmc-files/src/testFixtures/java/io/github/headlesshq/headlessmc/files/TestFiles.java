package io.github.headlesshq.headlessmc.files;

import java.nio.file.Path;

/**
 * {@link McFiles}/{@link AppFiles} over a test root directory.
 */
public final class TestFiles {
    private TestFiles() {
    }

    public static McFiles mcFiles(Path root) {
        return new McFiles() {
            @Override
            public Path getMcDir() {
                return root.resolve("mc");
            }

            @Override
            public Path getVersionsDir() {
                return getMcDir().resolve("versions");
            }

            @Override
            public Path getLibraryDir() {
                return getMcDir().resolve("libraries");
            }

            @Override
            public Path getAssetsDir() {
                return getMcDir().resolve("assets");
            }

            @Override
            public Path getResourcesDir() {
                return getMcDir().resolve("resources");
            }
        };
    }

    public static AppFiles appFiles(Path root) {
        return new AppFiles() {
            @Override
            public Path getConfigDir() {
                return root.resolve("headlessmc");
            }

            @Override
            public Path getDataDir() {
                return root.resolve("headlessmc");
            }

            @Override
            public Path getCacheDir() {
                return root.resolve("headlessmc").resolve("cache");
            }

            @Override
            public Path getStateDir() {
                return root.resolve("headlessmc").resolve("state");
            }
        };
    }

}
