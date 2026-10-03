package io.github.headlesshq.headlessmc.commands.mod;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Helper for locating and listing the worlds of a {@link Profile}.
 *
 * <p>For {@code CLIENT} profiles worlds live inside the {@code saves} directory of the
 * game directory, for {@code SERVER} profiles each world is a directory in the profile
 * root (identified by containing a {@code datapacks} folder), with {@code world} being
 * the default.
 */
@UtilityClass
final class Worlds {
    static final String SAVES = "saves";
    static final String DATAPACKS = "datapacks";
    static final String DEFAULT_SERVER_WORLD = "world";

    /**
     * Lists the worlds of the given profile.
     *
     * @param profile the profile to list the worlds of.
     * @return the world directories of the profile.
     */
    static List<Path> listWorlds(Profile profile) {
        if (profile.side().isClient()) {
            return listDirectories(profile.path().resolve(SAVES));
        }

        return listDirectories(profile.path()).stream()
            .filter(dir -> Files.isDirectory(dir.resolve(DATAPACKS)))
            .toList();
    }

    /**
     * Resolves the world directory of the given profile.
     *
     * <p>If no world is specified, the single world of a {@code CLIENT} profile or the
     * default {@code world} of a {@code SERVER} profile is used.
     *
     * @param profile the profile to resolve the world of.
     * @param world   the name of the world, or {@code null} to use the default.
     * @return the resolved world directory.
     * @throws IllegalArgumentException if the world cannot be resolved.
     */
    static Path resolveWorld(Profile profile, @Nullable String world) {
        if (profile.side().isClient()) {
            Path saves = profile.path().resolve(SAVES);
            if (world != null) {
                return requireDirectory(saves.resolve(world), profile, world);
            }

            List<Path> worlds = listDirectories(saves);
            if (worlds.size() == 1) {
                return worlds.getFirst();
            }

            throw new IllegalArgumentException(
                "Please specify a world with --world, found " + worlds.size() + " worlds in " + saves
            );
        }

        if (world != null) {
            return requireDirectory(profile.path().resolve(world), profile, world);
        }

        Path defaultWorld = profile.path().resolve(DEFAULT_SERVER_WORLD);
        if (Files.isDirectory(defaultWorld)) {
            return defaultWorld;
        }

        throw new IllegalArgumentException(
            "Please specify a world with --world, no default '" + DEFAULT_SERVER_WORLD
                + "' world found in " + profile.path()
        );
    }

    private static Path requireDirectory(Path dir, Profile profile, String world) {
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException(
                "Failed to find world '" + world + "' for profile " + profile.name() + " at " + dir
            );
        }

        return dir;
    }

    private static List<Path> listDirectories(Path dir) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(Files::isDirectory).sorted().toList();
        } catch (IOException e) {
            throw new HeadlessMcIOException("Failed to list directory " + dir, e);
        }
    }

}
