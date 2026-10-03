package io.github.headlesshq.headlessmc.files;

import io.github.headlesshq.headlessmc.config.Holder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Default implementation of {@link AppFiles}.
 * Dynamic, uses a dynamic config and a provider for the XDG spec,
 * so that if configuration changes at runtime other files are used.
 */
@Default
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DefaultAppFiles implements AppFiles {
    private final Provider<XdgBaseDirectories> xdg;
    private final Holder<FileConfig> config;
    private final FileService fileService;

    @Override
    public Path getConfigDir() {
        return customDir().orElseGet(() -> xdg.get().configHome().resolve("headlessmc"));
    }

    @Override
    public Path getDataDir() {
        return customDir().orElseGet(() -> xdg.get().dataHome().resolve("headlessmc"));
    }

    @Override
    public Path getStateDir() {
        return customDir().orElseGet(() -> xdg.get().stateHome().resolve("headlessmc"));
    }

    @Override
    public Path getCacheDir() {
        return customDir().map(path -> path.resolve("cache"))
            .orElseGet(() -> xdg.get().cacheHome().resolve("headlessmc"));
    }

    private Optional<Path> customDir() {
        String location = config.get().location().orElse(null);
        if (location != null) {
            return Optional.of(fileService.getUserPath(location));
        }

        return Optional.empty();
    }

}
