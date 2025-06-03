package io.github.headlesshq.headlessmc.platform;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class PlatformServiceImpl implements PlatformService {
    private final List<Platform> platforms;

    @Inject
    public PlatformServiceImpl(@Any Instance<Platform> platforms) {
        this.platforms = Collections.unmodifiableList(platforms.stream().collect(Collectors.toList()));
    }

    @Override
    public @Nullable Platform getPlatform(String name) {
        for (Platform platform : platforms) {
            if (platform.getName().equalsIgnoreCase(name)) {
                return platform;
            }
        }

        return null;
    }

    @Override
    public Stream<Platform> stream() {
        return platforms.stream();
    }

    @Override
    public Iterator<Platform> iterator() {
        return platforms.iterator();
    }

}
