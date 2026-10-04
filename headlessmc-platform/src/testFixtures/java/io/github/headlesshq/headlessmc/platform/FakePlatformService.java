package io.github.headlesshq.headlessmc.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A {@link PlatformService} over a {@link FakeVanillaPlatform}
 * and any additional platforms.
 */
public class FakePlatformService implements PlatformService {
    private final FakeVanillaPlatform vanillaPlatform;
    private final List<Platform> platforms = new ArrayList<>();

    public FakePlatformService(FakeVanillaPlatform vanillaPlatform, Platform... others) {
        this.vanillaPlatform = vanillaPlatform;
        platforms.add(vanillaPlatform);
        platforms.addAll(List.of(others));
    }

    @Override
    public List<Platform> getPlatforms() {
        return platforms;
    }

    @Override
    public List<Platform> getPlatformsWithoutVanilla() {
        return platforms.stream()
            .filter(platform -> !(platform instanceof VanillaPlatform))
            .toList();
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
