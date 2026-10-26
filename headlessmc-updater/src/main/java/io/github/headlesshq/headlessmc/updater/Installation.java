package io.github.headlesshq.headlessmc.updater;

import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/**
 * Describes how the running HeadlessMc has been installed.
 *
 * @param type the type of the installation.
 * @param path the file that has been executed, the native executable or the jar,
 *             or {@code null} if it could not be found.
 */
public record Installation(Type type, @Nullable Path path) {
    /**
     * Set to {@code runtime} by GraalVM inside a native image.
     */
    private static final String IMAGE_CODE_PROPERTY = "org.graalvm.nativeimage.imagecode";

    /**
     * Detects the installation of the running HeadlessMc.
     *
     * @return the installation of the running HeadlessMc.
     */
    public static Installation detect() {
        return detect(System::getProperty, ProcessHandle.current().info().command(), getCodeSource());
    }

    /**
     * Detects the installation from the given process information.
     *
     * @param properties the system properties of the process.
     * @param command    the executable of the process.
     * @param codeSource the jar or directory the classes of HeadlessMc have been loaded from.
     * @return the detected installation.
     */
    @VisibleForTesting
    static Installation detect(Function<String, @Nullable String> properties, Optional<String> command, Optional<Path> codeSource) {
        if ("runtime".equals(properties.apply(IMAGE_CODE_PROPERTY))) {
            return new Installation(Type.NATIVE, command.map(Installation::toPath).orElse(null));
        }

        // java -jar headlessmc.jar puts only the jar on the class path.
        // It is only the uber jar if HeadlessMc itself has been loaded from it, the quarkus-run.jar
        // of a fast-jar build is on the class path, too, but only references the jars in its lib directory.
        String classPath = properties.apply("java.class.path");
        String separator = Optional.ofNullable(properties.apply("path.separator")).orElse(":");
        if (classPath != null
            && !classPath.isBlank()
            && !classPath.contains(separator)
            && classPath.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            Path jar = toPath(classPath);
            if (codeSource.map(Installation::normalize).filter(jar::equals).isPresent()) {
                return new Installation(Type.JAR, jar);
            }
        }

        return new Installation(Type.UNKNOWN, null);
    }

    private static Path toPath(String path) {
        return normalize(Path.of(path));
    }

    private static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

    private static Optional<Path> getCodeSource() {
        try {
            CodeSource codeSource = Installation.class.getProtectionDomain().getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                return Optional.empty();
            }

            return Optional.of(Path.of(codeSource.getLocation().toURI()));
        } catch (URISyntaxException | IllegalArgumentException | FileSystemNotFoundException | SecurityException e) {
            return Optional.empty();
        }
    }

    /**
     * The type of installation.
     */
    public enum Type {
        /**
         * A native executable built with GraalVM.
         */
        NATIVE,
        /**
         * The {@code headlessmc.jar}, started with {@code java -jar}.
         */
        JAR,
        /**
         * Neither, e.g. when running from an IDE, or from the {@code quarkus-run.jar} of a fast-jar build,
         * which cannot be replaced by the published {@code headlessmc.jar}.
         */
        UNKNOWN
    }

}
