package io.github.headlesshq.headlessmc.files;

import io.github.headlesshq.headlessmc.os.OS;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.function.Function;

import static io.github.headlesshq.headlessmc.files.XdgBaseDirectories.*;

/**
 * Factory for {@link XdgBaseDirectories}.
 */
@Slf4j
@ApplicationScoped
public class XdgBaseDirectoriesFactory {
    @Default
    @Produces
    @Dependent
    public XdgBaseDirectories resolve(FileService fileService, OS os) {
        return resolve(fileService, os, System::getenv, userHome(fileService));
    }

    @VisibleForTesting
    XdgBaseDirectories resolve(
        FileService fileService,
        OS os,
        Function<String, @Nullable String> environment,
        Path userHome
    ) {
        if (OS.Type.WINDOWS.equals(os.type())) {
            Path roaming = fromEnv(
                fileService, environment, APPDATA, userHome.resolve("AppData").resolve("Roaming")
            );

            Path local = fromEnv(
                fileService, environment, LOCALAPPDATA, userHome.resolve("AppData").resolve("Local")
            );

            return resolve(
                fileService, environment, roaming, roaming, local.resolve("state"), local.resolve("cache")
            );
        }

        if (OS.Type.MACOS.equals(os.type())) {
            Path applicationSupport = userHome.resolve("Library").resolve("Application Support");
            return resolve(
                fileService,
                environment,
                applicationSupport,
                applicationSupport,
                applicationSupport,
                userHome.resolve("Library").resolve("Caches")
            );
        }

        Path local = userHome.resolve(".local");
        return resolve(
            fileService,
            environment,
            local.resolve("share"),
            userHome.resolve(".config"),
            local.resolve("state"),
            userHome.resolve(".cache")
        );
    }

    private XdgBaseDirectories resolve(
        FileService fileService,
        Function<String, @Nullable String> environment,
        Path data,
        Path config,
        Path state,
        Path cache
    ) {
        return new XdgBaseDirectories(
            fromEnv(fileService, environment, XDG_DATA_HOME, data),
            fromEnv(fileService, environment, XDG_CONFIG_HOME, config),
            fromEnv(fileService, environment, XDG_STATE_HOME, state),
            fromEnv(fileService, environment, XDG_CACHE_HOME, cache)
        );
    }

    private Path fromEnv(
        FileService fileService,
        Function<String, @Nullable String> environment,
        String variable,
        Path fallback
    ) {
        String value = environment.apply(variable);
        if (value == null || value.isBlank()) {
            return fallback;
        }

        Path path;
        try {
            path = fileService.getUserPath(value);
        } catch (InvalidPathException e) {
            log.warn("{} is not a valid path, using {} instead", variable, fallback, e);
            return fallback;
        }

        if (!path.isAbsolute()) {
            log.warn("{} is relative ({}), using {} instead", variable, path, fallback);
            return fallback;
        }

        return path;
    }

    private static Path userHome(FileService fileService) {
        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.isBlank()) {
            log.error("user.home not specified");
            return fileService.getUserPath();
        }

        return fileService.getUserPath(userHome);
    }

}
