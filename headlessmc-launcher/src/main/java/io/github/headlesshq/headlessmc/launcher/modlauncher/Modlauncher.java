package io.github.headlesshq.headlessmc.launcher.modlauncher;

import io.github.headlesshq.headlessmc.launcher.api.Platform;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

// This is just Platforms for the client, and should have a different name
@Getter
@RequiredArgsConstructor
public enum Modlauncher {
    FABRIC(Platform.FABRIC, "fabric", "fabric"),
    LEXFORGE(Platform.FORGE, "forge", "lexforge"),
    NEOFORGE(Platform.NEOFORGE, "neoforge", "neoforge");

    private final Platform platform;
    private final String officialName;
    private final String hmcName;

    public static @Nullable Modlauncher getFromVersionName(String name) {
        String lower = name.toLowerCase(Locale.ENGLISH);
        if (lower.contains(FABRIC.officialName)) {
            return FABRIC;
        }
        // ask for neoforge before forge
        if (lower.contains(NEOFORGE.officialName)) {
            return NEOFORGE;
        }

        if (lower.contains(LEXFORGE.officialName)) {
            return LEXFORGE;
        }

        return null;
    }

}
