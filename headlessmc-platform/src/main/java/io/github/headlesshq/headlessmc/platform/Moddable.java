package io.github.headlesshq.headlessmc.platform;

import java.nio.file.Path;
import java.util.List;

public interface Moddable {
    String getName();

    Platform getPlatform();

    VersionID getVersion();

    List<Mod> listMods();

    Path getModsDirectory();

}
