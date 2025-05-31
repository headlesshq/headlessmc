package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.launcher.api.Platform;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
