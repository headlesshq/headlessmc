package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.version.id.VersionID;

import java.nio.file.Path;

public interface PlatformDownloadOptions {
    VersionID getVersionID();

    boolean isInMemory();

    PathProvider getPathProvider();

    PathProvider getVanillaPathProvider();

    @FunctionalInterface
    interface PathProvider {
        Path getPath(VersionID resolvedVersionID);
    }

}
