package io.github.headlesshq.headlessmc.launcher.profile;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Completes arguments that are paths to files, for {@link LaunchTarget#complete(TargetArgs)}.
 */
@Slf4j
@UtilityClass
public class PathCandidates {
    private static final int LIMIT = 200;

    /**
     * Lists the entries of the directory the given, partially typed path points into.
     * Directories are always listed, so that the user can navigate into them.
     *
     * @param typed  the path typed so far, e.g. {@code mods/fab}.
     * @param filter which files to list.
     * @return the directories and matching files, written the way they continue what has been typed.
     */
    public static List<ArgCandidate> complete(String typed, Predicate<Path> filter) {
        try {
            int separator = Math.max(typed.lastIndexOf('/'), typed.lastIndexOf('\\'));
            String prefix = separator < 0 ? "" : typed.substring(0, separator + 1);
            Path dir = prefix.isEmpty() ? Path.of("") : Path.of(prefix);
            if (!Files.isDirectory(dir.toAbsolutePath())) {
                return List.of();
            }

            List<ArgCandidate> result = new ArrayList<>();
            try (Stream<Path> files = Files.list(dir.toAbsolutePath())) {
                files.sorted().limit(LIMIT).forEach(file -> {
                    String name = file.getFileName().toString();
                    if (Files.isDirectory(file)) {
                        result.add(new ArgCandidate(prefix + name + "/"));
                    } else if (filter.test(file)) {
                        result.add(new ArgCandidate(prefix + name));
                    }
                });
            }

            return result;
        } catch (IOException | InvalidPathException e) {
            log.debug("Failed to complete path {}", typed, e);
            return List.of();
        }
    }

    /**
     * @param extensions file extensions including the dot, e.g. {@code .jar}.
     * @return a filter for files with one of the extensions.
     */
    public static Predicate<Path> extension(String... extensions) {
        return file -> hasExtension(file, extensions);
    }

    /**
     * @param file       the file to check.
     * @param extensions file extensions including the dot, e.g. {@code .jar}.
     * @return {@code true} if the name of the file ends with one of the extensions, ignoring case.
     */
    public static boolean hasExtension(Path file, String... extensions) {
        Path fileName = file.getFileName();
        if (fileName == null) {
            return false;
        }

        String name = fileName.toString().toLowerCase(Locale.ENGLISH);
        for (String extension : extensions) {
            if (name.endsWith(extension)) {
                return true;
            }
        }

        return false;
    }

}
