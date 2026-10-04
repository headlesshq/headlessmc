package io.github.headlesshq.headlessmc.java.sources;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaSource;
import io.github.headlesshq.headlessmc.java.SafePath;
import io.github.headlesshq.headlessmc.java.version.JavaScanner;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@ApplicationScoped
public class JavaScannerService {
    private final Instance<JavaScanner> scanners;
    private final JavaExecutableFinder executableFinder;
    private final JavaHomeFinder javaHomeFinder;

    @Inject
    public JavaScannerService(
        @Any Instance<JavaScanner> scanners,
        JavaExecutableFinder executableFinder,
        JavaHomeFinder javaHomeFinder
    ) {
        this.scanners = scanners;
        this.executableFinder = executableFinder;
        this.javaHomeFinder = javaHomeFinder;
    }

    public Optional<Java> scanDir(JavaSource source, Path dir) {
        return scanDir(source, dir, 0);
    }

    /**
     * @param source   the source the returned {@link Java} belongs to.
     * @param dir      the directory containing the installation.
     * @param maxDepth how deep to search for the java home inside {@code dir},
     *                 see {@link JavaHomeFinder#find(Path, int)}.
     * @return the installation found in {@code dir}, if any.
     */
    public Optional<Java> scanDir(JavaSource source, Path dir, int maxDepth) {
        Optional<Path> javaHome = javaHomeFinder.find(dir, maxDepth);
        if (javaHome.isEmpty()) {
            return Optional.empty();
        }

        Path executable = executableFinder.getExecutable(javaHome.get());
        HeadlessMcException exception = new HeadlessMcIOException("Failed to scan java dir " + dir);
        int version = scanners.stream().map(scanner -> {
                try {
                    return Optional.of(scanner.scan(executable));
                } catch (HeadlessMcException e) {
                    exception.addSuppressed(e);
                    return Optional.<Integer>empty();
                }
            }).flatMap(Optional::stream)
            .findFirst()
            .orElseThrow(() -> exception);

        return Optional.of(new Java(
            dir.getFileName().toString(),
            version,
            new SafePath(javaHome.get()),
            new SafePath(executable),
            false,
            source.sort()
        ));
    }

    public List<Java> scanDirs(JavaSource source, Stream<Path> dirs) {
        return scanDirs(source, dirs, 0);
    }

    public List<Java> scanDirs(JavaSource source, Stream<Path> dirs, int maxDepth) {
        List<Path> paths = dirs.toList();
        List<Java> result = new ArrayList<>(paths.size());
        for (Path path : paths) {
            if (Files.isDirectory(path)) {
                try {
                    scanDir(source, path, maxDepth).ifPresent(result::add);
                } catch (HeadlessMcException e) {
                    log.error("Failed to scan java dir {}", path, e);
                }
            }
        }

        return result;
    }

    public List<Java> scanSubDirsOf(JavaSource source, Path javaDir) {
        return scanSubDirsOf(source, javaDir, 0);
    }

    public List<Java> scanSubDirsOf(JavaSource source, Path javaDir, int maxDepth) {
        try {
            if (Files.isDirectory(javaDir)) {
                try (Stream<Path> files = Files.list(javaDir)) {
                    return scanDirs(source, files, maxDepth);
                }
            }
        } catch (IOException e) {
            log.error("Failed to get Java versions from {}", javaDir, e);
        }

        return List.of();
    }

}
