package io.github.headlesshq.headlessmc.launcher.files;

import io.github.headlesshq.headlessmc.os.OS;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;

public class MinecraftFinder {
    public Path mkdir(OS os) {
        try {
            Path path = provide(os);
            Files.createDirectories(path);
            return path;
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private Path provide(OS os) {
        switch (os.getType()) {
            case WINDOWS:
                String appData = requireNonNull(System.getenv("APPDATA"), "APPDATA ENV variable missing");
                return Paths.get(appData).resolve(".minecraft");
            case LINUX:
                String linuxUser = requireNonNull(System.getProperty("user.home"), "user.home not set");
                return Paths.get(linuxUser).resolve(".minecraft");
            case OSX:
                String macUser = requireNonNull(System.getProperty("user.home"), "user.home not set");
                return Paths.get(macUser).resolve("Library").resolve("Application Support").resolve("minecraft");
            default:
                throw new IllegalStateException(os.getType().toString());
        }
    }

}
