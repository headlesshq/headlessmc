package io.github.headlesshq.headlessmc.platform;

import java.nio.file.Path;

public interface PlatformDownloadOptions {
    VersionID getVersionID();

    boolean inMemory();

    Path getPath();

}
