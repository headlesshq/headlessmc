package io.github.headlesshq.headlessmc.java;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

@EqualsAndHashCode
@RequiredArgsConstructor
public final class SafePath {
    private final @Nullable Path path;

    public Optional<Path> get(FileSystem fileSystem) {
        if (path == null || !fileSystem.equals(path.getFileSystem())) {
            return Optional.empty();
        }

        return Optional.of(path);
    }

    public Optional<FileSystem> getFilesystem() {
        return Optional.ofNullable(path).map(Path::getFileSystem);
    }

    public Optional<Path> getUnchecked() {
        return Optional.ofNullable(path);
    }

    public SafePath map(Function<Path, Path> mapping) {
        if (path == null) {
            return new SafePath(null);
        }

        return new SafePath(mapping.apply(this.path));
    }

    public SafePath resolve(String child) {
        return path == null ? new SafePath(null) : new SafePath(path.resolve(child));
    }

    @Override
    public String toString() {
        return path == null ? "null" : path.toString();
    }

}
