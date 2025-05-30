package io.github.headlesshq.headlessmc.launcher.auth;

import lombok.Getter;
import lombok.Setter;
import io.github.headlesshq.headlessmc.api.settings.Config;

@Setter
@Getter
public class OfflineChecker {
    private boolean offline;

    public OfflineChecker(Config configService) {
        this.offline = configService.getConfig().get(LauncherProperties.OFFLINE, false);
    }

}
