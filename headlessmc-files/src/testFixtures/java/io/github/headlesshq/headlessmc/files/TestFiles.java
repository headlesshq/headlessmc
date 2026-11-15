package io.github.headlesshq.headlessmc.files;

import java.nio.file.Path;

/**
 * {@link McFiles}/{@link McFilesService}/{@link AppFiles} over a test root directory.
 */
public final class TestFiles {
    private TestFiles() {
    }

    /**
     * @param root the test root directory.
     * @return {@link McFiles} for {@code <root>/mc}.
     */
    public static McFiles mcFiles(Path root) {
        return McFiles.of(root.resolve("mc"));
    }

    /**
     * @param root the test root directory.
     * @return a {@link McFilesService} whose only directory is {@link #mcFiles(Path)}.
     */
    public static McFilesService mcFilesService(Path root) {
        return McFilesService.of(mcFiles(root));
    }

    /**
     * @param primary    the primary directory.
     * @param additional additional directories.
     * @return a {@link McFilesService} for the given directories.
     */
    public static McFilesService mcFilesService(McFiles primary, McFiles... additional) {
        return McFilesService.of(primary, additional);
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
