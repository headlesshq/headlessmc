package io.github.headlesshq.headlessmc.platform.vanilla;

import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.id.VersionID;
import lombok.Data;

import java.nio.file.Path;

@Data
public class DownloadedVersion {
    private final VersionID versionID;
    private final Version version;
    private final Path path;

}
