package io.github.headlesshq.headlessmc.plugins;

import io.github.headlesshq.headlessmc.HeadlessMc;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.net.hash.HashService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Remembers which build and which Java to launch for a set of plugins, so that HeadlessMc does not have to
 * hash the plugins, resolve them and search for Java installations on every start.
 * <p>
 * Like Gradle's file snapshots or git's index, the {@link #fingerprint(List, Path)} of the inputs only uses
 * file metadata (size, modification time and file key), not the content. If the metadata of any input changes,
 * the fingerprint changes and the slow path hashes the files again, which then decides whether a rebuild is needed.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PluginLaunchCache {
    private static final String FILE = "launch.properties";

    private final HashService hashService;
    private final AppFiles appFiles;

    /**
     * @param plugins    the plugin jars.
     * @param mutableJar the configured mutable jar (directory or zip), or {@code null} if the lock is used.
     * @return a fingerprint of the given inputs and the running HeadlessMc version.
     */
    public String fingerprint(List<Path> plugins, @Nullable Path mutableJar) {
        StringBuilder input = new StringBuilder("headlessmc=").append(HeadlessMc.VERSION).append('\n');
        if (mutableJar == null) {
            input.append("mutable=").append(MutableJarLock.load().map(MutableJarLock::sha256).orElse("none")).append('\n');
        } else if (Files.isDirectory(mutableJar)) {
            input.append("mutable=").append(stat(mutableJar.resolve("quarkus").resolve("quarkus-application.dat"))).append('\n');
            input.append("mutable=").append(stat(mutableJar.resolve("lib").resolve("deployment").resolve("appmodel.dat"))).append('\n');
        } else {
            input.append("mutable=").append(stat(mutableJar)).append('\n');
        }

        for (Path plugin : plugins) {
            input.append("plugin=").append(stat(plugin)).append('\n');
        }

        byte[] digest = hashService.getAlgorithm(HashService.SHA256).digest(input.toString().getBytes(StandardCharsets.UTF_8));
        return hashService.toHexString(digest);
    }

    /**
     * @param fingerprint the {@link #fingerprint(List, Path)} of the current inputs.
     * @return the launch remembered for the fingerprint, if its build and its Java still exist.
     */
    public Optional<Launch> get(String fingerprint) {
        Path file = getFile();
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(file)) {
            properties.load(inputStream);
        } catch (IOException e) {
            log.debug("Failed to read {}", file, e);
            return Optional.empty();
        }

        if (!fingerprint.equals(properties.getProperty("fingerprint"))) {
            return Optional.empty();
        }

        try {
            Launch launch = new Launch(
                getFile().resolveSibling(properties.getProperty("build")),
                Path.of(properties.getProperty("java.home")),
                Path.of(properties.getProperty("java.executable")),
                Integer.parseInt(properties.getProperty("java.version"))
            );

            if (Files.isRegularFile(launch.build().resolve(PluginBuilder.QUARKUS_APP).resolve("quarkus-run.jar"))
                && Files.isExecutable(launch.javaExecutable())) {
                return Optional.of(launch);
            }
        } catch (RuntimeException e) {
            log.debug("Invalid {}", file, e);
        }

        return Optional.empty();
    }

    /**
     * Remembers the launch for the given fingerprint, replacing the previous one.
     *
     * @param fingerprint the {@link #fingerprint(List, Path)} of the current inputs.
     * @param launch      what to launch for them.
     */
    public void put(String fingerprint, Launch launch) {
        Properties properties = new Properties();
        properties.setProperty("fingerprint", fingerprint);
        properties.setProperty("build", launch.build().getFileName().toString());
        properties.setProperty("java.home", launch.javaHome().toString());
        properties.setProperty("java.executable", launch.javaExecutable().toString());
        properties.setProperty("java.version", String.valueOf(launch.javaVersion()));
        Path file = getFile();
        Path temp = file.resolveSibling(FILE + "." + ProcessHandle.current().pid() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream outputStream = Files.newOutputStream(temp)) {
                properties.store(outputStream, "Written by HeadlessMc, see PluginLaunchCache");
            }

            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.warn("Failed to write {}", file, e);
        }
    }

    private Path getFile() {
        return appFiles.getCacheDir().resolve("plugins").resolve("builds").resolve(FILE);
    }

    private static String stat(Path path) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            return path.toAbsolutePath() + ":" + attributes.size() + ":" + attributes.lastModifiedTime().toMillis()
                + ":" + attributes.fileKey();
        } catch (IOException e) {
            return path.toAbsolutePath() + ":missing";
        }
    }

    /**
     * What to launch for a set of plugins.
     *
     * @param build          the build directory, containing {@value PluginBuilder#QUARKUS_APP}.
     * @param javaHome       the home of the Java to launch it with.
     * @param javaExecutable the executable of the Java to launch it with.
     * @param javaVersion    the version of the Java to launch it with.
     */
    public record Launch(Path build, Path javaHome, Path javaExecutable, int javaVersion) {

    }

}
