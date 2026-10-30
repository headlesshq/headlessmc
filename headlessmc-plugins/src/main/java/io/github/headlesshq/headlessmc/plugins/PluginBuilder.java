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

import java.io.IOException;
import java.io.InputStream;
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
        // all jars, the libraries contain most of HeadlessMc, which can change without a new version during development
        List<Path> mutableFiles = new ArrayList<>();
        mutableFiles.add(mutableJar.resolve("quarkus").resolve("quarkus-application.dat"));
        mutableFiles.add(mutableJar.resolve("lib").resolve("deployment").resolve("appmodel.dat"));
        for (String directory : List.of("app", "lib/boot", "lib/main", "hmc")) {
            Path path = mutableJar.resolve(directory);
            if (!Files.isDirectory(path)) {
                continue;
            }

            try (Stream<Path> files = Files.list(path)) {
                files.filter(Files::isRegularFile).sorted().forEach(mutableFiles::add);
            } catch (IOException e) {
                throw new HeadlessMcException("Failed to list " + path, e);
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
                    + " plugin(s). This takes a few seconds, subsequent runs will be faster."
                    + " Run 'plugin compile' afterwards to compile a native executable, which starts even faster.");
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

        for (PluginResolution.Library library : resolution.libraries()) {
            PluginManifest.Library entry = library.library();
            Path target = providers.resolve("lib-" + entry.group() + "-" + entry.artifact() + "-" + entry.version() + ".jar");
            try (ZipFile zipFile = new ZipFile(library.source().path().toFile())) {
                ZipEntry zipEntry = zipFile.getEntry(entry.path());
                if (zipEntry == null) {
                    throw new HeadlessMcException(library.source().fileName() + " does not contain its library " + entry.path());
                }

                try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
                    Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
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

    private void prune(Path builds, Path current) {
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
                log.debug("Deleting old plugin build {}", path);
                fileService.delete(path);
                Files.deleteIfExists(builds.resolve(path.getFileName() + ".lock"));
            }
        } catch (IOException | HeadlessMcException e) {
            log.warn("Failed to delete old plugin builds in {}", builds, e);
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

    private static FileTime lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException e) {
            return FileTime.fromMillis(0L);
        }
    }

}
