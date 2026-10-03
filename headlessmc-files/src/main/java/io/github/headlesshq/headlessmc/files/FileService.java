package io.github.headlesshq.headlessmc.files;

import io.github.headlesshq.headlessmc.exceptions.FileException;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.function.Consumer;

public interface FileService {
    FileSystemProvider fs();

    Path getUserPath(String... path);

    Path getPath(Path base, String... path);

    void delete(Path file) throws FileException;

    void atomic(Path destination, FileConsumer ops) throws FileException;

    void temp(FileConsumer temp) throws FileException;

    void create(Path path, InputStream stream) throws FileException;

    void create(Path path, Consumer<OutputStream> action) throws FileException;

    void saveMove(Path origin, Path destination) throws FileException;

    void extractResource(String resourceName, Path destination) throws FileException;

    void ensureFileExists(Path file, byte[] defaultContent) throws FileException;

    void deleteFileAndEmptyParentDirs(Path file) throws FileException;

}
