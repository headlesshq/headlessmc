package io.github.headlesshq.headlessmc.patcher;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.McFiles;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

final class TestPatchers {
    private TestPatchers() {
    }

    static Patcher patcher(String name, long version) {
        return new Patcher() {
            @Override
            public void patch(PatchContext context) {
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public long version() {
                return version;
            }
        };
    }

    static AppFiles appFiles(Path root) {
        return new AppFiles() {
            @Override
            public Path getConfigDir() {
                return root.resolve("hmc");
            }

            @Override
            public Path getDataDir() {
                return root.resolve("hmc");
            }

            @Override
            public Path getCacheDir() {
                return root.resolve("hmc").resolve("cache");
            }

            @Override
            public Path getStateDir() {
                return root.resolve("hmc").resolve("state");
            }
        };
    }

    static McFiles mcFiles(Path root) {
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

    static Path writeJar(Path file, String entryName, String content) throws IOException {
        Files.createDirectories(file.getParent());
        try (OutputStream out = Files.newOutputStream(file);
             JarOutputStream jar = new JarOutputStream(out)) {
            jar.putNextEntry(new JarEntry(entryName));
            jar.write(content.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }

        return file;
    }

}
