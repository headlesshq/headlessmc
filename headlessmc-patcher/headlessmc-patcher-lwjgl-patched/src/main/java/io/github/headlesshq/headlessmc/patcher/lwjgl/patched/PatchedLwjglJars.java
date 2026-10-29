package io.github.headlesshq.headlessmc.patcher.lwjgl.patched;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.quarkus.runtime.annotations.RegisterResources;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Provides the patched jars of <a href="https://github.com/headlesshq/headlessmc-lwjgl">headlessmc-lwjgl</a>.
 * They are downloaded from the GitHub release locked in {@link #RESOURCE_NAME} into the cache directory and verified
 * by their size and SHA-256 checksum. If {@link #DIRECTORY_PROPERTY} is set, the jars are taken from that directory.
 */
@ApplicationScoped
@RegisterResources(globs = {PatchedLwjglJars.RESOURCE_NAME})
class PatchedLwjglJars {
    static final String RESOURCE_NAME = PatchedLwjglPatcher.RESOURCES + "headlessmc-lwjgl.json";
    /**
     * A directory with the same layout as the keys of {@link PatchedLwjglLock#jars()} ({@code lwjgl3/<module>.jar},
     * {@code lwjgl2/...}, {@code headlessmc-lwjgl-api.jar}), used instead of the release, e.g. for testing a local
     * build of headlessmc-lwjgl.
     */
    static final String DIRECTORY_PROPERTY = "hmc.lwjgl.patched-directory";

    private final DownloadService downloadService;
    private final AppFiles appFiles;
    private final PatchedLwjglLock lock;

    @Inject
    PatchedLwjglJars(JsonService jsonService, DownloadService downloadService, AppFiles appFiles) {
        this(downloadService, appFiles, loadLock(jsonService));
    }

    PatchedLwjglJars(DownloadService downloadService, AppFiles appFiles, PatchedLwjglLock lock) {
        this.downloadService = downloadService;
        this.appFiles = appFiles;
        this.lock = lock;
    }

    public PatchedLwjglLock getLock() {
        return lock;
    }

    /**
     * @param jar the path of a patched jar, e.g. {@code lwjgl3/lwjgl-glfw.jar}.
     * @return {@code true} if the patched jar exists.
     */
    public boolean exists(String jar) {
        Path directory = directory();
        return directory != null ? Files.isRegularFile(directory.resolve(jar)) : lock.jars().containsKey(jar);
    }

    /**
     * Downloads the patched jar, if it has not been downloaded yet.
     *
     * @param jar the path of a patched jar, e.g. {@code lwjgl3/lwjgl-glfw.jar}.
     * @return the path of the patched jar.
     */
    public Path get(String jar) {
        Path directory = directory();
        if (directory != null) {
            return directory.resolve(jar);
        }

        PatchedLwjglJar locked = Objects.requireNonNull(lock.jars().get(jar), () -> "No patched LWJGL jar " + jar);
        Path path = appFiles.getCacheDir().resolve("lwjgl-patched").resolve(lock.version()).resolve(locked.fileName());
        if (isDownloaded(path, locked)) {
            return path;
        }

        return downloadService.download(locked.getURL())
            .sha256(locked.sha256())
            .size(locked.size())
            .toFile(path);
    }

    private static boolean isDownloaded(Path path, PatchedLwjglJar jar) {
        try {
            return Files.isRegularFile(path) && Files.size(path) == jar.size();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static @Nullable Path directory() {
        String directory = System.getProperty(DIRECTORY_PROPERTY);
        return directory == null || directory.isBlank() ? null : Path.of(directory);
    }

    static PatchedLwjglLock loadLock(JsonService jsonService) {
        try (InputStream stream = PatchedLwjglJars.class.getClassLoader().getResourceAsStream(RESOURCE_NAME)) {
            Objects.requireNonNull(stream, RESOURCE_NAME + " not found");
            return jsonService.parseRegisteredForReflection(stream, new TypeLiteral<PatchedLwjglLock>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + RESOURCE_NAME, e);
        }
    }

}
