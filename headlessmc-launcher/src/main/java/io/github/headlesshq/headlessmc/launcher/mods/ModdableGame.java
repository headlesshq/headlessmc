package io.github.headlesshq.headlessmc.launcher.mods;

import io.github.headlesshq.headlessmc.api.traits.HasId;
import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.launcher.api.Platform;
import io.github.headlesshq.headlessmc.launcher.api.VersionId;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public interface ModdableGame extends HasName, HasId {
    Path getModsDirectory();

    String getVersionName();

    Platform getPlatform();

    @Nullable String getBuild();

    boolean isServer();

    default VersionId getVersionId() {
        return new VersionId(getPlatform(), getBuild(), isServer(), getVersionName());
    }

}
