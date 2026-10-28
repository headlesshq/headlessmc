package io.github.headlesshq.headlessmc.util.json;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

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

    @Inject
    public RelativePathService(Instance<RelativePathSource> sources) {
        this.sources = sources;
    }

    public RelativePathService(Collection<? extends RelativePathSource> sources) {
        this.sources = List.copyOf(sources);
    }

    /**
     * @param path the path to convert.
     * @return a path relative to the best matching source, or the path itself if no source matches.
     */
    public RelativePath relativize(Path path) {
        if (!path.isAbsolute()) {
            return new RelativePath(null, null, path.toString());
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

        StringJoiner relative = new StringJoiner(RelativePath.SEPARATOR);
        for (Path name : bestPath.relativize(normalized)) {
            relative.add(name.toString());
        }

        return new RelativePath(bestSource.getIdentifier(), bestPath.toString(), relative.toString());
    }

    /**
     * Resolves a {@link RelativePath} against the current location of its source.
     * Logs a warning if the source has moved since the path was serialized.
     *
     * @param relativePath the path to resolve.
     * @return the resolved path.
     * @throws JsonParseException if the source is unknown and the path cannot be resolved.
     */
    public Path resolve(RelativePath relativePath) throws JsonParseException {
        String identifier = relativePath.source();
        if (identifier == null) {
            return Path.of(relativePath.path());
        }

        Path base = findSourcePath(identifier, relativePath);
        for (String name : relativePath.path().split(RelativePath.SEPARATOR)) {
            if (!name.isEmpty()) {
                base = base.resolve(name);
            }
        }

        return base;
    }

    private Path findSourcePath(String identifier, RelativePath relativePath) throws JsonParseException {
        String serializedSourcePath = relativePath.sourcePath();
        for (RelativePathSource source : sources) {
            if (identifier.equals(source.getIdentifier())) {
                Path current = source.getPath().toAbsolutePath().normalize();
                if (serializedSourcePath != null && !current.toString().equals(serializedSourcePath)) {
                    log.warn("Location '{}' of path '{}' has changed from '{}' to '{}'",
                             identifier, relativePath.path(), serializedSourcePath, current);
                }

                return current;
            }
        }

        if (serializedSourcePath == null) {
            throw new JsonParseException("Unknown relative path source '" + identifier + "' for " + relativePath);
        }

        log.warn("Unknown location '{}' for path '{}', falling back to '{}'",
                 identifier, relativePath.path(), serializedSourcePath);
        return Path.of(serializedSourcePath);
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
