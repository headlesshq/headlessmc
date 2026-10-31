package io.github.headlesshq.headlessmc.launcher.backup;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.files.FileService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Writes the files of a {@link BackupScope} into zip archives,
 * compares archives with the files on disk and restores them.
 * Paths inside an archive are relative to {@link BackupScope#source()} and use {@code /} as separator.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class BackupArchiveService {
    /**
     * Minecraft keeps this file locked while a world is open, it is never backed up.
     */
    public static final String SESSION_LOCK = "session.lock";

    private final FileService fileService;

    /**
     * The amount and size of the files written to an archive.
     *
     * @param files the amount of files.
     * @param size  the uncompressed size of the files in bytes.
     */
    public record ArchiveResult(int files, long size) {

    }

    /**
     * Writes the files of the given scope into a new zip archive.
     *
     * @param scope   the files to archive.
     * @param archive the archive to write.
     * @param skip    a directory to never archive, e.g. the backup directory.
     * @return the amount and size of the archived files.
     * @throws IOException if the files cannot be read or the archive cannot be written.
     */
    public ArchiveResult write(BackupScope scope, Path archive, Path skip) throws IOException {
        Path source = scope.source();
        List<Path> files = collect(scope, scope.paths(), skip);
        long size = 0;
        try (OutputStream os = Files.newOutputStream(archive);
             ZipOutputStream zip = new ZipOutputStream(os)) {
            for (Path file : files) {
                ZipEntry entry = new ZipEntry(relativize(source, file));
                entry.setLastModifiedTime(Files.getLastModifiedTime(file));
                zip.putNextEntry(entry);
                size += Files.copy(file, zip);
                zip.closeEntry();
            }
        }

        return new ArchiveResult(files.size(), size);
    }

    /**
     * @param scope the files to check.
     * @param skip  a directory to never archive, e.g. the backup directory.
     * @return {@code true} if any file of the given scope exists on disk.
     */
    public boolean hasFiles(BackupScope scope, Path skip) {
        try {
            return !collect(scope, scope.paths(), skip).isEmpty();
        } catch (IOException e) {
            throw new HeadlessMcIOException("Failed to list files in " + scope.source(), e);
        }
    }

    /**
     * Compares an archive with the files on disk.
     *
     * @param archive  the archive to compare.
     * @param scope    the scope the archive was created with.
     * @param paths    the paths, relative to the source of the scope, to compare.
     * @param checksum whether to compare the contents of files whose size and modification time are unchanged.
     * @param skip     a directory to never compare, e.g. the backup directory.
     * @return the difference between the archive and the files on disk.
     */
    public BackupDiff diff(Path archive, BackupScope scope, List<String> paths, boolean checksum, Path skip) {
        try (ZipFile zip = open(archive)) {
            return diff(zip, scope, paths, checksum, skip);
        } catch (IOException e) {
            throw new HeadlessMcIOException("Failed to compare " + archive + " with " + scope.source(), e);
        }
    }

    /**
     * Reverts the given changes, so that the files on disk match the archive again.
     *
     * @param archive the archive to restore files from.
     * @param scope   the scope the archive was created with.
     * @param changes the changes to revert, as returned by {@link #diff(Path, BackupScope, List, boolean, Path)}.
     *                Added files are deleted, removed and modified files are restored from the archive.
     */
    public void restore(Path archive, BackupScope scope, BackupDiff changes) {
        Path source = scope.source();
        try (ZipFile zip = open(archive)) {
            for (String name : changes.added()) {
                Path file = resolve(source, name);
                Files.deleteIfExists(file);
                deleteEmptyParents(file.getParent(), source);
            }

            List<String> restored = new ArrayList<>(changes.removed());
            restored.addAll(changes.modified());
            for (String name : restored) {
                ZipEntry entry = zip.getEntry(name);
                if (entry == null) {
                    throw new IOException("Archive " + archive + " does not contain " + name);
                }

                Path file = resolve(source, name);
                try (InputStream is = zip.getInputStream(entry)) {
                    fileService.create(file, is);
                }

                FileTime lastModified = entry.getLastModifiedTime();
                if (lastModified != null) {
                    Files.setLastModifiedTime(file, lastModified);
                }
            }
        } catch (IOException e) {
            throw new HeadlessMcIOException("Failed to restore " + archive + " to " + source, e);
        }
    }

    private BackupDiff diff(ZipFile zip, BackupScope scope, List<String> paths, boolean checksum, Path skip)
        throws IOException {
        Map<String, ZipEntry> entries = new HashMap<>();
        zip.stream()
            .filter(entry -> !entry.isDirectory())
            .forEach(entry -> entries.put(entry.getName(), entry));

        List<String> added = new ArrayList<>();
        List<String> modified = new ArrayList<>();
        Path source = scope.source();
        for (Path file : collect(scope, paths, skip)) {
            String name = relativize(source, file);
            ZipEntry entry = entries.remove(name);
            if (entry == null) {
                added.add(name);
            } else if (isModified(entry, file, checksum)) {
                modified.add(name);
            }
        }

        List<String> removed = new ArrayList<>(entries.keySet());
        Collections.sort(removed);
        return new BackupDiff(added, removed, modified);
    }

    private boolean isModified(ZipEntry entry, Path file, boolean checksum) throws IOException {
        if (entry.getSize() != Files.size(file)) {
            return true;
        }

        FileTime archived = entry.getLastModifiedTime();
        if (!checksum && archived != null
            && archived.toMillis() / 1000 == Files.getLastModifiedTime(file).toMillis() / 1000) {
            return false;
        }

        return entry.getCrc() != crc(file);
    }

    private List<Path> collect(BackupScope scope, List<String> paths, Path skip) throws IOException {
        Path source = scope.source();
        Path skipped = skip.toAbsolutePath().normalize();
        List<Pattern> ignored = compile(scope.ignored());
        SortedSet<Path> files = new TreeSet<>();
        for (String path : paths) {
            Path root = path.isEmpty() ? source : source.resolve(path);
            if (!Files.isDirectory(root)) {
                continue;
            }

            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.toAbsolutePath().normalize().startsWith(skipped)
                        || isIgnored(relativize(source, dir), ignored)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile()
                        && !SESSION_LOCK.equals(file.getFileName().toString())
                        && !isIgnored(relativize(source, file), ignored)) {
                        files.add(file);
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.warn("Skipping {}, failed to read it", file, exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return List.copyOf(files);
    }

    /**
     * A path is ignored if a pattern matches it or one of its parent directories.
     */
    private boolean isIgnored(String path, List<Pattern> ignored) {
        if (path.isEmpty() || ignored.isEmpty()) {
            return false;
        }

        for (int i = path.indexOf('/'); ; i = path.indexOf('/', i + 1)) {
            String prefix = i == -1 ? path : path.substring(0, i);
            if (ignored.stream().anyMatch(pattern -> pattern.matcher(prefix).matches())) {
                return true;
            }

            if (i == -1) {
                return false;
            }
        }
    }

    private List<Pattern> compile(List<String> globs) {
        return globs.stream()
            .map(String::trim)
            .filter(glob -> !glob.isEmpty())
            .map(this::compile)
            .toList();
    }

    private Pattern compile(String glob) {
        String normalized = glob.replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c == '*') {
                if (i + 1 < normalized.length() && normalized.charAt(i + 1) == '*') {
                    i++;
                    if (i + 1 < normalized.length() && normalized.charAt(i + 1) == '/') {
                        i++;
                        regex.append("(?:.*/)?");
                    } else {
                        regex.append(".*");
                    }
                } else {
                    regex.append("[^/]*");
                }
            } else if (c == '?') {
                regex.append("[^/]");
            } else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }

        return Pattern.compile(regex.toString());
    }

    private ZipFile open(Path archive) throws IOException {
        return new ZipFile(archive.toFile());
    }

    private Path resolve(Path source, String name) {
        return fileService.getPath(source, name.split("/"));
    }

    private void deleteEmptyParents(@Nullable Path dir, Path root) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        while (dir != null) {
            Path normalized = dir.toAbsolutePath().normalize();
            if (normalized.equals(normalizedRoot) || !normalized.startsWith(normalizedRoot)) {
                return;
            }

            try (Stream<Path> entries = Files.list(dir)) {
                if (entries.findFirst().isPresent()) {
                    return;
                }
            }

            Files.delete(dir);
            dir = dir.getParent();
        }
    }

    /**
     * @param source a directory.
     * @param file   a file inside the directory.
     * @return the path of the file relative to the directory, using {@code /} as separator.
     */
    public String relativize(Path source, Path file) {
        Path relative = source.relativize(file);
        StringJoiner joiner = new StringJoiner("/");
        for (Path name : relative) {
            joiner.add(name.toString());
        }

        return joiner.toString();
    }

    private long crc(Path file) throws IOException {
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[8192];
        try (InputStream is = Files.newInputStream(file)) {
            int read;
            while ((read = is.read(buffer)) != -1) {
                crc.update(buffer, 0, read);
            }
        }

        return crc.getValue();
    }

}
