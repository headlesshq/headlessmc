package io.github.headlesshq.headlessmc.platform;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.stream.Stream;

@ApplicationScoped
public class PlatformServiceImpl implements PlatformService {
    private final Instance<Platform> platforms;

    @Inject
    public PlatformServiceImpl(@Any Instance<Platform> platforms) {
        this.platforms = platforms;
    }

    @Override
    public @Nullable Platform getPlatform(String name) {
        Instance<Platform> platform = platforms.select(NamedLiteral.of(name));
        if (platform.isResolvable()) {
            return platform.get();
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
