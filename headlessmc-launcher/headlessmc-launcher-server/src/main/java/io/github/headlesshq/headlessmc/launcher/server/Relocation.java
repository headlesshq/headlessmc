package io.github.headlesshq.headlessmc.launcher.server;

import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * Helps launch targets that remember files inside the game directory of a profile to follow a move
 * of that directory, see {@link io.github.headlesshq.headlessmc.launcher.profile.LaunchTarget#relocate}.
 */
@UtilityClass
class Relocation {
    /**
     * @param file    a file a launch target remembers, may be {@code null}.
     * @param oldPath the old game directory.
     * @param newPath the new game directory.
     * @return the file in the new game directory if it has been in the old one, otherwise the file itself.
     */
    static @Nullable Path relocate(@Nullable Path file, Path oldPath, Path newPath) {
        if (file == null) {
            return null;
        }

        Path normalized = file.toAbsolutePath().normalize();
        Path oldDir = oldPath.toAbsolutePath().normalize();
        if (!normalized.startsWith(oldDir)) {
            return file;
        }

        return newPath.toAbsolutePath().normalize().resolve(oldDir.relativize(normalized).toString());
    }

}
