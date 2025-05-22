package io.github.headlesshq.headlessmc.launcher.server;

import lombok.Data;
import lombok.EqualsAndHashCode;
import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.launcher.api.Platform;

@Data
public class ServerType implements HasName {
    private final Platform platform;
    @EqualsAndHashCode.Exclude
    private final ServerTypeDownloader downloader;

    @Override
    public String getName() {
        return platform.getName();
    }

}
