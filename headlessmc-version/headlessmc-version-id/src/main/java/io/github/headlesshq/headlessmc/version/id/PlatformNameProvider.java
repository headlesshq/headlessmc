package io.github.headlesshq.headlessmc.version.id;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface PlatformNameProvider {
    List<String> getPlatformNames(String version, boolean server);

    List<String> getAllPlatformNames();

    @Nullable
    PlatformBuildProvider getPlatformBuildProvider(String name, String platformName, boolean server);

}
