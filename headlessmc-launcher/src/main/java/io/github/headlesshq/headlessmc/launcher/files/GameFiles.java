package io.github.headlesshq.headlessmc.launcher.files;

import io.github.headlesshq.headlessmc.api.settings.Config;
import io.github.headlesshq.headlessmc.launcher.settings.LauncherSettings;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class GameFiles {
    private final LauncherSettings settings;
    private final Config config;

    public Path getMcDirectory() {
        return config.get(settings.mcDir());
    }

    public Path getGameDirectory(@Nullable String name) {
        Path gameDir = config.get(settings.gameDir(), this::getMcDirectory);
        if (name != null && config.get(settings.gameDirForEachVersion())) {
            return gameDir.resolve(name);
        }

        return gameDir;
    }

}
