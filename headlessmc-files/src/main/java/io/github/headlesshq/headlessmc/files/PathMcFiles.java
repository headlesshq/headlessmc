package io.github.headlesshq.headlessmc.files;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * {@link McFiles} for a fixed directory, see {@link McFiles#of(Path)}.
 * The path is kept as it was given, but two instances are equal if they point to the same directory.
 */
final class PathMcFiles implements McFiles {
    private final Path mcDir;
    private final Path normalized;

    PathMcFiles(Path mcDir) {
        this.mcDir = mcDir;
        this.normalized = McFiles.normalize(mcDir);
    }

    @Override
    public Path getMcDir() {
        return mcDir;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof PathMcFiles other && normalized.equals(other.normalized);
    }

    @Override
    public int hashCode() {
        return normalized.hashCode();
    }

    @Override
    public String toString() {
        return mcDir.toString();
    }

}
