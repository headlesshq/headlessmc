package io.github.headlesshq.headlessmc.files;

import io.github.headlesshq.headlessmc.exceptions.FileException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DefaultFileService implements FileService {
    private final FileSystemProvider fs;

    @Override
    public FileSystemProvider fs() {
        return fs;
    }

    @Override
    public Path getUserPath(String... path) {
        FileSystem fileSystem = fs().getFileSystem();
        if (path.length == 0) {
            return fileSystem.getPath("");
        }

        Path result = fileSystem.getPath(path[0]);
        for (int i = 1; i < path.length; i++) {
            result = result.resolve(path[i]);
        }

        return result;
    }

    @Override
    public Path getPath(Path base, String... path) {
        if (path.length == 0) {
            return base;
        }

        Path result = base;
        for (String s : path) {
            result = result.resolve(s);
        }

        result = result.normalize();
        Path parent = base.toAbsolutePath().normalize();
        Path child = result.toAbsolutePath().normalize();
        if (!child.startsWith(parent) || child.equals(parent)) {
            throw new FileException("Path " + Arrays.toString(path) + " does not resolve to a child of " + base);
        }

        return result;
    }

    @Override
    public void delete(Path file) throws FileException {
        if (!Files.exists(file)) {
            return;
        }

        if (!Files.isDirectory(file)) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                throw new FileException("Failed to delete file " + file, e);
            }

            return;
        }

        try {
            Files.walkFileTree(file, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }

                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new FileException(e);
        }
    }

    // TODO
    @Override
    public void atomic(Path destination, FileConsumer ops) throws FileException {
        temp(tempPath -> {
            ops.accept(tempPath);
            delete(destination);
            try {
                Files.createDirectories(destination);
            } catch (IOException e) {
                throw new FileException("Failed to create directories " + destination, e);
            }

            saveMove(tempPath, destination);
        });
    }

    private Path getTempDir(String random) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void temp(FileConsumer action) throws FileException {
        UUID random = UUID.randomUUID();

        Path temp;
        if (fs().getFileSystem().equals(FileSystems.getDefault())) {
            try {
                temp = Files.createTempDirectory(random.toString());
            } catch (IOException e) {
                throw new FileException("Failed to create temp dir: " + random, e);
            }
        } else {
            temp = getTempDir(random.toString());
        }

        RuntimeException exception = null;
        try {
            try {
                Files.createDirectories(temp);
            } catch (IOException e) {
                throw new FileException("Failed to create directories " + temp, e);
            }

            action.accept(temp);
        } catch (HeadlessMcException | IOException e) {
            exception = new FileException(e);
        } catch (InterruptedException e) {
            exception = new UncheckedInterruptedException(e);
        } finally {
            try {
                delete(temp);
            } catch (FileException e) {
                if (exception == null) {
                    exception = e;
                } else {
                    exception.addSuppressed(e);
                }
            }
        }

        if (exception != null) {
            throw exception;
        }
    }

    @Override
    public void create(Path path, InputStream stream) throws FileException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new FileException("Failed to create directories: " + path, e);
            }
        }

        try (OutputStream os = Files.newOutputStream(path)) {
            stream.transferTo(os);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException deleteException) {
                e.addSuppressed(deleteException);
            }

            throw new FileException("Failed to write to " + path, e);
        }
    }

    @Override
    public void create(Path path, Consumer<OutputStream> action) throws FileException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new FileException("Failed to create directories: " + path, e);
            }
        }

        try (OutputStream os = Files.newOutputStream(path)) {
            action.accept(os);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException deleteException) {
                e.addSuppressed(deleteException);
            }

            throw new FileException(e);
        }
    }

    @Override
    public void saveMove(Path origin, Path destination) throws FileException {
        if (!Files.exists(origin)) {
            throw new FileException("Source does not exist: " + origin);
        }

        try {
            Path parent = destination.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try {
                Files.move(
                    origin,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                );
            } catch (FileSystemException e) {
                copyRecursively(origin, destination);
                deleteRecursively(origin);
            }
        } catch (IOException e) {
            throw new FileException("Failed to move " + origin + " to " + destination, e);
        }
    }

    private void copyRecursively(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(
                    file,
                    target.resolve(source.relativize(file)),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES
                );
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteRecursively(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public void extractResource(String resourceName, Path destination) throws FileException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = FileService.class.getClassLoader();
        }

        try (InputStream inputStream = classLoader.getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new FileException("Failed to find resource " + resourceName);
            }

            create(destination, inputStream);
        } catch (IOException e) {
            throw new FileException("Failed to read resource " + resourceName, e);
        }
    }

    @Override
    public void ensureFileExists(Path file, byte[] defaultContent) throws FileException {
        if (!Files.exists(file)) {
            create(file, new ByteArrayInputStream(defaultContent));
        }
    }

    @Override
    public void deleteFileAndEmptyParentDirs(Path file) throws FileException {
        delete(file);
        Path parent = file;
        while ((parent = parent.getParent()) != null) {
            if (isEmptyDirectory(parent)) {
                delete(parent);
            } else {
                return;
            }
        }
    }

    private boolean isEmptyDirectory(Path dir) {
        if (!Files.isDirectory(dir)) {
            return false;
        }

        try (Stream<Path> entries = Files.list(dir)) {
            return entries.findFirst().isEmpty();
        } catch (IOException e) {
            throw new FileException("Failed to check if " + dir + " is an empty directory", e);
        }
    }

}
