package io.github.headlesshq.headlessmc.files;

import java.nio.file.Path;

public interface McFiles {
    Path getMcDir();

    default Path getVersionsDir() {
        return getMcDir().resolve("versions");
    }

    default Path getLibraryDir() {
        return getMcDir().resolve("libraries");
    }

    default Path getAssetsDir() {
        return getMcDir().resolve("assets");
    }

    default Path getResourcesDir() {
        return getMcDir().resolve("resources");
    }

}
