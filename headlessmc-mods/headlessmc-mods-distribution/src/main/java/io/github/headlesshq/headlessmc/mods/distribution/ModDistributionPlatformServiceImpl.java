package io.github.headlesshq.headlessmc.mods.distribution;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Default implementation of {@link ModDistributionPlatformService}.
 */
@ApplicationScoped
public class ModDistributionPlatformServiceImpl implements ModDistributionPlatformService {
    private final Instance<ModDistributionPlatform> platforms;

    @Inject
    public ModDistributionPlatformServiceImpl(@Any Instance<ModDistributionPlatform> platforms) {
        this.platforms = platforms;
    }

    @Override
    public ModDistributionPlatform defaultPlatform() {
        return new CompositeDistributionPlatform(
            ModDistributionPlatform.DEFAULT,
            ModDistributionPlatform.DEFAULT_PLATFORMS.stream()
                .flatMap(name -> findPlatform(name).stream())
                .toList()
        );
    }

    @Override
    public Stream<ModDistributionPlatform> platforms() {
        return platforms.stream();
    }

    @Override
    public Optional<ModDistributionPlatform> byName(String name) {
        if (ModDistributionPlatform.DEFAULT.equalsIgnoreCase(name)) {
            return Optional.of(defaultPlatform());
        }

        if (ModDistributionPlatform.ALL.equalsIgnoreCase(name)) {
            return Optional.of(new CompositeDistributionPlatform(ModDistributionPlatform.ALL, platforms().toList()));
        }

        return findPlatform(name);
    }

    private Optional<ModDistributionPlatform> findPlatform(String name) {
        return platforms.stream()
            .filter(platform -> platform.getName().equalsIgnoreCase(name))
            .findFirst();
    }

}
