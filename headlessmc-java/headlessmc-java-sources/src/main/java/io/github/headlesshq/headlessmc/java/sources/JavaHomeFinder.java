package io.github.headlesshq.headlessmc.java.sources;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.exceptions.FileException;
import io.github.headlesshq.headlessmc.java.JavaConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Finds the Java home inside a directory, e.g. one of the installation directories inside
 * {@code HeadlessMc/java} or a directory an archive was just extracted to.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class JavaHomeFinder {
    private final JavaExecutableFinder executableFinder;
    private final Holder<JavaConfig> config;

    /**
     * Searches the given directory and its subdirectories for a Java home.
     * The search is breadth-first, so the installation closest to {@code dir} wins.
     *
     * @param dir the directory to search in.
     * @return the (JAVA_HOME) directory of the installation found, if any.
     * @throws FileException if a directory could not be listed.
     */
    public Optional<Path> find(Path dir) throws FileException {
        return find(dir, config.get().maxScanDepth());
    }

    /**
     * Searches the given directory and its subdirectories for a Java home.
     * The search is breadth-first, so the installation closest to {@code dir} wins.
     *
     * @param dir      the directory to search in.
     * @param maxDepth how many directories to descend into, {@code 0} to only look at {@code dir} itself.
     * @return the (JAVA_HOME) directory of the installation found, if any.
     * @throws FileException if a directory could not be listed.
     */
    public Optional<Path> find(Path dir, int maxDepth) throws FileException {
        List<Path> currentDepth = List.of(dir);
        for (int depth = 0; depth <= maxDepth && !currentDepth.isEmpty(); depth++) {
            List<Path> nextDepth = new ArrayList<>();
            for (Path candidate : currentDepth) {
                if (findExecutable(candidate).isPresent()) {
                    return Optional.of(candidate);
                }

                if (depth < maxDepth) {
                    nextDepth.addAll(subDirsOf(candidate));
                }
            }

            currentDepth = nextDepth;
        }

        return Optional.empty();
    }

    /**
     * @param javaHome the (JAVA_HOME) directory of an installation.
     * @return the {@code bin/java} or {@code bin/java.exe} inside the given directory, if it exists.
     */
    public Optional<Path> findExecutable(Path javaHome) {
        Path executable = executableFinder.getExecutable(javaHome);
        if (Files.isRegularFile(executable)) {
            return Optional.of(executable);
        }

        return Optional.empty();
    }

    private List<Path> subDirsOf(Path dir) throws FileException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }

        try (Stream<Path> children = Files.list(dir)) {
            return children.filter(Files::isDirectory).sorted().toList();
        } catch (IOException e) {
            throw new FileException("Failed to list " + dir, e);
        }
    }

}
