package io.github.headlesshq.headlessmc.launcher.files;

import lombok.Data;
import lombok.experimental.Delegate;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Data
public class FileManager implements Path {
    @Delegate
    private final Path base;

    public Path get(String... path) {
        Path result = base;
        for (String s : path) {
            result = result.resolve(s);
        }

        return result;
    }

    public Path file(String... path) {
        Path file = get(path);
        try {
            Files.createDirectories(file.getParent());
            Files.createFile(file);
        } catch (IOException e) {
            throw new IOError(e);
        }

        return file;
    }

    public Path dir(String... path) {
        Path directory = get(path);
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IOError(e);
        }

        return directory;
    }

    public Path forEach(Consumer<Path> action, String... path) {
        Path file = get(path);
        try (Stream<Path> stream = Files.walk(file)) {
            stream.forEach(action);
        } catch (IOException e) {
            throw new IOError(e);
        }

        return file;
    }

    public Path delete(String... path) {
        return forEach(p -> {
            try {
                Files.deleteIfExists(p);
            } catch (IOException e) {
                throw new IOError(e);
            }
        }, path);
    }

    public FileManager relative(String... path) {
        return new FileManager(get(path));
    }

    public static FileManager of(Path base) {
        return new FileManager(base);
    }

    public static FileManager mkdir(Path path) {
        FileManager manager = of(path);
        manager.dir();
        return manager;
    }

}
