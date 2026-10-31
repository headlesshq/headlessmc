package io.github.headlesshq.headlessmc.files;

import java.nio.file.Path;

/**
 * A .minecraft folder, holding versions, libraries and assets.
 *
 * @see McFilesService
 */
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

    /**
     * @param mcDir the .minecraft folder.
     * @return McFiles for the given .minecraft folder.
     */
    static McFiles of(Path mcDir) {
        return new DefaultMcFiles(mcDir);
    }

}
