package io.github.headlesshq.headlessmc.plugins;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.net.hash.HashService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

/**
 * Provides the {@code quarkus-app} directory of a mutable-jar build of HeadlessMc,
 * which contains the deployment dependencies needed to re-augment HeadlessMc with plugins.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class MutableJarProvider {
    private static final String COMPLETE = ".complete";

    private final Holder<PluginConfig> config;
    private final DownloadService downloadService;
    private final FileService fileService;
    private final HashService hashService;
    private final AppFiles appFiles;

    /**
     * Returns the {@code quarkus-app} directory of the mutable jar,
     * downloading and verifying it with the {@link MutableJarLock} if necessary.
     *
     * @return the {@code quarkus-app} directory, containing {@code lib/deployment}.
     * @throws HeadlessMcException if the mutable jar could not be found or downloaded.
     */
    public Path get() throws HeadlessMcException {
        Optional<String> configured = config.get().mutableJar();
        if (configured.isPresent()) {
            return fromConfig(Path.of(configured.get()));
        }

        MutableJarLock lock = MutableJarLock.load().orElseThrow(() -> new HeadlessMcException(
            "This build of HeadlessMc does not know its mutable jar, which is needed to load plugins."
                + " Set hmc.plugins.mutable-jar to a quarkus-app directory or headlessmc-mutable.zip."));
        return extract(lock.sha256(), mutableDir -> {
            Path zip = mutableDir.resolve(lock.sha256() + ".zip");
            log.info("Downloading the HeadlessMc mutable jar {} to load plugins", lock.url());
            downloadService.download(URI.create(lock.url()))
                .size(lock.size())
                .hash(HashService.SHA256, lock.sha256())
                .progressBar("Downloading HeadlessMc mutable jar")
                .toFile(zip);
            return zip;
        }, true);
    }

    private Path fromConfig(Path path) throws HeadlessMcException {
        if (Files.isDirectory(path)) {
            if (!Files.isDirectory(path.resolve("lib").resolve("deployment"))) {
                throw new HeadlessMcException(path + " is not the quarkus-app directory of a mutable jar, lib/deployment is missing");
            }

            return path;
        }

        if (!Files.isRegularFile(path)) {
            throw new HeadlessMcException("hmc.plugins.mutable-jar " + path + " does not exist");
        }

        String sha256 = hashService.hashFiles(List.of(path), HashService.SHA256).hash();
        return extract(sha256, mutableDir -> path, false);
    }

    /**
     * Extracts the zip with the given hash into {@code <cache>/plugins/mutable/<sha256>}, unless that already happened.
     * Multiple HeadlessMc processes may start at the same time, so this happens while holding a lock on
     * {@code <sha256>.lock} (and synchronized, because file locks are held by the whole JVM),
     * into a temporary directory, which is then moved into place.
     *
     * @param sha256    the hash of the zip.
     * @param zip       provides the zip, given the mutable directory, only called if it has not been extracted yet.
     * @param deleteZip whether to delete the zip after extracting it.
     * @return the {@code quarkus-app} directory.
     */
    private synchronized Path extract(String sha256, ZipProvider zip, boolean deleteZip) throws HeadlessMcException {
        Path mutableDir = getMutableDir();
        Path directory = mutableDir.resolve(sha256);
        Path quarkusApp = directory.resolve("quarkus-app");
        if (Files.exists(directory.resolve(COMPLETE))) {
            return quarkusApp;
        }

        try {
            Files.createDirectories(mutableDir);
            try (FileChannel channel = FileChannel.open(mutableDir.resolve(sha256 + ".lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                // another process might have extracted it while we were waiting for the lock
                if (Files.exists(directory.resolve(COMPLETE))) {
                    return quarkusApp;
                }

                Path zipFile = zip.get(mutableDir);
                Path temp = mutableDir.resolve(sha256 + ".tmp");
                fileService.delete(temp);
                PluginFiles.unzip(zipFile, temp);
                if (!Files.isDirectory(temp.resolve("quarkus-app").resolve("lib").resolve("deployment"))) {
                    throw new HeadlessMcException(zipFile + " does not contain a quarkus-app directory with lib/deployment");
                }

                Files.createFile(temp.resolve(COMPLETE));
                fileService.delete(directory);
                Files.move(temp, directory, StandardCopyOption.ATOMIC_MOVE);
                if (deleteZip) {
                    fileService.delete(zipFile);
                }
            }
        } catch (IOException e) {
            throw new HeadlessMcException("Failed to extract the mutable jar " + sha256, e);
        }

        return quarkusApp;
    }

    private Path getMutableDir() {
        return appFiles.getCacheDir().resolve("plugins").resolve("mutable");
    }

    @FunctionalInterface
    private interface ZipProvider {
        Path get(Path mutableDir) throws HeadlessMcException;
    }

}
