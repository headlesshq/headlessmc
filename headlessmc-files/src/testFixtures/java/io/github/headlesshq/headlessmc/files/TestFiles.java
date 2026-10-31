package io.github.headlesshq.headlessmc.files;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link McFiles}/{@link AppFiles} over a test root directory.
 */
public final class TestFiles {
    private TestFiles() {
    }

    public static McFiles mcFiles(Path root) {
        return McFiles.of(root.resolve("mc"));
    }

    /**
     * @param root the test root directory.
     * @return a {@link McFilesService} with only the {@link #mcFiles(Path)} of the root.
     */
    public static McFilesService mcFilesService(Path root) {
        return mcFilesService(mcFiles(root));
    }

    /**
     * @param primary    the primary .minecraft folder.
     * @param additional additional .minecraft folders.
     * @return a {@link McFilesService} for the given folders.
     */
    public static McFilesService mcFilesService(McFiles primary, McFiles... additional) {
        List<McFiles> all = new ArrayList<>();
        all.add(primary);
        all.addAll(Arrays.asList(additional));
        return new McFilesService() {
            @Override
            public McFiles getPrimaryMcFiles() {
                return primary;
            }

            @Override
            public List<McFiles> getAllMcFiles() {
                return List.copyOf(all);
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
