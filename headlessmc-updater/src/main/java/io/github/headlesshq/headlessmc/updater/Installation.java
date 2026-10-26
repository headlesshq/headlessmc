package io.github.headlesshq.headlessmc.updater;

import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
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
     * The jar Quarkus fast-jar builds, e.g. from {@code gradlew runApp}, are started with.
     * It is not the {@code headlessmc.jar} that is published to GitHub.
     */
    private static final String QUARKUS_RUN_JAR = "quarkus-run.jar";

    /**
     * Detects the installation of the running HeadlessMc.
     *
     * @return the installation of the running HeadlessMc.
     */
    public static Installation detect() {
        return detect(System::getProperty, ProcessHandle.current().info().command());
    }

    @VisibleForTesting
    static Installation detect(Function<String, @Nullable String> properties, Optional<String> command) {
        if ("runtime".equals(properties.apply(IMAGE_CODE_PROPERTY))) {
            return new Installation(Type.NATIVE, command.map(Installation::toPath).orElse(null));
        }

        // java -jar headlessmc.jar puts only the jar on the class path
        String classPath = properties.apply("java.class.path");
        String separator = Optional.ofNullable(properties.apply("path.separator")).orElse(":");
        if (classPath != null
            && !classPath.isBlank()
            && !classPath.contains(separator)
            && classPath.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            Path jar = toPath(classPath);
            if (!QUARKUS_RUN_JAR.equals(String.valueOf(jar.getFileName()))) {
                return new Installation(Type.JAR, jar);
            }
        }

        return new Installation(Type.UNKNOWN, null);
    }

    private static Path toPath(String path) {
        return Path.of(path).toAbsolutePath().normalize();
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
         * Neither, e.g. when running from an IDE.
         */
        UNKNOWN
    }

}
