package io.github.headlesshq.headlessmc.files;

import java.nio.file.Path;

/**
 * {@link McFiles} for a fixed .minecraft folder.
 *
 * @param mcDir the .minecraft folder.
 */
record DefaultMcFiles(Path mcDir) implements McFiles {
    @Override
    public Path getMcDir() {
        return mcDir;
    }

}
