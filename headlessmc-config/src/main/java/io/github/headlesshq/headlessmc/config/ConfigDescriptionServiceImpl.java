package io.github.headlesshq.headlessmc.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Default implementation of {@link ConfigDescriptionService}.
 */
@ApplicationScoped
public class ConfigDescriptionServiceImpl implements ConfigDescriptionService {
    private final Instance<ConfigDescriptionSource> sources;

    @Inject
    public ConfigDescriptionServiceImpl(@Any Instance<ConfigDescriptionSource> sources) {
        this.sources = sources;
    }

    @Override
    public Optional<String> getDescription(String name) {
        return sources.stream()
            .flatMap(source -> source.getDescription(name).stream())
            .findFirst();
    }

    @Override
    public Stream<ConfigDescriptionSource> sources() {
        return sources.stream();
    }

}
