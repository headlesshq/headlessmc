package io.github.headlesshq.headlessmc.launcher.auth;

import io.github.headlesshq.headlessmc.api.settings.Config;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OfflineChecker {
    private boolean offline;

    public OfflineChecker(Config configService) {
        this.offline = configService.getConfig().get(LauncherProperties.OFFLINE, false);
    }

}
