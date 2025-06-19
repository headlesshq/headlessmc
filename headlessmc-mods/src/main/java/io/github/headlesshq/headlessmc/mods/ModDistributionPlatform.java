package io.github.headlesshq.headlessmc.mods;

import io.github.headlesshq.headlessmc.version.id.VersionID;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface ModDistributionPlatform {
    String getName();

    List<ModDownload> search(String name) throws IOException;

    List<ModDownload> search(String name, VersionID version) throws IOException;

    void download(VersionID id, Path directory, String modName) throws IOException;

}
