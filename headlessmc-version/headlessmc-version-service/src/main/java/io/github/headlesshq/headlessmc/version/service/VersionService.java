package io.github.headlesshq.headlessmc.version.service;

import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.id.VersionID;
import io.github.headlesshq.headlessmc.version.parser.MergedVersion;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface VersionService extends Iterable<Version> {
    @Nullable Version get(VersionID id);

    VersionID getId(Version version);

    MergedVersion resolve(VersionID id);

    Version addOrGet(Path versionJsonFile) throws IOException;

    Path getVersionJsonFile(Version version) throws IOException;

    List<Version> refresh();

}
