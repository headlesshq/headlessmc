package io.github.headlesshq.headlessmc.launcher.mods;

import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.launcher.api.VersionId;

import java.io.IOException;
import java.util.List;

public interface ModDistributionPlatform extends HasName {
    List<Mod> search(String name) throws IOException;

    List<Mod> search(String name, VersionId versionId) throws IOException;

    void download(ModdableGame game, String modName) throws IOException;

}
