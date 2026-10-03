package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.platform.server.ServerFinder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Slf4j
@Forge
@ApplicationScoped
@RequiredArgsConstructor
public class ForgeServerFinder implements ServerFinder {
    private final String platformName;
    private final OS os;

    @Inject
    public ForgeServerFinder(OS os) {
        this(Forge.PLATFORM_NAME, os);
    }

    @Override
    public Path findExecutable(Path serverDir) throws HeadlessMcException {
        Path runFile = isWindows() ? serverDir.resolve("run.bat") : serverDir.resolve("run.sh");
        if (Files.exists(runFile)) {
            return runFile;
        }

        List<Path> jarFiles;
        try (Stream<Path> files = Files.list(serverDir)) {
            jarFiles = files
                .filter(file -> file.toString().toLowerCase(Locale.ENGLISH).endsWith(".jar"))
                .toList();
        } catch (IOException e) {
            throw new HeadlessMcIOException("Failed to list files in " + serverDir, e);
        }

        if (jarFiles.isEmpty()) {
            throw new HeadlessMcIOException("Failed to find server scripts or jar files in " + serverDir);
        }

        if (jarFiles.size() == 1) {
            log.info("Only found one jar file in {}: {}", serverDir, jarFiles);
            return jarFiles.getFirst();
        }

        List<Path> forgeJars = jarFiles.stream()
            .filter(file -> file.toString().toLowerCase(Locale.ENGLISH).contains(platformName))
            .toList();

        if (forgeJars.size() == 1) {
            return forgeJars.getFirst();
        }

        log.warn("Found multiple forge jars: {}", forgeJars);
        List<Path> bestJars = new ArrayList<>(1);
        for (Path path : forgeJars.isEmpty() ? jarFiles : forgeJars) {
            // TODO: findExecutable should get reference to the expected VersionID?
        }

        return serverDir.resolve(DEFAULT_JAR);
    }

    @VisibleForTesting
    boolean isWindows() {
        return OS.Type.WINDOWS.equals(os.type());
    }

}
