package io.github.headlesshq.headlessmc.platform;

import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public interface PlatformService extends Iterable<Platform> {
    @Nullable Platform getPlatform(String name);

    Stream<Platform> stream();

}
