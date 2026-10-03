package io.github.headlesshq.headlessmc.platform;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

/**
 * Default {@link PlatformService} implementation.
 */
@ApplicationScoped
public class PlatformServiceImpl implements PlatformService {
    private final Instance<Platform> platforms;
    private final VanillaPlatform vanillaPlatform;

    @Inject
    public PlatformServiceImpl(
        @Any Instance<Platform> platforms,
        // requires a dependency on headlessmc-platform-vanilla
        VanillaPlatform vanillaPlatform
    ) {
        this.platforms = platforms;
        this.vanillaPlatform = vanillaPlatform;
    }

    @Override
    public List<Platform> getPlatformsWithoutVanilla() {
        return platforms.stream()
                .filter(platform -> !(platform instanceof VanillaPlatform)) // use @Vanilla qualifier?
                .toList();
    }

    @Override
    public List<Platform> getPlatforms() {
        return platforms.stream().toList();
    }

    @Override
    public Optional<Platform> getPlatform(String name) {
        return platforms.stream()
                .filter(platform -> name.equalsIgnoreCase(platform.getName()))
                .findFirst();
    }

    @Override
    public VanillaPlatform getVanillaPlatform() {
        return vanillaPlatform;
    }

}
