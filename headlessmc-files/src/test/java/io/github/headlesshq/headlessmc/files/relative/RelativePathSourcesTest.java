package io.github.headlesshq.headlessmc.files.relative;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.util.json.RelativePath;
import io.github.headlesshq.headlessmc.util.json.RelativePathService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelativePathSourcesTest {
    @TempDir
    Path root;

    private AppFiles appFiles() {
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
    void sourcesPointToTheirLocations() {
        AppFiles appFiles = appFiles();
        Path mcDir = root.resolve(".minecraft");

        assertEquals(appFiles.getConfigDir(), new AppFilesConfigPathSource(appFiles).getPath());
        assertEquals(appFiles.getDataDir(), new AppFilesDataPathSource(appFiles).getPath());
        assertEquals(appFiles.getCacheDir(), new AppFilesCachePathSource(appFiles).getPath());
        assertEquals(appFiles.getStateDir(), new AppFilesStatePathSource(appFiles).getPath());
        assertEquals(mcDir, new McFilesPathSource(() -> mcDir).getPath());
        assertEquals(Path.of(System.getProperty("user.home")),
                     new UserHomePathSource(new DefaultFileService(new DefaultFileSystemProvider())).getPath());
    }

    @Test
    void appFilesAreRelativeToTheirMostSpecificDirectory() {
        AppFiles appFiles = appFiles();
        RelativePathService service = new RelativePathService(List.of(
            new AppFilesConfigPathSource(appFiles),
            new AppFilesDataPathSource(appFiles),
            new AppFilesCachePathSource(appFiles),
            new AppFilesStatePathSource(appFiles),
            new McFilesPathSource(() -> root.resolve(".minecraft"))
        ));

        assertEquals(new RelativePath(AppFilesDataPathSource.IDENTIFIER, appFiles.getDataDir().toString(), "java/jdk-21"),
                     service.relativize(appFiles.getJavaDir().resolve("jdk-21")));
        assertEquals(new RelativePath(McFilesPathSource.IDENTIFIER, root.resolve(".minecraft").toString(), "mods"),
                     service.relativize(root.resolve(".minecraft").resolve("mods")));
    }

}
