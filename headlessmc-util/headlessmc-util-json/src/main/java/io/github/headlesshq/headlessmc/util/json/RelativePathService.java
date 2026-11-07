package io.github.headlesshq.headlessmc.util.json;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

/**
 * Converts {@link Path}s into {@link RelativePath}s and back,
 * using the best matching {@link RelativePathSource}.
 * The best match is the source with the deepest path that contains the given path,
 * e.g. a file in {@code .minecraft} is relative to {@code .minecraft}, not {@code user.home}.
 */
@Slf4j
@ApplicationScoped
public class RelativePathService {
    private final Iterable<RelativePathSource> sources;
    private final FileSystem fileSystem;

    @Inject
    public RelativePathService(Instance<RelativePathSource> sources) {
        this.sources = sources;
        this.fileSystem = FileSystems.getDefault();
    }

    public RelativePathService(Collection<? extends RelativePathSource> sources) {
        this(sources, FileSystems.getDefault());
    }

    /**
     * @param sources the sources to relativize against.
     * @param fileSystem the file system that paths without a known source are parsed with.
     */
    public RelativePathService(Collection<? extends RelativePathSource> sources, FileSystem fileSystem) {
        this.sources = List.copyOf(sources);
        this.fileSystem = fileSystem;
    }

    /**
     * @param path the path to convert.
     * @return a path relative to the best matching source, or the path itself if no source matches.
     */
    public RelativePath relativize(Path path) {
        if (!path.isAbsolute()) {
            // write relative paths with '/' too, a path written on Windows as "mods\\mod.jar"
            // would be a single file name on Linux. Drive relative paths like "C:mods" are kept as is.
            return new RelativePath(null, null, path.getRoot() == null ? join(path) : path.toString());
        }

        Path normalized = path.normalize();
        @Nullable RelativePathSource bestSource = null;
        @Nullable Path bestPath = null;
        for (RelativePathSource source : sources) {
            Path sourcePath = source.getPath().toAbsolutePath().normalize();
            if (normalized.startsWith(sourcePath) && isBetterMatch(source, sourcePath, bestSource, bestPath)) {
                bestSource = source;
                bestPath = sourcePath;
            }
        }

        if (bestSource == null || bestPath == null) {
            return new RelativePath(null, null, path.toString());
        }

        return new RelativePath(bestSource.getIdentifier(), bestPath.toString(), join(bestPath.relativize(normalized)));
    }

    /**
     * Resolves a {@link RelativePath} against the current location of its source.
     * If the source has moved since the path was serialized,
     * a warning is logged if the file still exists at the old location, but not at the new one.
     *
     * @param relativePath the path to resolve.
     * @return the resolved path.
     * @throws JsonParseException if the source is unknown and the path cannot be resolved,
     *                            if the path would escape its source, e.g. via {@code ..},
     *                            or if the path is not valid on this file system.
     */
    public Path resolve(RelativePath relativePath) throws JsonParseException {
        try {
            String identifier = relativePath.source();
            if (identifier == null) {
                return fileSystem.getPath(relativePath.path());
            }

            @Nullable Path serializedBase = parseAbsolute(relativePath.sourcePath());
            @Nullable RelativePathSource source = findSource(identifier);
            Path base;
            if (source != null) {
                base = source.getPath().toAbsolutePath().normalize();
            } else if (serializedBase != null) {
                log.warn("Unknown location '{}' for path '{}', falling back to '{}'",
                         identifier, relativePath.path(), serializedBase);
                base = serializedBase;
            } else if (relativePath.sourcePath() == null) {
                throw new JsonParseException("Unknown relative path source '" + identifier + "' for " + relativePath);
            } else {
                throw new JsonParseException("Unknown relative path source '" + identifier + "' for " + relativePath
                                                 + ", '" + relativePath.sourcePath() + "' is not an absolute path on"
                                                 + " this machine, was it written on another operating system?");
            }

            Path result = resolve(base, relativePath);
            if (source != null && relativePath.sourcePath() != null && !base.equals(serializedBase)) {
                warnIfLeftBehind(identifier, serializedBase, base, relativePath, result);
            }

            return result;
        } catch (InvalidPathException e) {
            throw new JsonParseException("Path " + relativePath + " is not valid on this machine: " + e.getMessage(), e);
        }
    }

    private Path resolve(Path base, RelativePath relativePath) throws JsonParseException {
        Path result = base;
        for (String name : relativePath.path().split(RelativePath.SEPARATOR)) {
            if (!name.isEmpty()) {
                result = result.resolve(name);
            }
        }

        result = result.normalize();
        if (!result.startsWith(base)) {
            throw new JsonParseException("Path " + relativePath + " escapes its location '" + base + "'");
        }

        return result;
    }

    /**
     * The source has moved, e.g. because HeadlessMc has been copied to another machine or the
     * {@code .minecraft} directory has been changed. We only warn if the file actually got left behind,
     * if it exists at the new location, or nowhere at all, everything is fine.
     */
    private void warnIfLeftBehind(
        String identifier,
        @Nullable Path oldBase,
        Path newBase,
        RelativePath relativePath,
        Path result
    ) {
        @Nullable Path old = null;
        try {
            old = oldBase == null ? null : resolve(oldBase, relativePath);
        } catch (JsonParseException ignored) {
            // cannot escape the old base if it does not escape the new one, but be safe
        }

        if (old != null && Files.exists(old) && !Files.exists(result)) {
            log.warn("Location '{}' has moved from '{}' to '{}', but '{}' only exists at the old location '{}'",
                     identifier, oldBase, newBase, relativePath.path(), old);
        } else {
            log.debug("Location '{}' of path '{}' has moved from '{}' to '{}'",
                      identifier, relativePath.path(), relativePath.sourcePath(), newBase);
        }
    }

    private @Nullable RelativePathSource findSource(String identifier) {
        for (RelativePathSource source : sources) {
            if (identifier.equals(source.getIdentifier())) {
                return source;
            }
        }

        return null;
    }

    /**
     * @return the given path if it is absolute on this file system, {@code null} otherwise,
     * e.g. if it has been written on another operating system.
     */
    private @Nullable Path parseAbsolute(@Nullable String path) {
        if (path == null) {
            return null;
        }

        try {
            Path result = fileSystem.getPath(path);
            return result.isAbsolute() ? result.normalize() : null;
        } catch (InvalidPathException e) {
            return null;
        }
    }

    private static String join(Path path) {
        StringJoiner joiner = new StringJoiner(RelativePath.SEPARATOR);
        for (Path name : path) {
            joiner.add(name.toString());
        }

        return joiner.toString();
    }

    private boolean isBetterMatch(
        RelativePathSource source,
        Path sourcePath,
        @Nullable RelativePathSource bestSource,
        @Nullable Path bestPath
    ) {
        if (bestSource == null || bestPath == null) {
            return true;
        }

        int comparison = Integer.compare(sourcePath.getNameCount(), bestPath.getNameCount());
        return comparison > 0 || comparison == 0 && source.getIdentifier().compareTo(bestSource.getIdentifier()) < 0;
    }

}
