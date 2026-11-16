package io.github.headlesshq.headlessmc.files;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Finds {@code .minecraft} directories on the drives of this computer, for {@code hmc.files.mc-detect}.
 *
 * <p>A drive is searched for directories called {@value #MC_DIR} that contain a {@code versions} directory,
 * down to a small depth: a {@code .minecraft} directory on another drive is usually at its top, e.g.
 * {@code D:\.minecraft}, or next to a launcher, e.g. {@code D:\Games\HMCL\.minecraft}.
 * This only lists directories, nothing is read or changed. It is bounded in depth and in the number of
 * directories it looks at, a drive is never searched completely.</p>
 *
 * <p>The drives: on Windows every drive letter, elsewhere what is mounted at the usual places
 * {@code /mnt}, {@code /media}, {@code /run/media} and {@code /Volumes}. The file system root of other
 * systems is not searched, the {@code .minecraft} directory in the home directory is the default one anyway.</p>
 */
@Slf4j
@ApplicationScoped
public class McDirDetector {
    /**
     * The name of the directories that are searched for.
     */
    public static final String MC_DIR = ".minecraft";
    private static final String VERSIONS = "versions";
    /** How many directories are listed on one drive at most. */
    private static final int MAX_DIRECTORIES = 20_000;
    /** Directories in which nobody keeps a game, but which are large. */
    private static final Set<String> IGNORED = Set.of(
        "windows", "program files", "program files (x86)", "programdata", "system volume information",
        "lost+found", "node_modules", "proc", "sys", "dev"
    );

    /**
     * Searches the drives of this computer.
     *
     * @param depth how deep to search a drive, 1 only finds a {@code .minecraft} directory at its top.
     * @return the {@code .minecraft} directories that have been found, sorted.
     */
    public List<Path> detect(int depth) {
        return detect(getDrives(), depth);
    }

    /**
     * Searches the given directories.
     *
     * @param drives the directories to search, e.g. the roots of drives.
     * @param depth  how deep to search each of them, 1 only finds a {@code .minecraft} directory directly in them.
     * @return the {@code .minecraft} directories that have been found, sorted.
     */
    public List<Path> detect(Collection<Path> drives, int depth) {
        Set<Path> result = new LinkedHashSet<>();
        if (depth < 1) {
            return List.of();
        }

        for (Path drive : drives) {
            search(drive, depth, result);
        }

        List<Path> sorted = result.stream().sorted().toList();
        log.debug("Detected Minecraft directories {} on {}", sorted, drives);
        return sorted;
    }

    /**
     * @return the directories other drives are found at, see {@link McDirDetector}.
     */
    public List<Path> getDrives() {
        List<Path> result = new ArrayList<>();
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            // "/" is not a drive, on Windows every root is one
            if (root.toString().length() > 1) {
                result.add(root);
            }
        }

        addChildren(Path.of("/mnt"), 1, result);
        // these are /media/<label>, but also /media/<user>/<label>, both are searched
        addChildren(Path.of("/media"), 2, result);
        addChildren(Path.of("/run/media"), 2, result);
        addChildren(Path.of("/Volumes"), 1, result);
        return result;
    }

    private void addChildren(Path parent, int levels, List<Path> result) {
        if (!parent.isAbsolute() || !Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }

        for (Path child : list(parent)) {
            result.add(child);
            if (levels > 1) {
                addChildren(child, levels - 1, result);
            }
        }
    }

    private void search(Path drive, int depth, Set<Path> result) {
        record Entry(Path dir, int depth) {
        }

        Deque<Entry> queue = new ArrayDeque<>();
        queue.add(new Entry(drive, 0));
        int listed = 0;
        while (!queue.isEmpty() && listed++ < MAX_DIRECTORIES) {
            Entry entry = queue.poll();
            for (Path child : list(entry.dir())) {
                String name = String.valueOf(child.getFileName());
                if (MC_DIR.equalsIgnoreCase(name)) {
                    if (Files.isDirectory(child.resolve(VERSIONS))) {
                        result.add(McFiles.normalize(child));
                    }

                    // nothing is searched for inside of a .minecraft directory
                    continue;
                }

                if (entry.depth() + 1 < depth && !isIgnored(name)) {
                    queue.add(new Entry(child, entry.depth() + 1));
                }
            }
        }

        if (!queue.isEmpty()) {
            log.debug("Stopped searching {} for Minecraft directories after {} directories", drive, MAX_DIRECTORIES);
        }
    }

    private static boolean isIgnored(String name) {
        return name.startsWith("$") || IGNORED.contains(name.toLowerCase(Locale.ROOT));
    }

    /**
     * @return the directories in the given directory, without following links. Empty if it cannot be listed,
     * e.g. because it is a drive without a medium or a directory of another user.
     */
    private List<Path> list(Path dir) {
        List<Path> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path child : stream) {
                if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                    result.add(child);
                }
            }
        } catch (IOException | RuntimeException e) {
            log.debug("Cannot list {}: {}", dir, e.toString());
        }

        result.sort(null);
        return result;
    }

}
