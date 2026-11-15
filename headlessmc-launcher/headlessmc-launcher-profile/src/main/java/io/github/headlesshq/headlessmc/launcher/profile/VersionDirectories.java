package io.github.headlesshq.headlessmc.launcher.profile;

import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Tells the files of an installed version apart from everything else in its directory.
 * A version is installed into {@code versions/<id>/}, as {@code <id>.json} and {@code <id>.jar}.
 * With the game directory layouts of other launchers, see {@link GameDirLayout}, that directory is also a
 * game directory, with worlds, mods and settings next to the version.
 * Whoever moves a game directory or removes a version has to leave the other part where it is.
 */
@UtilityClass
public class VersionDirectories {
    private static final String VERSIONS = "versions";
    private static final String JSON = ".json";
    private static final String JAR = ".jar";
    private static final String NATIVES = "natives";

    /**
     * @param dir a directory.
     * @return {@code true} if a version is installed in the directory: it is {@code versions/<id>}
     * and contains {@code <id>.json}.
     */
    public static boolean isVersionDirectory(Path dir) {
        Path normalized = dir.toAbsolutePath().normalize();
        @Nullable Path name = normalized.getFileName();
        @Nullable Path parent = normalized.getParent();
        return name != null
            && parent != null
            && parent.getFileName() != null
            && VERSIONS.equals(parent.getFileName().toString())
            && Files.isRegularFile(normalized.resolve(name + JSON));
    }

    /**
     * Checks if an entry of a version directory belongs to the installed version: its version.json, its jar,
     * and the directories launchers extract its natives into, which can be created again at any time.
     * Everything else belongs to a game played in the directory.
     *
     * @param versionDir a {@link #isVersionDirectory(Path) version directory}.
     * @param entry      a file or directory directly in the version directory.
     * @return {@code true} if the entry belongs to the installed version.
     */
    public static boolean isVersionFile(Path versionDir, Path entry) {
        String id = String.valueOf(versionDir.toAbsolutePath().normalize().getFileName());
        String name = String.valueOf(entry.getFileName());
        return name.equals(id + JSON)
            || name.equals(id + JAR)
            || name.toLowerCase(Locale.ROOT).contains(NATIVES) && Files.isDirectory(entry);
    }

    /**
     * @param versionDir a {@link #isVersionDirectory(Path) version directory}.
     * @return the entries of the directory that do not {@link #isVersionFile(Path, Path) belong to the version},
     * the files of a game that is played in the directory. Empty if the directory only holds the version.
     * @throws IOException if the directory cannot be listed.
     */
    public static List<Path> getGameFiles(Path versionDir) throws IOException {
        try (Stream<Path> entries = Files.list(versionDir)) {
            return entries.filter(entry -> !isVersionFile(versionDir, entry)).sorted().toList();
        }
    }

    /**
     * @param versionDir a {@link #isVersionDirectory(Path) version directory}.
     * @return the entries of the directory that {@link #isVersionFile(Path, Path) belong to the version}.
     * @throws IOException if the directory cannot be listed.
     */
    public static List<Path> getVersionFiles(Path versionDir) throws IOException {
        try (Stream<Path> entries = Files.list(versionDir)) {
            return entries.filter(entry -> isVersionFile(versionDir, entry)).sorted().toList();
        }
    }

}
