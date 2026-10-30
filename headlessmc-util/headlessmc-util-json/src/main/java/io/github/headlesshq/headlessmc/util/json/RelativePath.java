package io.github.headlesshq.headlessmc.util.json;

import org.jspecify.annotations.Nullable;

/**
 * The serialized form of a {@link java.nio.file.Path}.
 *
 * @param source the identifier of the {@link RelativePathSource} the path is relative to,
 *               or {@code null} if the path is not relative to any source.
 * @param sourcePath the absolute path the source pointed to when this path was serialized,
 *                   or {@code null} if there is no source.
 * @param path the path relative to the source, with {@code /} as separator,
 *             or the path itself if there is no source.
 */
public record RelativePath(@Nullable String source, @Nullable String sourcePath, String path) {
    public static final String SEPARATOR = "/";

}
