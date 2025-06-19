package io.github.headlesshq.headlessmc.version.id;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface PlatformBuildProvider {
    List<String> getBuilds(String version, boolean server);

    @Nullable
    String getLatest(boolean server);

}
