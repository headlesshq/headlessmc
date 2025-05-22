package io.github.headlesshq.headlessmc.launcher.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import io.github.headlesshq.headlessmc.api.traits.HasName;
import org.jetbrains.annotations.Nullable;

@Getter
@RequiredArgsConstructor
public enum Platform implements HasName {
    VANILLA("vanilla", "vanilla", true, true),
    FABRIC("fabric", "fabric", true, true),
    FORGE("forge", "lexforge", true, true),
    NEOFORGE("neoforge", "neoforge", true, true),
    PAPER("paper", "paper", false, true),
    PURPUR("purpur", "purpur", false, true);

    private final String name;
    private final String hmcName;
    private final boolean client;
    private final boolean server;

    public boolean isOnlyServer() {
        return isServer() && !isClient();
    }

    public static @Nullable Platform getPlatform(String name) {
        for (Platform platform : values()) {
            if (platform.getName().equalsIgnoreCase(name)
                || platform.getHmcName().equalsIgnoreCase(name)) {
                return platform;
            }
        }

        return null;
    }

}
