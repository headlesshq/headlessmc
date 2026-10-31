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
import java.nio.file.Files;
import java.nio.file.Path;
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
        Path directory = getMutableDir().resolve(lock.sha256());
        Path quarkusApp = directory.resolve("quarkus-app");
        if (Files.exists(directory.resolve(COMPLETE))) {
            return quarkusApp;
        }

        Path zip = getMutableDir().resolve(lock.sha256() + ".zip");
        log.info("Downloading the HeadlessMc mutable jar {} to load plugins", lock.url());
        downloadService.download(URI.create(lock.url()))
            .size(lock.size())
            .hash(HashService.SHA256, lock.sha256())
            .progressBar("Downloading HeadlessMc mutable jar")
            .toFile(zip);
        extract(zip, directory);
        fileService.delete(zip);
        return quarkusApp;
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
        Path directory = getMutableDir().resolve(sha256);
        if (!Files.exists(directory.resolve(COMPLETE))) {
            extract(path, directory);
        }

        return directory.resolve("quarkus-app");
    }

    private void extract(Path zip, Path directory) throws HeadlessMcException {
        try {
            fileService.delete(directory);
            PluginFiles.unzip(zip, directory);
            if (!Files.isDirectory(directory.resolve("quarkus-app").resolve("lib").resolve("deployment"))) {
                throw new HeadlessMcException(zip + " does not contain a quarkus-app directory with lib/deployment");
            }

            Files.createFile(directory.resolve(COMPLETE));
        } catch (IOException e) {
            throw new HeadlessMcException("Failed to extract " + zip, e);
        }
    }

    private Path getMutableDir() {
        return appFiles.getCacheDir().resolve("plugins").resolve("mutable");
    }

}
