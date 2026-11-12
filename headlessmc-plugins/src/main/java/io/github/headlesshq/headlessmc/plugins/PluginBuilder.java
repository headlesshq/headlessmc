package io.github.headlesshq.headlessmc.plugins;

import io.github.headlesshq.headlessmc.HeadlessMc;
import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.launcher.JavaLauncherService;
import io.github.headlesshq.headlessmc.net.hash.HashService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Re-augments the mutable jar of HeadlessMc with plugins.
 * <p>
 * The mutable jar is copied to {@code <cache>/plugins/builds/<hash>/quarkus-app},
 * the plugins and their resolved libraries are placed into its {@code providers} directory,
 * which Quarkus adds as application archives, and {@code quarkus-run.jar} is run with
 * {@code -Dquarkus.launch.rebuild=true}. The result is a regular fast-jar,
 * which is reused for as long as the same plugins are installed.
 * <p>
 * Only {@link PluginConfig#keepBuilds()} builds are kept. A JVM running a build holds a shared lock on
 * {@code <hash>.use}, see {@link #use(Path)}, and builds are only deleted while holding an exclusive lock on it,
 * as well as on {@code <hash>.lock}, which is held while a build is created.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PluginBuilder {
    /**
     * The directory name Quarkus requires for the application.
     */
    public static final String QUARKUS_APP = "quarkus-app";
    /**
     * The file in a build directory, which lists the plugins of the build.
     */
    public static final String PLUGINS_FILE = "plugins.txt";
    private static final String USE = ".use";
    private static final Duration USE_TIMEOUT = Duration.ofSeconds(10);

    private final Holder<PluginConfig> config;
    private final JavaLauncherService javaLauncherService;
    private final FileService fileService;
    private final HashService hashService;
    private final AppFiles appFiles;
    private final Console console;

    /**
     * Computes the hash identifying the build of the given plugins with the given mutable jar.
     *
     * @param resolution the resolved plugins.
     * @param mutableJar the {@code quarkus-app} directory of the mutable jar.
     * @return the hash of the build.
     */
    public String hash(PluginResolution resolution, Path mutableJar) {
        List<Path> mutableFiles = new ArrayList<>();
        mutableFiles.add(mutableJar.resolve("quarkus").resolve("quarkus-application.dat"));
        // the model of the deployment dependencies, hashing them all would take too long
        mutableFiles.add(mutableJar.resolve("lib").resolve("deployment").resolve("appmodel.dat"));
        for (Path directory : List.of(mutableJar.resolve("app"), mutableJar.resolve("lib").resolve("main"), mutableJar.resolve("lib").resolve("boot"))) {
            if (!Files.isDirectory(directory)) {
                continue;
            }

            try (Stream<Path> files = Files.walk(directory)) {
                files.filter(Files::isRegularFile).sorted().forEach(mutableFiles::add);
            } catch (IOException | UncheckedIOException e) {
                throw new HeadlessMcException("Failed to list " + directory, e);
            }
        }

        StringBuilder input = new StringBuilder()
            .append("headlessmc=").append(HeadlessMc.VERSION).append('\n')
            .append("mutable=").append(hashService.hashFiles(mutableFiles, HashService.SHA256).hash()).append('\n');
        resolution.plugins().stream()
            .map(PluginFile::sha256)
            .sorted()
            .forEach(sha256 -> input.append("plugin=").append(sha256).append('\n'));
        MessageDigest digest = hashService.getAlgorithm(HashService.SHA256);
        // 32 hex chars are plenty and keep paths short on Windows
        return hashService.toHexString(digest.digest(input.toString().getBytes(StandardCharsets.UTF_8))).substring(0, 32);
    }

    /**
     * Returns the build of the given plugins, building it if it does not exist yet.
     *
     * @param resolution the resolved plugins.
     * @param mutableJar the {@code quarkus-app} directory of the mutable jar.
     * @param java       the Java to run the re-augmentation with.
     * @return the build directory, containing {@value #QUARKUS_APP}.
     * @throws HeadlessMcException if the build fails.
     */
    public Path build(PluginResolution resolution, Path mutableJar, Java java) throws HeadlessMcException {
        String hash = hash(resolution, mutableJar);
        Path builds = getBuildsDir();
        Path build = builds.resolve(hash);
        try {
            Files.createDirectories(builds);
            try (FileChannel channel = FileChannel.open(builds.resolve(hash + ".lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                if (isComplete(build)) {
                    Files.setLastModifiedTime(build, FileTime.from(Instant.now()));
                    return build;
                }

                console.write("Plugins changed, re-building HeadlessMc with " + resolution.plugins().size()
                    + " plugin(s). This takes a few seconds, subsequent runs will be faster.");
                Path temp = builds.resolve(hash + ".tmp");
                fileService.delete(temp);
                Path quarkusApp = temp.resolve(QUARKUS_APP);
                PluginFiles.copyDirectory(mutableJar, quarkusApp);
                writeProviders(resolution, quarkusApp.resolve("providers"));
                reaugment(quarkusApp, java);
                // the deployment dependencies are only needed for the re-augmentation
                fileService.delete(quarkusApp.resolve("lib").resolve("deployment"));
                Files.writeString(temp.resolve(PLUGINS_FILE), describe(resolution), StandardCharsets.UTF_8);
                fileService.delete(build);
                fileService.saveMove(temp, build);
            }
        } catch (IOException e) {
            throw new HeadlessMcException("Failed to build HeadlessMc with plugins in " + build, e);
        }

        prune(builds, build);
        return build;
    }

    /**
     * Marks the given build as in use, so that no other HeadlessMc process deletes it, until the lease is closed.
     * Also updates the modification time of the build, which decides which builds are deleted first.
     *
     * @param build the build directory, as returned by {@link #build}.
     * @return the lease, or empty if the build does not exist (anymore).
     */
    public Optional<Lease> use(Path build) {
        Path lockFile = build.resolveSibling(build.getFileName() + USE);
        FileChannel channel = null;
        try {
            Files.createDirectories(build.getParent());
            channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
            // a blocking lock would wait forever if the file system does not support shared locks,
            // the exclusive locks of other processes are only held briefly while deleting the build
            FileLock lock = null;
            long deadline = System.nanoTime() + USE_TIMEOUT.toNanos();
            while (lock == null && System.nanoTime() < deadline) {
                lock = channel.tryLock(0L, Long.MAX_VALUE, true);
                if (lock == null) {
                    Thread.sleep(50L);
                }
            }

            if (lock == null) {
                log.debug("Could not lock {}, running the build without lock", lockFile);
                channel.close();
                channel = null;
            }

            // checked after locking, the build might have been deleted while we were waiting
            if (!isComplete(build)) {
                if (channel != null) {
                    channel.close();
                }

                return Optional.empty();
            }

            Files.setLastModifiedTime(build, FileTime.from(Instant.now()));
            return Optional.of(new Lease(channel));
        } catch (IOException | OverlappingFileLockException e) {
            log.debug("Failed to lock {}", lockFile, e);
            close(channel);
            return isComplete(build) ? Optional.of(new Lease(null)) : Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            close(channel);
            throw new HeadlessMcException("Interrupted while waiting for " + lockFile, e);
        }
    }

    /**
     * @param build the hash of a build, see {@link PluginBootstrap#getCurrentBuild()}.
     * @return the SHA-256 hashes of the plugin jars contained in the build, empty if the build does not exist.
     */
    public Set<String> getPluginHashes(String build) {
        Path file = getBuildsDir().resolve(build).resolve(PLUGINS_FILE);
        if (!Files.isRegularFile(file)) {
            return Set.of();
        }

        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return lines.map(line -> line.split(" "))
                .filter(parts -> parts.length == 4 && parts[0].equals("plugin"))
                .map(parts -> parts[3])
                .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            log.warn("Failed to read {}", file, e);
            return Set.of();
        }
    }

    /**
     * @return the directory builds are placed in.
     */
    public Path getBuildsDir() {
        return appFiles.getCacheDir().resolve("plugins").resolve("builds");
    }

    private void writeProviders(PluginResolution resolution, Path providers) throws IOException {
        fileService.delete(providers);
        Files.createDirectories(providers);
        for (PluginFile plugin : resolution.plugins()) {
            @SuppressWarnings("DataFlowIssue") // resolved plugins have a manifest
            String id = plugin.manifest().id();
            Files.copy(plugin.path(), providers.resolve("plugin-" + id + ".jar"), StandardCopyOption.REPLACE_EXISTING);
        }

        List<PluginResolution.Library> libraries = resolution.libraries();
        for (int i = 0; i < libraries.size(); i++) {
            PluginResolution.Library library = libraries.get(i);
            PluginManifest.Library entry = library.library();
            // the index keeps e.g. a-b:c and a:b-c apart, the PluginScanner validated the rest of the name
            Path target = providers.resolve("lib-" + i + "-" + entry.group() + "-" + entry.artifact() + "-" + entry.version() + ".jar");
            try (ZipFile zipFile = new ZipFile(library.source().path().toFile())) {
                ZipEntry zipEntry = zipFile.getEntry(entry.path());
                if (zipEntry == null) {
                    throw new HeadlessMcException(library.source().fileName() + " does not contain its library " + entry.path());
                }

                try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
                    Files.copy(inputStream, target);
                }
            }

            String sha256 = hashService.hashFiles(List.of(target), HashService.SHA256).hash();
            if (!sha256.equalsIgnoreCase(entry.sha256())) {
                throw new HeadlessMcException("Library " + entry.path() + " of " + library.source().fileName()
                    + " has hash " + sha256 + ", but its manifest declares " + entry.sha256());
            }
        }
    }

    private void reaugment(Path quarkusApp, Java java) throws HeadlessMcException {
        int exitCode = javaLauncherService.buildProcess()
            .id("HeadlessMc re-augmentation")
            .java(java)
            .directory(quarkusApp)
            .systemProperty("quarkus.launch.rebuild", "true")
            .jar(quarkusApp.resolve("quarkus-run.jar"))
            .start()
            .waitFor((Duration) null);
        if (exitCode != 0) {
            throw new HeadlessMcException("Re-augmentation of HeadlessMc with plugins failed with exit code " + exitCode);
        }
    }

    private static String describe(PluginResolution resolution) {
        StringBuilder builder = new StringBuilder();
        for (PluginFile plugin : resolution.plugins()) {
            @SuppressWarnings("DataFlowIssue")
            PluginManifest manifest = plugin.manifest();
            builder.append("plugin ").append(manifest.id()).append(' ').append(manifest.version())
                .append(' ').append(plugin.sha256()).append('\n');
        }

        for (PluginResolution.Library library : resolution.libraries()) {
            builder.append("library ").append(library.library().module()).append(' ').append(library.library().version())
                .append(' ').append(String.join(",", library.requestedBy())).append('\n');
        }

        return builder.toString();
    }

    private boolean isComplete(Path build) {
        return Files.isRegularFile(build.resolve(PLUGINS_FILE))
            && Files.isRegularFile(build.resolve(QUARKUS_APP).resolve("quarkus-run.jar"));
    }

    void prune(Path builds, Path current) {
        int keep = Math.max(1, config.get().keepBuilds());
        try (Stream<Path> stream = Files.list(builds)) {
            List<Path> directories = stream.filter(Files::isDirectory).toList();
            for (Path temp : directories.stream().filter(path -> path.getFileName().toString().endsWith(".tmp")).toList()) {
                deleteAbandoned(builds, temp);
            }

            List<Path> old = directories.stream()
                .filter(path -> !path.equals(current))
                .filter(path -> !path.getFileName().toString().endsWith(".tmp"))
                .sorted(Comparator.comparing(PluginBuilder::lastModified).reversed())
                .skip(keep - 1L)
                .toList();
            for (Path path : old) {
                try {
                    deleteUnused(builds, path);
                } catch (IOException | HeadlessMcException e) {
                    log.debug("Failed to delete old plugin build {}", path, e);
                }
            }
        } catch (IOException | HeadlessMcException e) {
            log.warn("Failed to delete old plugin builds in {}", builds, e);
        }
    }

    /**
     * Deletes an old build, unless another process is building or running it.
     */
    private void deleteUnused(Path builds, Path build) throws IOException {
        Path buildLock = builds.resolve(build.getFileName() + ".lock");
        Path useLock = builds.resolve(build.getFileName() + USE);
        try (FileChannel buildChannel = FileChannel.open(buildLock, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock buildFileLock = buildChannel.tryLock();
             FileChannel useChannel = FileChannel.open(useLock, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock useFileLock = useChannel.tryLock()) {
            if (buildFileLock == null || useFileLock == null) {
                log.debug("Not deleting old plugin build {}, it is in use", build);
                return;
            }

            log.debug("Deleting old plugin build {}", build);
            fileService.delete(build);
        } catch (OverlappingFileLockException e) {
            log.debug("Not deleting old plugin build {}, it is used by this process", build);
            return;
        }

        // another process might have opened them in the meantime, it will notice that the build is gone
        try {
            Files.deleteIfExists(buildLock);
            Files.deleteIfExists(useLock);
        } catch (IOException e) {
            log.debug("Failed to delete the locks of {}", build, e);
        }
    }

    /**
     * Deletes the temporary directory of a build that failed or has been interrupted,
     * unless another process is still building it.
     */
    private void deleteAbandoned(Path builds, Path temp) throws IOException {
        String name = temp.getFileName().toString();
        Path lock = builds.resolve(name.substring(0, name.length() - ".tmp".length()) + ".lock");
        try (FileChannel channel = FileChannel.open(lock, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock fileLock = channel.tryLock()) {
            if (fileLock != null) {
                log.debug("Deleting abandoned plugin build {}", temp);
                fileService.delete(temp);
            }
        } catch (OverlappingFileLockException e) {
            log.debug("{} is locked by this process", lock);
        }
    }

    private static void close(@Nullable FileChannel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                log.debug("Failed to close {}", channel, e);
            }
        }
    }

    /**
     * Marks a build as in use, see {@link #use(Path)}.
     */
    public static final class Lease implements AutoCloseable {
        private final @Nullable FileChannel channel;

        private Lease(@Nullable FileChannel channel) {
            this.channel = channel;
        }

        @Override
        public void close() {
            PluginBuilder.close(channel);
        }

    }

    private static FileTime lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException e) {
            return FileTime.fromMillis(0L);
        }
    }

}
