package io.github.headlesshq.headlessmc.launcher.files;

import lombok.Data;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Data
public class FileManager {
    private final Path base;

    public Path get(String... path) {
        Path result = base;
        for (String s : path) {
            result = result.resolve(s);
        }

        return result;
    }

    public Path createFile(String... path) {
        Path file = get(path);
        try {
            Files.createDirectories(file.getParent());
            Files.createFile(file);
        } catch (IOException e) {
            throw new IOError(e);
        }

        return file;
    }

    public Path createDirectory(String... path) {
        Path directory = get(path);
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IOError(e);
        }

        return directory;
    }

    public Iterable<Path> list(String... path) {
        Path file = get(path);
        return null; // TODO
    }

    public void delete(String... path) {
        return; // TODO
    }

    public FileManager relative(String... path) {
        return new FileManager(get(path));
    }

    public static FileManager mkdir(Path path) {
        FileManager manager = new FileManager(path);
        manager.createDirectory();
        return manager;
    }

}
