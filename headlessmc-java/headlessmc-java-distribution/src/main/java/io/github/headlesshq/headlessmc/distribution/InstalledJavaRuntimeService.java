package io.github.headlesshq.headlessmc.distribution;

import io.github.headlesshq.headlessmc.exceptions.FileException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.cache.CacheObject;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Installs {@link JavaRuntime}s and records which runtime has been installed into which directory,
 * see {@link InstalledJavaRuntime}.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class InstalledJavaRuntimeService {
    public static final String FILE_NAME = "headlessmc-runtime.json";
    /**
     * The version of the JSON format of {@link InstalledJavaRuntime}, increment it whenever the format changes.
     */
    public static final int FORMAT_VERSION = 1;

    private final JsonService jsonService;

    /**
     * Downloads the given runtime to the given path and records which runtime has been installed there,
     * so that it can be checked for updates later.
     *
     * @param provider the provider to download the runtime with.
     * @param runtime  the runtime to install.
     * @param path     the installation directory.
     * @throws HeadlessMcException if the download fails or the runtime could not be recorded.
     */
    public void install(JavaDistributionProvider provider, JavaRuntime runtime, Path path) throws HeadlessMcException {
        provider.download(runtime, path);
        write(path, runtime);
    }

    /**
     * Records that the given runtime has been installed into the given directory.
     *
     * @param installation the installation directory.
     * @param runtime      the runtime installed into the directory.
     * @throws HeadlessMcException if the file could not be written.
     */
    public void write(Path installation, JavaRuntime runtime) throws HeadlessMcException {
        CacheObject<InstalledJavaRuntime> object = new CacheObject<>(FORMAT_VERSION, InstalledJavaRuntime.of(runtime));
        jsonService.write(installation.resolve(FILE_NAME), object, true);
    }

    /**
     * @param installation the installation directory.
     * @return the runtime installed into the given directory,
     * or empty if it has not been recorded or has been recorded in a different {@link #FORMAT_VERSION}.
     * @throws HeadlessMcException if the file exists but could not be parsed.
     */
    public Optional<JavaRuntime> read(Path installation) throws HeadlessMcException {
        Path file = installation.resolve(FILE_NAME);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }

        Optional<InstalledJavaRuntime> installed;
        try (InputStream inputStream = Files.newInputStream(file)) {
            installed = jsonService.parseVersioned(inputStream, new TypeLiteral<InstalledJavaRuntime>() {}, FORMAT_VERSION);
        } catch (IOException e) {
            throw new FileException("Failed to read " + file, e);
        }

        if (installed.isEmpty()) {
            log.info("{} has a different format than version {}, ignoring it", file, FORMAT_VERSION);
            return Optional.empty();
        }

        return Optional.of(installed.get().toRuntime());
    }

}
