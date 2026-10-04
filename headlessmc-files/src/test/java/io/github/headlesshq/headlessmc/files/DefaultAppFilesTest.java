package io.github.headlesshq.headlessmc.files;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DefaultAppFilesTest {
    private record TestFileConfig(Optional<String> location) implements FileConfig {

        @Override
        public boolean gameForEachVersion() {
            return true;
        }

        @Override
        public Optional<String> mcDir() {
            return Optional.empty();
        }

        @Override
        public Optional<String> gameDir() {
            return Optional.empty();
        }
    }

    private final FileService fileService = new DefaultFileService(new DefaultFileSystemProvider());
    private final XdgBaseDirectories xdg = new XdgBaseDirectories(
        Path.of("/data"), Path.of("/config"), Path.of("/state"), Path.of("/cache")
    );

    private DefaultAppFiles appFiles(Optional<String> location) {
        return new DefaultAppFiles(() -> xdg, () -> new TestFileConfig(location), fileService);
    }

    @Test
    void usesTheConfiguredLocationInsteadOfXdgDirectoriesWhenSet() {
        AppFiles files = appFiles(Optional.of("headlessmc"));

        assertEquals(fileService.getUserPath("headlessmc"), files.getConfigDir());
        assertEquals(fileService.getUserPath("headlessmc"), files.getDataDir());
        assertEquals(fileService.getUserPath("headlessmc", "cache"), files.getCacheDir());
        assertEquals(fileService.getUserPath("headlessmc"), files.getStateDir());
        assertEquals(fileService.getUserPath("headlessmc", "logs"), files.getLogDir());
    }

    @Test
    void usesTheXdgDirectoriesWhenNoLocationIsSet() {
        AppFiles files = appFiles(Optional.empty());

        assertEquals(Path.of("/config/headlessmc"), files.getConfigDir());
        assertEquals(Path.of("/data/headlessmc"), files.getDataDir());
        assertEquals(Path.of("/cache/headlessmc"), files.getCacheDir());
        assertEquals(Path.of("/state/headlessmc"), files.getStateDir());
        assertEquals(Path.of("/state/headlessmc/logs"), files.getLogDir());
    }

}
